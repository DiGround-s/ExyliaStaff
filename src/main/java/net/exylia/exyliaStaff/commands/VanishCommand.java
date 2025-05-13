package net.exylia.exyliaStaff.commands;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.ColorUtils.sendPlayerMessage;
import static net.exylia.commons.utils.ColorUtils.sendSenderMessage;

public class VanishCommand implements CommandExecutor, TabCompleter {

    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;

    public VanishCommand(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            sendSenderMessage(sender, plugin.getConfigManager().getMessage("system.only-players"));
            return true;
        }

        // /vanish
        if (args.length == 0) {
            Player player = (Player) sender;

            if (!player.hasPermission("exyliastaff.vanish")) {
                sendPlayerMessage(player, plugin.getConfigManager().getMessage("system.no-permission"));
                return true;
            }

            staffModeManager.getVanishManager().toggleVanish(player);
            return true;
        }

        // /vanish <player>
        if (!sender.hasPermission("exyliastaff.vanish.others")) {
            sendSenderMessage(sender, plugin.getConfigManager().getMessage("system.no-permission"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sendSenderMessage(sender, plugin.getConfigManager().getMessage("system.player-not-found", "%player%", args[0]));
            return true;
        }

        staffModeManager.getVanishManager().toggleVanish(target);
        boolean isVanished = staffModeManager.isVanished(target);

        String status = isVanished ? "enabled" : "disabled";
        if (sender != target) {
            sendSenderMessage(sender, plugin.getConfigManager().getMessage("actions.vanish." + status + "-other", "%player%", target.getName()));
        }

        return true;

    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
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