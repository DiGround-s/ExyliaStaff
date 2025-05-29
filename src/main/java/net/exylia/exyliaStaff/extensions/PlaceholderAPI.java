package net.exylia.exyliaStaff.extensions;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import net.exylia.exyliaStaff.models.StaffPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Expansion for PlaceholderAPI that provides placeholders for staff mode features.
 */
public class PlaceholderAPI extends PlaceholderExpansion {

    private final ExyliaStaff plugin;
    private final StaffManager staffManager;

    public PlaceholderAPI(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.staffManager = plugin.getStaffManager();
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
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("online_staff")) {
            return String.valueOf(getOnlineStaffCount());
        }

        if (player == null || !player.isOnline()) {
            return "";
        }

        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer == null) {
            return "";
        }

        if (params.equalsIgnoreCase("prefix")) {
            return getPrefix(onlinePlayer);
        }

        if (params.equalsIgnoreCase("instaff")) {
            return String.valueOf(staffManager.getStaffModeManager().isInStaffMode(onlinePlayer));
        }

        if (params.equalsIgnoreCase("vanished")) {
            return String.valueOf(staffManager.getVanishManager().isVanished(onlinePlayer.getUniqueId()));
        }

        if (params.equalsIgnoreCase("frozen")) {
            return String.valueOf(staffManager.getFreezeManager().isFrozen(onlinePlayer));
        }

        if (params.equalsIgnoreCase("status_vanish")) {
            return getVanishStatus(onlinePlayer);
        }

        if (params.equalsIgnoreCase("status_notifications")) {
            return getNotificationsStatus(onlinePlayer);
        }

        return null;
    }

    /**
     * Gets the appropriate prefix for a player based on their status.
     * Priority order: Frozen > Staff Mode > Vanish > No prefix
     *
     * @param player The player to get the prefix for
     * @return The prefix string, or empty string if no prefix applies
     */
    private String getPrefix(Player player) {
        if (staffManager.getFreezeManager().isFrozen(player)) {
            return plugin.getConfigManager().getConfig("config").getString("prefixes.frozen", "<#ffc58f>SS <dark_gray><bold>•<reset> <#ffd2a8>");
        }

        if (staffManager.getStaffModeManager().isInStaffMode(player)) {
            return plugin.getConfigManager().getConfig("config").getString("prefixes.staff-mode", "<#ffc58f>Staff <dark_gray><bold>•<reset> <#ffd2a8>");
        }

        if (staffManager.getVanishManager().isVanished(player.getUniqueId())) {
            return plugin.getConfigManager().getConfig("config").getString("prefixes.vanish", "<#ffc58f>V <dark_gray><bold>•<reset> <#ffd2a8>");
        }

        return "";
    }

    /**
     * Gets the vanish status placeholder for a player.
     *
     * @param player The player to get the status for
     * @return Active or Inactive text based on vanish status
     */
    private String getVanishStatus(Player player) {
        boolean isVanished = staffManager.getVanishManager().isVanished(player.getUniqueId());
        return isVanished ?
                plugin.getConfigManager().getConfig("messages").getString("placeholders.active") :
                plugin.getConfigManager().getConfig("messages").getString("placeholders.inactive");
    }

    /**
     * Gets the notifications status placeholder for a player.
     *
     * @param player The player to get the status for
     * @return Active or Inactive text based on notifications status
     */
    private String getNotificationsStatus(Player player) {
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(player.getUniqueId());
        if (staffPlayer == null) {
            return plugin.getConfigManager().getConfig("messages").getString("placeholders.inactive");
        }

        boolean notificationsEnabled = staffPlayer.hasNotificationsEnabled();
        return notificationsEnabled ?
                plugin.getConfigManager().getConfig("messages").getString("placeholders.active") :
                plugin.getConfigManager().getConfig("messages").getString("placeholders.inactive");
    }

    /**
     * Gets the count of online staff members.
     *
     * @return The number of online staff members
     */
    private int getOnlineStaffCount() {
        int count = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("exyliastaff.staff")) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean canRegister() {
        return true;
    }
}