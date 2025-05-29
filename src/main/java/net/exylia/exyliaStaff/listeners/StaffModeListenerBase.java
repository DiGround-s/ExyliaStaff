package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.block.Action;

/**
 * Base class for all Staff Mode related listeners.
 * Contains common functionality used by child listeners.
 */
public abstract class StaffModeListenerBase implements Listener {

    protected final ExyliaStaff plugin;
    protected final StaffManager staffManager;
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private static final long COOLDOWN_MILLIS = 200;

    public StaffModeListenerBase(ExyliaStaff plugin, StaffManager staffManager) {
        this.plugin = plugin;
        this.staffManager = staffManager;
    }

    /**
     * Executes a staff item action if cooldown has passed.
     * @param player The player using the item
     * @param item The staff item being used
     * @param targetPlayer The target player (if any)
     * @param action The interaction action (if any)
     * @return true if action was executed, false if on cooldown
     */
    protected boolean tryExecuteStaffItemAction(Player player, ItemStack item, Player targetPlayer, Action action) {
        if (!staffManager.getStaffModeManager().getStaffItems().isStaffItem(item)) {
            return false;
        }

        long now = System.currentTimeMillis();
        long last = lastUse.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MILLIS) {
            return false;
        }

        lastUse.put(player.getUniqueId(), now);
        staffManager.getStaffModeManager().executeStaffItemAction(player, item, targetPlayer, action);
        return true;
    }
    protected boolean isInStaffMode(Player player) {
        return staffManager.getStaffModeManager().isInStaffMode(player);
    }
    protected boolean isFrozen(Player player) {
        return staffManager.getFreezeManager().isFrozen(player);
    }
    protected void handlePlayerDisconnectWhileFrozen(UUID playerUUID) {
        staffManager.getFreezeManager().handlePlayerDisconnectWhileFrozen(playerUUID, staffManager.getFreezeManager().getStaffPlayerWhoFroze(playerUUID));
    }
}