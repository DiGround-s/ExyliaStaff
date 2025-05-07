package net.exylia.exyliaStaff.commands;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VanishCommand implements CommandExecutor, TabCompleter {

    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;

    public VanishCommand(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command.only-players"));
            return true;
        }

        // /vanish
        if (args.length == 0) {
            if (!(sender instanceof Player)) return false;
            Player player = (Player) sender;

            if (!player.hasPermission("exyliastaff.vanish")) {
                player.sendMessage(plugin.getConfigManager().getMessage("command.no-permission"));
                return true;
            }

            staffModeManager.toggleVanish(player);
            return true;
        }

        // /vanish <player>
        if (args.length >= 1) {
            if (!sender.hasPermission("exyliastaff.vanish.others")) {
                sender.sendMessage(plugin.getConfigManager().getMessage("command.no-permission"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().getMessage("command.player-not-found", "%player%", args[0]));
                return true;
            }

            staffModeManager.toggleVanish(target);
            boolean isVanished = staffModeManager.isVanished(target);

            String status = isVanished ? "enabled" : "disabled";
            if (sender != target) {
                sender.sendMessage(plugin.getConfigManager().getMessage("vanish." + status + "-other", "%player%", target.getName()));
            }

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1 && sender.hasPermission("exyliastaff.vanish.others")) {
            String current = args[0].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(current))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}