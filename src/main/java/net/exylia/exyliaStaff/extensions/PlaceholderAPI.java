package net.exylia.exyliaStaff.extensions;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Expansion for PlaceholderAPI that provides prefixes for staff mode, frozen, and vanish statuses.
 */
public class PlaceholderAPI extends PlaceholderExpansion {

    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;

    /**
     * Constructor for the PlaceholderAPI expansion.
     * @param plugin The ExyliaStaff plugin instance
     */
    public PlaceholderAPI(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.staffModeManager = plugin.getStaffModeManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "exyliastaff";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Exylia";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required for the expansion to remain registered until the server stops
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || !player.isOnline()) {
            return "";
        }

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "";
        }

        // Handle different placeholder requests
        if (params.equalsIgnoreCase("prefix")) {
            return getPrefix(onlinePlayer);
        }

        // Additional placeholders can be added here
        if (params.equalsIgnoreCase("instaff")) {
            return String.valueOf(staffModeManager.isInStaffMode(onlinePlayer));
        }

        if (params.equalsIgnoreCase("vanished")) {
            return String.valueOf(staffModeManager.isVanished(onlinePlayer));
        }

        if (params.equalsIgnoreCase("frozen")) {
            return String.valueOf(staffModeManager.getFreezeManager().isFrozen(onlinePlayer));
        }

        return null; // Placeholder not found
    }

    /**
     * Gets the appropriate prefix for a player based on their status.
     * Priority order: Frozen > Staff Mode > Vanish > No prefix
     *
     * @param player The player to get the prefix for
     * @return The prefix string, or empty string if no prefix applies
     */
    private String getPrefix(Player player) {
        // Check if player is frozen (highest priority)
        if (staffModeManager.getFreezeManager().isFrozen(player)) {
            return plugin.getConfigManager().getConfig("config").getString("prefixes.frozen", "<#ffc58f>SS <dark_gray><bold>•<reset> <#ffd2a8>");
        }

        // Check if player is in staff mode (second priority)
        if (staffModeManager.isInStaffMode(player)) {
            return plugin.getConfigManager().getConfig("config").getString("prefixes.staff-mode", "<#ffc58f>Staff <dark_gray><bold>•<reset> <#ffd2a8>");
        }

        // Check if player is vanished (third priority)
        if (staffModeManager.isVanished(player)) {
            return plugin.getConfigManager().getConfig("config").getString("prefixes.vanish", "<#ffc58f>V <dark_gray><bold>•<reset> <#ffd2a8>");
        }

        // No prefix applies
        return "";
    }

    @Override
    public boolean canRegister() {
        return true;
    }
}