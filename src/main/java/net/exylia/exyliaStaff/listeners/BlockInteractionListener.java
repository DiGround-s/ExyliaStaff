package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Handles all block interaction events for staff mode
 */
public class BlockInteractionListener extends StaffModeListenerBase {

    public BlockInteractionListener(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        super(plugin, staffModeManager);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (isFrozen(player)) {
            event.setCancelled(true);
            return;
        }

        if (isInStaffMode(player) &&
                !plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.can-break-blocks", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (isFrozen(player)) {
            event.setCancelled(true);
            return;
        }

        if (isInStaffMode(player) &&
                !plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.can-place-blocks", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (isInStaffMode(player)) return;

        if (staffModeManager.getBlockBreakNotifier().isWatchedBlock(block.getType())) {
            staffModeManager.getBlockBreakNotifier().notifyStaff(player, block);
        }
    }
}