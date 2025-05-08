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

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            // Usamos el nuevo sistema para procesar las acciones de los ítems de staff
            if (staffModeManager.getStaffItems().isStaffItem(item)) {
                // Ejecutamos la acción del ítem sin un jugador objetivo
                staffModeManager.executeStaffItemAction(player, item, null);
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();

        // Si es un ítem de staff y la entidad es un jugador, ejecutamos la acción
        if (staffModeManager.getStaffItems().isStaffItem(item) && event.getRightClicked() instanceof Player targetPlayer) {
            event.setCancelled(true);
            staffModeManager.executeStaffItemAction(player, item, targetPlayer);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

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
        if (!(event.getEntity() instanceof Player player)) return;

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
        if (!(event.getEntity() instanceof Player player)) return;

        // Los jugadores en modo staff no reciben daño
        if (staffModeManager.isInStaffMode(player)) {
            event.setCancelled(true);
        }

        // Los jugadores congelados no reciben daño
        if (staffModeManager.isFrozen(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;

        // Los jugadores en modo staff no pueden hacer daño a menos que se configure lo contrario
        if (staffModeManager.isInStaffMode(damager) &&
                !plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.can-damage-entities", false)) {
            event.setCancelled(true);
        }

        // Los jugadores congelados no pueden hacer daño
        if (staffModeManager.isFrozen(damager)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;

        // Las entidades no atacan a jugadores en modo staff o congelados
        if (staffModeManager.isInStaffMode(player) || staffModeManager.isFrozen(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Si el jugador está congelado, solo permitimos la rotación pero no el movimiento
        if (staffModeManager.isFrozen(player)) {
            // Permitimos rotación pero no movimiento
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {

                // Cancelamos solo el movimiento, manteniendo la rotación
                event.getTo().setX(event.getFrom().getX());
                event.getTo().setY(event.getFrom().getY());
                event.getTo().setZ(event.getFrom().getZ());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        // Bloqueamos comandos para jugadores congelados
        if (staffModeManager.isFrozen(player)) {
            // Podríamos permitir algunos comandos como /msg o similares
            String command = event.getMessage().split(" ")[0].toLowerCase();

            // Lista de comandos permitidos mientras está congelado
            boolean allowed = false;
            for (String allowedCmd : plugin.getConfigManager().getConfig("config").getStringList("freeze.allowed-commands")) {
                if (command.equalsIgnoreCase("/" + allowedCmd)) {
                    allowed = true;
                    break;
                }
            }

            if (!allowed) {
                event.setCancelled(true);
                player.sendMessage(plugin.getConfigManager().getMessage("freeze.commands-blocked"));
            }
        }
    }
}