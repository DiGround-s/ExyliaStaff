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

        staffModeManager.loadPlayer(player);

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
        if (staffModeManager.getFreezeManager().isFrozen((player))) event.setCancelled(true);
        if (!staffModeManager.isInStaffMode(player)) return;

        ItemStack item = event.getItem();
        if (item != null && staffModeManager.getStaffItems().isStaffItem(item)) {
            long now = System.currentTimeMillis();
            long last = lastUse.getOrDefault(player.getUniqueId(), 0L);
            if (now - last < 200) return;

            lastUse.put(player.getUniqueId(), now);
            event.setCancelled(true);
            staffModeManager.executeStaffItemAction(player, item, null, event.getAction());
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null) {
            switch (clickedBlock.getType()) {
                case ACACIA_DOOR:
                case BIRCH_DOOR:
                case DARK_OAK_DOOR:
                case JUNGLE_DOOR:
                case OAK_DOOR:
                case SPRUCE_DOOR:
                case CRIMSON_DOOR:
                case WARPED_DOOR:
                case IRON_DOOR:
                case ACACIA_TRAPDOOR:
                case BIRCH_TRAPDOOR:
                case DARK_OAK_TRAPDOOR:
                case JUNGLE_TRAPDOOR:
                case OAK_TRAPDOOR:
                case SPRUCE_TRAPDOOR:
                case CRIMSON_TRAPDOOR:
                case WARPED_TRAPDOOR:
                case ACACIA_FENCE_GATE:
                case BIRCH_FENCE_GATE:
                case DARK_OAK_FENCE_GATE:
                case JUNGLE_FENCE_GATE:
                case OAK_FENCE_GATE:
                case SPRUCE_FENCE_GATE:
                case CRIMSON_FENCE_GATE:
                case WARPED_FENCE_GATE:
                case ACACIA_BUTTON:
                case BIRCH_BUTTON:
                case DARK_OAK_BUTTON:
                case JUNGLE_BUTTON:
                case OAK_BUTTON:
                case SPRUCE_BUTTON:
                case CRIMSON_BUTTON:
                case WARPED_BUTTON:
                case STONE_BUTTON:
                case POLISHED_BLACKSTONE_BUTTON:
                case LEVER:
                case DISPENSER:
                case DROPPER:
                case HOPPER:
                case REPEATER:
                case COMPARATOR:
                case DAYLIGHT_DETECTOR:
                case BEACON:
                case CHEST:
                case TRAPPED_CHEST:
                case ENDER_CHEST:
                case FURNACE:
                case BLAST_FURNACE:
                case SMOKER:
                case BREWING_STAND:
                case ENCHANTING_TABLE:
                case ANVIL:
                case CHIPPED_ANVIL:
                case DAMAGED_ANVIL:
                case BARREL:
                case LOOM:
                case CARTOGRAPHY_TABLE:
                case GRINDSTONE:
                case STONECUTTER:
                case SMITHING_TABLE:
                case LECTERN:
                case NOTE_BLOCK:
                case JUKEBOX:
                case CAMPFIRE:
                case SOUL_CAMPFIRE:
                case COMPOSTER:
                case BELL:
                    event.setCancelled(true);
                    return;
                default:
                    break;
            }
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

        if (staffModeManager.getFreezeManager().isFrozen((player))) event.setCancelled(true);
        if (!staffModeManager.isInStaffMode(player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (staffModeManager.getFreezeManager().isFrozen((player))) event.setCancelled(true);
        if (staffModeManager.isInStaffMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (staffModeManager.getFreezeManager().isFrozen((player))) event.setCancelled(true);
        if (staffModeManager.isInStaffMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (staffModeManager.getFreezeManager().isFrozen((player))) event.setCancelled(true);
        if (staffModeManager.isInStaffMode(player) &&
                !plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.can-break-blocks", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (staffModeManager.getFreezeManager().isFrozen((player))) event.setCancelled(true);
        if (staffModeManager.isInStaffMode(player) &&
                !plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.can-place-blocks", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (staffModeManager.isInStaffMode(player)) {
            event.setCancelled(true);
        }

        if (staffModeManager.getFreezeManager().isFrozen((player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;

        if (staffModeManager.isInStaffMode(damager) &&
                !plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.can-damage-entities", false)) {
            event.setCancelled(true);
        }

        if (staffModeManager.getFreezeManager().isFrozen((damager))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;

        if (staffModeManager.isInStaffMode(player) || staffModeManager.getFreezeManager().isFrozen((player))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (staffModeManager.getFreezeManager().isFrozen((player))) {
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {

                event.getTo().setX(event.getFrom().getX());
                event.getTo().setY(event.getFrom().getY());
                event.getTo().setZ(event.getFrom().getZ());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractPhysical(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!staffModeManager.isInStaffMode(player)) return;

        if (event.getAction() == Action.PHYSICAL) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (staffModeManager.getFreezeManager().isFrozen((player))) {
            String command = event.getMessage().split(" ")[0].toLowerCase();

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

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (staffModeManager.isInStaffMode(player)) return;

        if (staffModeManager.getBlockBreakNotifier().isWatchedBlock(block.getType())) {
            staffModeManager.getBlockBreakNotifier().notifyStaff(player, block);
        }
    }

}