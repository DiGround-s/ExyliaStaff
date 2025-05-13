package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private final Map<UUID, Long> lastUse = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!staffModeManager.isInStaffMode(player)) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        Action action = event.getAction();

        if (staffModeManager.getStaffItems().isStaffItem(item)) {
            long now = System.currentTimeMillis();
            long last = lastUse.getOrDefault(player.getUniqueId(), 0L);
            if (now - last < 200) return;

            lastUse.put(player.getUniqueId(), now);
            event.setCancelled(true);
            staffModeManager.executeStaffItemAction(player, item, null, action);
        }
    }



    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();

        if (!staffModeManager.isInStaffMode(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!staffModeManager.getStaffItems().isStaffItem(item)) return;

        if (!(event.getRightClicked() instanceof Player targetPlayer)) return;

        long now = System.currentTimeMillis();
        long last = lastUse.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 200) return;

        lastUse.put(player.getUniqueId(), now);
        event.setCancelled(true);
        staffModeManager.executeStaffItemAction(player, item, targetPlayer, null);
    }


    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (!staffModeManager.isInStaffMode(player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (staffModeManager.getFreezeManager().isFrozen((player))) event.setCancelled(true);
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

        if (staffModeManager.getFreezeManager().isFrozen((player))) event.setCancelled(true);
        // Los jugadores en modo staff no recogen items
        if (staffModeManager.isInStaffMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (staffModeManager.getFreezeManager().isFrozen((player))) event.setCancelled(true);
        // Si está configurado para no permitir romper bloques en modo staff
        if (staffModeManager.isInStaffMode(player) &&
                !plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.can-break-blocks", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (staffModeManager.getFreezeManager().isFrozen((player))) event.setCancelled(true);
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
        if (staffModeManager.getFreezeManager().isFrozen((player))) {
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
        if (staffModeManager.getFreezeManager().isFrozen((damager))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;

        // Las entidades no atacan a jugadores en modo staff o congelados
        if (staffModeManager.isInStaffMode(player) || staffModeManager.getFreezeManager().isFrozen((player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Si el jugador está congelado, solo permitimos la rotación pero no el movimiento
        if (staffModeManager.getFreezeManager().isFrozen((player))) {
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
        if (staffModeManager.getFreezeManager().isFrozen((player))) {
            // Podríamos permitir algunos comandos como /msg o similares
            String command = event.getMessage().split(" ")[0].toLowerCase();

            // Lista de comandos permitidos mientras está congelado
            boolean allowed = false;
            for (String allowedCmd : plugin.getConfigManager().getConfig("config").getStringList("frozen.allowed-commands")) {
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

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (staffModeManager.getFreezeManager().isFrozen(player)
                && !event.getCause().equals(PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            event.setCancelled(true);
        }
    }

}