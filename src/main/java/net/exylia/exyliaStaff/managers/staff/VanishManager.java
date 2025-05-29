package net.exylia.exyliaStaff.managers.staff;

import net.exylia.commons.utils.MessageUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import net.exylia.exyliaStaff.models.StaffPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the vanish functionality for staff members
 */
public class VanishManager {
    private final ExyliaStaff plugin;
    private final StaffManager staffManager;
    private final Set<UUID> vanished;

    public VanishManager(ExyliaStaff plugin, StaffManager staffManager) {
        this.plugin = plugin;
        this.staffManager = staffManager;
        this.vanished = new HashSet<>();
    }

    public void toggleVanish(Player player) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(uuid);

        if (staffPlayer == null) return;

        if (staffPlayer.isVanished()) {
            disableVanish(player);
        } else {
            enableVanish(player);
        }
    }

    public void enableVanish(Player player) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(uuid);

        if (staffPlayer == null) return;
        if (staffPlayer.isVanished()) return;

        staffPlayer.setVanished(true);
        vanished.add(uuid);

        applyVanishEffect(uuid, player);

        if (staffManager.getStaffModeManager().isInStaffMode(player)) {
            updateVanishItem(player, true);
        }

        MessageUtils.sendMessageAsync(player, (plugin.getConfigManager().getMessage("actions.vanish.enabled")));
        staffManager.savePlayer(player);
    }

    public void disableVanish(Player player) {
        disableVanish(player, true);
    }

    public void disableVanish(Player player, boolean async) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(uuid);

        if (staffPlayer == null) return;
        if (!staffPlayer.isVanished()) return;

        staffPlayer.setVanished(false);
        vanished.remove(uuid);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }

        if (staffManager.getStaffModeManager().isInStaffMode(player)) {
            updateVanishItem(player, false);
        }

        MessageUtils.sendMessageAsync(player, (plugin.getConfigManager().getMessage("actions.vanish.disabled")));
        staffManager.savePlayer(player, async);
    }

    public void applyVanishEffect(UUID uuid, Player player) {
        vanished.add(uuid);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("exyliastaff.vanish.see-others")) {
                online.hidePlayer(plugin, player);
            }
        }
    }

    private void updateVanishItem(Player player, boolean vanished) {
        if (!staffManager.getStaffModeManager().getStaffItems().hasAlternateState("vanish")) return;

        int slot = staffManager.getStaffModeManager().getStaffItems().getSlot("vanish");
        if (slot == -1) return;

        ItemStack vanishItem;
        if (vanished) {
            vanishItem = staffManager.getStaffModeManager().getStaffItems().getAlternateStateItem("vanish");
        } else {
            vanishItem = staffManager.getStaffModeManager().getStaffItems().getItem("vanish");
        }

        if (vanishItem != null) {
            player.getInventory().setItem(slot, vanishItem);
            player.updateInventory();
        }
    }

    public boolean isVanished(UUID playerUuid) {
        if (!plugin.isModuleEnabled("vanish")) {
            return false;
        }
        return vanished.contains(playerUuid);
    }

    public void addVanishedPlayer(UUID playerUuid) {
        vanished.add(playerUuid);
    }

    public void removeVanishedPlayer(UUID playerUuid) {
        vanished.remove(playerUuid);
    }
}