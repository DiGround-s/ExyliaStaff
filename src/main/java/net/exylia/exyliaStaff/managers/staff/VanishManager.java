package net.exylia.exyliaStaff.managers.staff;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import net.exylia.exyliaStaff.models.StaffPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static net.exylia.commons.utils.ColorUtils.sendPlayerMessage;

/**
 * Handles the vanish functionality for staff members
 */
public class VanishManager {
    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;
    private final Set<UUID> vanished;

    public VanishManager(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
        this.vanished = new HashSet<>();
    }

    public void toggleVanish(Player player) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffModeManager.getStaffPlayer(uuid);

        if (staffPlayer == null) return;

        if (staffPlayer.isVanished()) {
            disableVanish(player);
        } else {
            enableVanish(player);
        }
    }

    public void enableVanish(Player player) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffModeManager.getStaffPlayer(uuid);

        if (staffPlayer == null) return;
        if (staffPlayer.isVanished()) return;

        staffPlayer.setVanished(true);
        vanished.add(uuid);

        applyVanishEffect(uuid, player);

        if (staffModeManager.isInStaffMode(player)) {
            updateVanishItem(player, true);
        }

        player.sendMessage(plugin.getConfigManager().getMessage("actions.vanish.enabled"));
        staffModeManager.savePlayer(player);
    }

    public void disableVanish(Player player) {
        disableVanish(player, true);
    }

    public void disableVanish(Player player, boolean async) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffModeManager.getStaffPlayer(uuid);

        if (staffPlayer == null) return;
        if (!staffPlayer.isVanished()) return;

        staffPlayer.setVanished(false);
        vanished.remove(uuid);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }

        if (staffModeManager.isInStaffMode(player)) {
            updateVanishItem(player, false);
        }

        player.sendMessage(plugin.getConfigManager().getMessage("actions.vanish.disabled"));
        staffModeManager.savePlayer(player, async);
    }

    public void applyVanishEffect(UUID uuid, Player player) {
        vanished.add(uuid);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("exyliastaff.see-vanished")) {
                online.hidePlayer(plugin, player);
            }
        }
    }

    private void updateVanishItem(Player player, boolean vanished) {
        if (!staffModeManager.getStaffItems().hasAlternateState("vanish")) return;

        int slot = staffModeManager.getStaffItems().getSlot("vanish");
        if (slot == -1) return;

        ItemStack vanishItem;
        if (vanished) {
            vanishItem = staffModeManager.getStaffItems().getAlternateStateItem("vanish");
        } else {
            vanishItem = staffModeManager.getStaffItems().getItem("vanish");
        }

        if (vanishItem != null) {
            player.getInventory().setItem(slot, vanishItem);
            player.updateInventory();
        }
    }

    public boolean isVanished(UUID playerUuid) {
        return vanished.contains(playerUuid);
    }

    public void addVanishedPlayer(UUID playerUuid) {
        vanished.add(playerUuid);
    }

    public void removeVanishedPlayer(UUID playerUuid) {
        vanished.remove(playerUuid);
    }
}