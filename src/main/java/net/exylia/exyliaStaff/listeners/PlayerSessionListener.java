package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player session events (join/quit) related to staff mode
 */
public class PlayerSessionListener extends StaffModeListenerBase {

    public PlayerSessionListener(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        super(plugin, staffModeManager);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        staffModeManager.loadPlayer(player);

        // Hide vanished staff from the joining player if they don't have permission
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

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (isInStaffMode(player) || isFrozen(player)) {
            event.setCancelled(true);
        }
    }
}