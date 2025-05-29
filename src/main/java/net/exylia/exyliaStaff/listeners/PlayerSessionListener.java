package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player session events (join/quit) related to staff mode
 */
public class PlayerSessionListener extends StaffModeListenerBase {

    public PlayerSessionListener(ExyliaStaff plugin, StaffManager staffManager) {
        super(plugin, staffManager);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        staffManager.loadPlayer(player);

        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (staffManager.getVanishManager().isVanished(online.getUniqueId()) && !player.hasPermission("exyliastaff.vanish.see-others")) {
                player.hidePlayer(plugin, online);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        staffManager.savePlayer(player);
        staffManager.unloadPlayer(player);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (isInStaffMode(player) || isFrozen(player)) {
            event.setCancelled(true);
        }
    }
}