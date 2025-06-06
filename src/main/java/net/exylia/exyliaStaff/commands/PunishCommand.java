package net.exylia.exyliaStaff.commands;

import net.exylia.commons.command.annotation.CommandInfo;
import net.exylia.commons.command.types.SimpleCommand;
import net.exylia.commons.config.ConfigManager;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

@CommandInfo(
        usage = "<jugador>",
        playerOnly = true
)
public class PunishCommand extends SimpleCommand {

    private final ExyliaStaff plugin;
    private final StaffManager staffManager;
    private final ConfigManager configManager;

    public PunishCommand(ExyliaStaff plugin, StaffManager staffManager, List<String> aliases) {
        super(plugin, "punish", aliases, "exyliastaff.command.punish", true);
        this.plugin = plugin;
        this.staffManager = staffManager;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    protected boolean executeCommand(CommandSender sender, String label, String[] args) {
        if (args.length != 1) {
            showHelp(sender, label);
            return true;
        }

        Player executor = (Player) sender;

        staffManager.getPunishmentHubManager().openPunishmentInventory(executor, args[0]);
        return true;
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> playerNames = new ArrayList<>();
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (!player.equals(sender) && !player.hasPermission("exyliastaff.staff.punish")) {
                    playerNames.add(player.getName());
                }
            }

            return filterSuggestions(playerNames, args[0]);
        }

        return new ArrayList<>();
    }

    public List<String> filterSuggestions(List<String> suggestions, String input) {
        List<String> filtered = new ArrayList<>();
        String lowerInput = input.toLowerCase();

        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().startsWith(lowerInput)) {
                filtered.add(suggestion);
            }
        }

        return filtered;
    }
}