package net.exylia.exyliaStaff.managers.staff;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
import net.exylia.exyliaStaff.managers.enums.FlyState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Todo: Implement to database

public class FlyManager {
    private final Map<UUID, FlyState> staffFlyStates = new HashMap<>();
    private final ExyliaStaff plugin;
    private final StaffManager staffManager;

    public FlyManager(ExyliaStaff plugin, StaffManager staffManager) {
        this.plugin = plugin;
        this.staffManager = staffManager;
        startFlyMonitor();
    }

    private void startFlyMonitor() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : staffManager.getStaffPlayers().keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && staffManager.getStaffModeManager().isInStaffMode(player)) {
                    checkAndRestoreFly(player);
                }
            }
        }, 20L, 20L);
    }

    private void checkAndRestoreFly(Player player) {
        UUID playerId = player.getUniqueId();
        boolean currentAllowFlight = player.getAllowFlight();
        FlyState flyState = staffFlyStates.getOrDefault(playerId, FlyState.AUTO);

        if ((flyState == FlyState.AUTO || flyState == FlyState.ENABLED) && !currentAllowFlight) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (player.isOnline() && staffManager.getStaffModeManager().isInStaffMode(player)) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                    sendFlyStatusMessage(player, true);
                }
            }, 1L);
        }
        else if (flyState == FlyState.DISABLED && currentAllowFlight) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }
    }

    public void enableStaffFly(Player player) {
        if (!staffManager.getStaffModeManager().isInStaffMode(player)) return;

        UUID playerId = player.getUniqueId();
        staffFlyStates.put(playerId, FlyState.ENABLED);
        player.setAllowFlight(true);
        player.setFlying(true);
        sendFlyStatusMessage(player, true);
    }

    public void disableStaffFly(Player player) {
        if (!staffManager.getStaffModeManager().isInStaffMode(player)) return;

        UUID playerId = player.getUniqueId();
        staffFlyStates.put(playerId, FlyState.DISABLED);
        player.setAllowFlight(false);
        player.setFlying(false);
        sendFlyStatusMessage(player, false);
    }

    public void setAutoStaffFly(Player player) {
        if (!staffManager.getStaffModeManager().isInStaffMode(player)) return;

        UUID playerId = player.getUniqueId();
        staffFlyStates.put(playerId, FlyState.AUTO);
        player.setAllowFlight(true);
        player.setFlying(true);
        sendFlyStatusMessage(player, true, true);
    }

    public void toggleFlyState(Player player) {
        if (!staffManager.getStaffModeManager().isInStaffMode(player)) return;

        UUID playerId = player.getUniqueId();
        FlyState currentState = staffFlyStates.getOrDefault(playerId, FlyState.AUTO);

        switch (currentState) {
            case AUTO:
                disableStaffFly(player);
                break;
            case DISABLED:
                enableStaffFly(player);
                break;
            case ENABLED:
                setAutoStaffFly(player);
                break;
        }
    }

    private void sendFlyStatusMessage(Player player, boolean enabled) {
        sendFlyStatusMessage(player, enabled, false);
    }

    private void sendFlyStatusMessage(Player player, boolean enabled, boolean auto) {
        // Todo
    }

    public void restoreStaffFlyOnEnable(Player player) {
        UUID playerId = player.getUniqueId();
        FlyState state = staffFlyStates.getOrDefault(playerId, FlyState.AUTO);

        if (state == FlyState.DISABLED) {
            player.setAllowFlight(false);
            player.setFlying(false);
        } else {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
    }

    public void saveStaffFlyOnDisable(Player player) {
    }
}
