package net.exylia.exyliaStaff.managers.staff;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles execution of commands for staff members
 */
public class CommandManager {
    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;

    public CommandManager(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
    }

    /**
     * Executes commands as a player
     * @param staffPlayer The staff player executing the command
     * @param targetPlayer The target player (can be null)
     * @param itemKey The key of the item that triggered the command
     */
    public void executePlayerCommands(Player staffPlayer, Player targetPlayer, String itemKey) {
        List<String> commands = staffModeManager.getStaffItems().getItemCommands(itemKey);

        for (String cmd : commands) {
            // Replace variables
            String processedCmd = processCommandVariables(cmd, staffPlayer, targetPlayer);

            // Remove initial slash if exists
            if (processedCmd.startsWith("/")) {
                processedCmd = processedCmd.substring(1);
            }

            // Execute the command as the player
            staffPlayer.performCommand(processedCmd);
        }
    }

    /**
     * Executes commands as the console
     * @param staffPlayer The staff player who triggered the command
     * @param targetPlayer The target player (can be null)
     * @param itemKey The key of the item that triggered the command
     */
    public void executeConsoleCommands(Player staffPlayer, Player targetPlayer, String itemKey) {
        List<String> commands = staffModeManager.getStaffItems().getItemCommands(itemKey);

        for (String cmd : commands) {
            // Replace variables
            String processedCmd = processCommandVariables(cmd, staffPlayer, targetPlayer);

            // Remove initial slash if exists
            if (processedCmd.startsWith("/")) {
                processedCmd = processedCmd.substring(1);
            }

            // Execute the command as the console
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd);
        }
    }

    /**
     * Processes command variables, replacing placeholders with actual values
     * @param command The command string with placeholders
     * @param staffPlayer The staff player
     * @param targetPlayer The target player (can be null)
     * @return The processed command with replaced placeholders
     */
    private String processCommandVariables(String command, Player staffPlayer, Player targetPlayer) {
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