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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.ColorUtils.sendPlayerMessage;
import static net.exylia.commons.utils.ColorUtils.sendSenderMessage;

public class StaffModeCommand implements CommandExecutor, TabCompleter {

    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;

    public StaffModeCommand(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player) && args.length == 0) {
            sendSenderMessage(sender, plugin.getConfigManager().getMessage("system.only-players"));
            return true;
            
        }

        // /staffmode
        if (args.length == 0) {
            Player player = (Player) sender;

            if (!player.hasPermission("exyliastaff.staffmode")) {
                sendPlayerMessage(player, plugin.getConfigManager().getMessage("system.no-permission"));
                return true;
            }

            staffModeManager.toggleStaffMode(player);
            return true;
        }

        // /staffmode <on|off|toggle> [player]
        String action = args[0].toLowerCase();
        Player target;

        if (args.length >= 2) {
            // Con jugador específico (requiere permiso admin)
            if (!sender.hasPermission("exyliastaff.staffmode.others")) {
                sendSenderMessage(sender, plugin.getConfigManager().getMessage("system.no-permission"));
                return true;
            }

            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sendSenderMessage(sender, plugin.getConfigManager().getMessage("system.player-not-found", "%player%", args[1]));
                return true;
            }
        } else {
            // Sin jugador específico (solo para jugadores)
            if (!(sender instanceof Player)) {
                sendSenderMessage(sender, plugin.getConfigManager().getMessage("system.specify-player"));
                return true;
            }

            if (!sender.hasPermission("exyliastaff.staffmode")) {
                sendSenderMessage(sender, plugin.getConfigManager().getMessage("system.no-permission"));
                return true;
            }

            target = (Player) sender;
        }

        // Procesar acción
        switch (action) {
            case "on":
                staffModeManager.enableStaffMode(target);
                if (sender != target) {
                    sendSenderMessage(sender, plugin.getConfigManager().getMessage("actions.staff-mode.enabled-other", "%player%", target.getName()));
                }
                break;
            case "off":
                staffModeManager.disableStaffMode(target);
                if (sender != target) {
                    sendSenderMessage(sender, plugin.getConfigManager().getMessage("actions.staff-mode.enabled-other", "%player%", target.getName()));
                }
                break;
            case "toggle":
                staffModeManager.toggleStaffMode(target);
                String status = staffModeManager.isInStaffMode(target) ? "enabled" : "disabled";
                if (sender != target) {
                    sendSenderMessage(sender, plugin.getConfigManager().getMessage("actions.staff-mode." + status + "-other", "%player%", target.getName()));
                }
                break;
            case "reload":
                if (!sender.hasPermission("exyliastaff.reload")) {
                    sendSenderMessage(sender, plugin.getConfigManager().getMessage("system.no-permission"));
                    return true;
                }
                plugin.getConfigManager().reloadAllConfigs();
                sendSenderMessage(sender, plugin.getConfigManager().getMessage("system.reloaded"));
                break;
            default:
                sendSenderMessage(sender, plugin.getConfigManager().getMessage("system.usage"));
                break;
        }

        return true;

    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> actions = new ArrayList<>(Arrays.asList("on", "off", "toggle"));

            if (sender.hasPermission("exyliastaff.reload")) {
                actions.add("reload");
            }

            String current = args[0].toLowerCase();
            return actions.stream()
                    .filter(s -> s.startsWith(current))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && sender.hasPermission("exyliastaff.staffmode.others")) {
            if (!args[0].equalsIgnoreCase("reload")) {
                String current = args[1].toLowerCase();
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(current))
                        .collect(Collectors.toList());
            }
        }

        return completions;
    }
}