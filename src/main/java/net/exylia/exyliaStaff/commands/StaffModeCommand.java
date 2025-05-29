package net.exylia.exyliaStaff.commands;

import net.exylia.commons.command.types.ToggleCommand;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.utils.MessageUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;

import java.util.Arrays;
import java.util.List;

/**
 * Ejemplo de implementación de StaffModeCommand usando el nuevo sistema
 */
public class StaffModeCommand extends ToggleCommand {

    private final ExyliaStaff plugin;
    private final StaffManager staffManager;
    private ConfigManager configManager;

    /**
     * Constructor
     *
     * @param plugin Instancia del plugin
     * @param staffManager Gestor de StaffMode
     * @param aliases Aliases para el comando
     */
    public StaffModeCommand(ExyliaStaff plugin, StaffManager staffManager, List<String> aliases) {
        super(plugin, "staffmode", aliases,
                "exyliastaff.staff.mode", "exyliastaff.staff.mode.others");
        this.plugin = plugin;
        this.staffManager = staffManager;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender,
                                      @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> completions = filterSuggestions(
                    Arrays.asList("on", "off", "toggle"), args[0]
            );

            // Añadir subcomando reload si tiene permiso
            if (hasPermission(sender, "exyliastaff.reload")) {
                completions.addAll(filterSuggestions(Arrays.asList("reload"), args[0]));
            }

            return completions;
        }

        return super.onTabComplete(sender, command, alias, args);
    }

    @Override
    protected boolean onCommand(CommandSender sender, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!hasPermission(sender, "exyliastaff.reload")) {
                onPermissionDenied(sender);
                return true;
            }

            plugin.getConfigManager().reloadAllConfigs();
            MessageUtils.sendMessageAsync(sender, configManager.getMessage("system.reloaded"));
            return true;
        }

        return super.onCommand(sender, label, args);
    }

    @Override
    protected void enableFeature(Player player) {
        staffManager.getStaffModeManager().enableStaffMode(player);
    }

    @Override
    protected void disableFeature(Player player) {
        staffManager.getStaffModeManager().disableStaffMode(player);
    }

    @Override
    protected boolean toggleFeature(Player player) {
        staffManager.getStaffModeManager().toggleStaffMode(player);
        return staffManager.getStaffModeManager().isInStaffMode(player);
    }

    @Override
    protected void sendEnableMessage(Player player) {
    }

    @Override
    protected void sendDisableMessage(Player player) {
        MessageUtils.sendMessageAsync(player, configManager.getMessage("actions.staff-mode.disabled"));
    }

    @Override
    protected void sendEnableOtherMessage(Player sender, Player target) {
        MessageUtils.sendMessageAsync(sender, configManager.getMessage("actions.staff-mode.enabled-other", "%player%", target.getName()));
    }

    @Override
    protected void sendDisableOtherMessage(Player sender, Player target) {
        MessageUtils.sendMessageAsync(sender, configManager.getMessage("actions.staff-mode.disabled-other", "%player%", target.getName()));
    }

    @Override
    protected void onPermissionDenied(CommandSender sender) {
        MessageUtils.sendMessageAsync(sender, configManager.getMessage("system.no-permission"));
    }

    @Override
    protected void onPlayerNotFound(CommandSender sender, String name) {
        MessageUtils.sendMessageAsync(sender, configManager.getMessage("system.player-not-found", "%target%", name));
    }
}
