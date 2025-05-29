package net.exylia.exyliaStaff.listeners;

import net.exylia.commons.utils.MessageUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.UUID;

/**
 * Handles all events specific to frozen players
 */
public class FrozenPlayerListener extends StaffModeListenerBase {

    public FrozenPlayerListener(ExyliaStaff plugin, StaffManager staffManager) {
        super(plugin, staffManager);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (isFrozen(player)) {
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {

                event.getTo().setX(event.getFrom().getX());
                event.getTo().setY(event.getFrom().getY());
                event.getTo().setZ(event.getFrom().getZ());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        if (isFrozen(player)) {
            String command = event.getMessage().split(" ")[0].toLowerCase();

            boolean allowed = false;
            for (String allowedCmd : plugin.getConfigManager().getConfig("modules/freeze").getStringList("allowed-commands")) {
                if (command.equalsIgnoreCase("/" + allowedCmd)) {
                    allowed = true;
                    break;
                }
            }

            if (!allowed) {
                event.setCancelled(true);
                MessageUtils.sendMessageAsync(player, (plugin.getConfigManager().getMessage("actions.freeze.commands-blocked")));
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (isFrozen(player) && !event.getCause().equals(PlayerTeleportEvent.TeleportCause.PLUGIN)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (isFrozen(player)) {
            handlePlayerDisconnectWhileFrozen(playerUUID);
        }
    }
}