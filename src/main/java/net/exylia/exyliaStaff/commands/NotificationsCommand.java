package net.exylia.exyliaStaff.commands;

import net.exylia.commons.command.types.ToggleCommand;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.utils.MessageUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
import net.exylia.exyliaStaff.models.StaffPlayer;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Implementación de NotificationsCommand usando el nuevo sistema de comandos
 */
public class NotificationsCommand extends ToggleCommand {

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
    public NotificationsCommand(ExyliaStaff plugin, StaffManager staffManager, List<String> aliases) {
        super(plugin, "staffnotifications", aliases,
                "exyliastaff.notifications", "exyliastaff.notifications.others");
        this.plugin = plugin;
        this.staffManager = staffManager;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    protected void enableFeature(Player player) {
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(player.getUniqueId());
        if (staffPlayer != null && !staffPlayer.hasNotificationsEnabled()) {
            staffPlayer.setNotifications(true);
            staffManager.savePlayer(player);
        }
    }

    @Override
    protected void disableFeature(Player player) {
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(player.getUniqueId());
        if (staffPlayer != null && staffPlayer.hasNotificationsEnabled()) {
            staffPlayer.setNotifications(false);
            staffManager.savePlayer(player);
        }
    }

    @Override
    protected boolean toggleFeature(Player player) {
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(player.getUniqueId());
        if (staffPlayer == null) {
            return false;
        }

        boolean newState = !staffPlayer.hasNotificationsEnabled();
        staffPlayer.setNotifications(newState);
        staffManager.savePlayer(player);
        return newState;
    }

    @Override
    protected void sendEnableMessage(Player player) {
        MessageUtils.sendMessageAsync(player, configManager.getMessage("actions.notifications.enabled"));
    }

    @Override
    protected void sendDisableMessage(Player player) {
        MessageUtils.sendMessageAsync(player, configManager.getMessage("actions.notifications.disabled"));
    }

    @Override
    protected void sendEnableOtherMessage(Player sender, Player target) {
        MessageUtils.sendMessageAsync(sender, configManager.getMessage("actions.notifications.enabled-other", "%player%", target.getName()));
    }

    @Override
    protected void sendDisableOtherMessage(Player sender, Player target) {
        MessageUtils.sendMessageAsync(sender, configManager.getMessage("actions.notifications.disabled-other", "%player%", target.getName()));
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