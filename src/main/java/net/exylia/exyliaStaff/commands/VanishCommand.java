package net.exylia.exyliaStaff.commands;

import net.exylia.commons.command.types.ToggleCommand;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.utils.MessageUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Implementación de VanishCommand usando el nuevo sistema de comandos
 */
public class VanishCommand extends ToggleCommand {

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
    public VanishCommand(ExyliaStaff plugin, StaffManager staffManager, List<String> aliases) {
        super(plugin, "vanish", aliases,
                "exyliastaff.vanish", "exyliastaff.vanish.others");
        this.plugin = plugin;
        this.staffManager = staffManager;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    protected void enableFeature(Player player) {
        if (!staffManager.getVanishManager().isVanished(player.getUniqueId())) {
            staffManager.getVanishManager().toggleVanish(player);
        }
    }

    @Override
    protected void disableFeature(Player player) {
        if (staffManager.getVanishManager().isVanished(player.getUniqueId())) {
            staffManager.getVanishManager().toggleVanish(player);
        }
    }

    @Override
    protected boolean toggleFeature(Player player) {
        staffManager.getVanishManager().toggleVanish(player);
        return staffManager.getVanishManager().isVanished(player.getUniqueId());
    }

    @Override
    protected void sendEnableMessage(Player player) {
        MessageUtils.sendMessageAsync(player, configManager.getMessage("actions.vanish.enabled"));
    }

    @Override
    protected void sendDisableMessage(Player player) {
        MessageUtils.sendMessageAsync(player, configManager.getMessage("actions.vanish.disabled"));
    }

    @Override
    protected void sendEnableOtherMessage(Player sender, Player target) {
        MessageUtils.sendMessageAsync(sender, configManager.getMessage("actions.vanish.enabled-other", "%player%", target.getName()));
    }

    @Override
    protected void sendDisableOtherMessage(Player sender, Player target) {
        MessageUtils.sendMessageAsync(sender, configManager.getMessage("actions.vanish.disabled-other", "%player%", target.getName()));
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