package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
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
    protected final StaffModeManager staffModeManager;
    private final Map<UUID, Long> lastUse = new HashMap<>();
    private static final long COOLDOWN_MILLIS = 200;

    public StaffModeListenerBase(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
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
        if (!staffModeManager.getStaffItems().isStaffItem(item)) {
            return false;
        }

        long now = System.currentTimeMillis();
        long last = lastUse.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < COOLDOWN_MILLIS) {
            return false;
        }

        lastUse.put(player.getUniqueId(), now);
        staffModeManager.executeStaffItemAction(player, item, targetPlayer, action);
        return true;
    }

    /**
     * Checks if the player is in staff mode
     * @param player The player to check
     * @return true if player is in staff mode
     */
    protected boolean isInStaffMode(Player player) {
        return staffModeManager.isInStaffMode(player);
    }

    /**
     * Checks if the player is frozen
     * @param player The player to check
     * @return true if player is frozen
     */
    protected boolean isFrozen(Player player) {
        return staffModeManager.getFreezeManager().isFrozen(player);
    }
}