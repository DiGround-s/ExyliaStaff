package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class StaffModeListener implements Listener {

    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;

    public StaffModeListener(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Cargamos el staff player
        staffModeManager.loadPlayer(player);

        // Ocultamos a los jugadores vanished
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (staffModeManager.isVanished(online) && !player.hasPermission("exyliastaff.see-vanished")) {
                player.hidePlayer(plugin, online);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        staffModeManager.savePlayer(player);
        staffModeManager.unloadPlayer(player);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        // Verificamos si es un ítem de staff y procesamos la acción según corresponda
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            staffModeManager.checkStaffItem(player, item);

            // Si es un ítem de staff, cancelamos el evento para evitar interacciones no deseadas
            if (staffModeManager.getStaffItems().isStaffItem(item)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();

        // Si es un ítem de staff, permitimos que maneje la interacción pero cancelamos el evento
        if (staffModeManager.getStaffItems().isStaffItem(item)) {
            event.setCancelled(true);

            // Procesamiento específico según el ítem
            String itemKey = staffModeManager.getStaffItems().getStaffItemKey(item);
            if (itemKey == null) return;

            if (event.getRightClicked() instanceof Player) {
                Player target = (Player) event.getRightClicked();

                switch (itemKey) {
                    case "freeze":
                        handleFreezeAction(player, target);
                        break;
                    case "inspect":
                        handleInspectAction(player, target);
                        break;
                    case "teleport":
                        // El teleport se maneja con left-click, aquí no hacemos nada
                        break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (!staffModeManager.isInStaffMode(player)) return;

        ItemStack clicked = event.getCurrentItem();

        // Evitamos modificaciones al inventario de staff
        if (clicked != null && staffModeManager.getStaffItems().isStaffItem(clicked)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) return;

        ItemStack dropped = event.getItemDrop().getItemStack();

        // Evitamos que se tiren ítems de staff
        if (staffModeManager.getStaffItems().isStaffItem(dropped)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Los jugadores en modo staff no recogen items
        if (staffModeManager.isInStaffMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        // Si está configurado para no permitir romper bloques en modo staff
        if (staffModeManager.isInStaffMode(player) &&
                !plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.can-break-blocks", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        // Si está configurado para no permitir colocar bloques en modo staff
        if (staffModeManager.isInStaffMode(player) &&
                !plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.can-place-blocks", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();

        // Los jugadores en modo staff no reciben daño
        if (staffModeManager.isInStaffMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;

        Player damager = (Player) event.getDamager();

        // Los jugadores en modo staff no pueden hacer daño a menos que se configure lo contrario
        if (staffModeManager.isInStaffMode(damager) &&
                !plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.can-damage-entities", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player)) return;

        Player player = (Player) event.getTarget();

        // Las entidades no atacan a jugadores en modo staff
        if (staffModeManager.isInStaffMode(player)) {
            event.setCancelled(true);
        }
    }

    // Métodos auxiliares para manejar acciones específicas de ítems

    private void handleFreezeAction(Player staff, Player target) {
        // Aquí implementarías la lógica para congelar/descongelar jugadores
        // Esto podría ser parte de otro sistema o módulo
        staff.sendMessage(plugin.getConfigManager().getMessage("staff-items.freeze", "%player%", target.getName()));
    }

    private void handleInspectAction(Player staff, Player target) {
        // Aquí implementarías la lógica para inspeccionar el inventario de otros jugadores
        staff.sendMessage(plugin.getConfigManager().getMessage("staff-items.inspect", "%player%", target.getName()));
        // Ejemplo: staff.openInventory(target.getInventory());
    }
}