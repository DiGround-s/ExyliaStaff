package net.exylia.exyliaStaff.managers.staff;

import org.bukkit.entity.Player;

/**
 * Handles execution of commands for staff members
 */
public class CommandManager {

    private CommandManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Processes command variables, replacing placeholders with actual values
     * @param command The command string with placeholders
     * @param staffPlayer The staff player
     * @param targetPlayer The target player (can be null)
     * @return The processed command with replaced placeholders
     */
    public static String processCommandVariables(String command, Player staffPlayer, Player targetPlayer) {
        String processedCmd = command.replace("%staff%", staffPlayer.getName());

        if (targetPlayer != null) {
            processedCmd = processedCmd.replace("%player%", targetPlayer.getName());
            processedCmd = processedCmd.replace("%target%", targetPlayer.getName());
        } else {
            processedCmd = processedCmd.replace("%player%", "");
            processedCmd = processedCmd.replace("%target%", "");
        }

        // Add more placeholder replacements as needed
        return processedCmd;
    }
}