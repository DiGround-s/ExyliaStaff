package net.exylia.exyliaStaff.managers;

import net.exylia.commons.command.CommandExecutor;
import net.exylia.commons.utils.MessageUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.models.StaffPlayer;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

import static net.exylia.commons.utils.DebugUtils.logWarn;
import static net.exylia.commons.utils.EffectUtils.applyEffects;
import static net.exylia.commons.utils.EffectUtils.removeEffects;
import static net.exylia.exyliaStaff.managers.staff.CommandManager.processCommandVariables;

/**
 * Manages Staff Mode functionality specifically.
 * Handles enabling/disabling staff mode, inventory management, and staff item actions.
 */
public class StaffModeManager {
    private final ExyliaStaff plugin;
    private final StaffManager staffManager;
    private final StaffItems staffItems;
    private final Set<UUID> staffModeEnabled;

    public StaffModeManager(ExyliaStaff plugin, StaffManager staffManager) {
        this.plugin = plugin;
        this.staffManager = staffManager;
        this.staffItems = new StaffItems(plugin);
        this.staffModeEnabled = new HashSet<>();
    }

    /**
     * Initializes staff mode for a player (used when loading from database)
     */
    public void initializeStaffMode(Player player) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(uuid);

        if (staffPlayer != null && staffPlayer.isInStaffMode()) {
            staffModeEnabled.add(uuid);

            if (staffPlayer.hasStoredInventory()) {
                applyStaffMode(player);
            }
        }
    }

    /**
     * Toggles staff mode for a player
     */
    public void toggleStaffMode(Player player) {
        if (!plugin.isModuleEnabled("staff-mode")) {
            MessageUtils.sendMessageAsync(player, plugin.getConfigManager().getMessage("system.module-disabled"));
            return;
        }

        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffManager.getOrCreateStaffPlayer(uuid);

        if (staffPlayer.isInStaffMode()) {
            disableStaffMode(player);
        } else {
            enableStaffMode(player);
        }
    }

    /**
     * Enables staff mode for a player
     */
    public void enableStaffMode(Player player) {
        if (!plugin.isModuleEnabled("staff-mode")) {
            MessageUtils.sendMessageAsync(player, plugin.getConfigManager().getMessage("system.module-disabled"));
            return;
        }

        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffManager.getOrCreateStaffPlayer(uuid);

        if (staffPlayer.isInStaffMode()) return;

        // Store current state
        storePlayerInventory(player);

        // Update staff player state
        staffPlayer.setInStaffMode(true);
        staffModeEnabled.add(uuid);

        // Enable vanish if module is enabled
        if (plugin.isModuleEnabled("vanish")) {
            staffManager.getVanishManager().enableVanish(player);
        }

        // Apply staff mode effects
        applyStaffModeEffects(player);
        applyStaffMode(player);

        MessageUtils.sendMessageAsync(player, plugin.getConfigManager().getMessage("actions.staff-mode.enabled"));
        staffManager.savePlayer(player);
    }

    /**
     * Disables staff mode for a player
     */
    public void disableStaffMode(Player player) {
        disableStaffMode(player, true);
    }

    public void disableStaffMode(Player player, boolean async) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(uuid);

        if (staffPlayer == null || !staffPlayer.isInStaffMode()) return;

        // Update state
        staffPlayer.setInStaffMode(false);
        staffModeEnabled.remove(uuid);

        // Disable vanish if enabled
        if (staffPlayer.isVanished() && plugin.isModuleEnabled("vanish")) {
            staffManager.getVanishManager().disableVanish(player, async);
        }

        // Restore player state
        restorePlayerInventory(player);
        removeStaffModeEffects(player);

        // Hide scoreboard
        if (plugin.isModuleEnabled("scoreboard")) {
            staffManager.getScoreboardManager().hideStaffScoreboard(player);
        }

        // Clear stored inventory data
        clearStoredInventory(staffPlayer);

        MessageUtils.sendMessageAsync(player, plugin.getConfigManager().getMessage("actions.staff-mode.disabled"));
        staffManager.savePlayer(player, async);
    }

    /**
     * Disables staff mode for all players currently in staff mode
     */
    public void disableAllStaffMode(boolean async) {
        for (UUID uuid : new HashSet<>(staffModeEnabled)) {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                disableStaffMode(player, async);
            }
        }
    }

    /**
     * Executes an action when a staff item is used
     */
    public void executeStaffItemAction(Player staffPlayer, ItemStack item, @Nullable Player targetPlayer, @Nullable Action action) {
        if (item == null || !isInStaffMode(staffPlayer)) return;
        if (!staffItems.isStaffItem(item)) return;

        String itemKey = staffItems.getStaffItemKey(item);
        String itemAction = staffItems.getItemAction(item);

        if (itemKey == null || itemAction == null) return;

        executeAction(staffPlayer, itemAction, itemKey, targetPlayer, action);
    }

    /**
     * Executes a specific staff item action
     */
    private void executeAction(Player staffPlayer, String action, String itemKey, @Nullable Player targetPlayer, @Nullable Action clickAction) {
        switch (action) {
            case "phase":
                staffManager.getMovementManager().phasePlayer(staffPlayer, clickAction);
                break;
            case "exit":
                disableStaffMode(staffPlayer);
                break;
            case "vanish":
            case "un_vanish":
                handleVanishAction(staffPlayer);
                break;
            case "freeze":
                handleFreezeAction(staffPlayer, targetPlayer);
                break;
            case "inspect":
                handleInspectAction(staffPlayer, targetPlayer);
                break;
            case "commands":
                handleCommandsAction(staffPlayer, itemKey, targetPlayer);
                break;
            case "random_player_tp":
                staffManager.getMovementManager().teleportToRandomPlayer(staffPlayer);
                break;
            case "miner_hub":
                handleMinerHubAction(staffPlayer);
                break;
            case "punish_menu":
                handlePunishMenuAction(staffPlayer, targetPlayer);
                break;
            case "toggle_spectator":
                toggleSpectator(staffPlayer);
                break;
            case "teleporter":
                staffManager.getInspectionManager().openOnlinePlayersMenu(staffPlayer);
                break;
        }
    }

    /**
     * Handles vanish action
     */
    private void handleVanishAction(Player staffPlayer) {
        if (!plugin.isModuleEnabled("vanish")) {
            MessageUtils.sendMessageAsync(staffPlayer, plugin.getConfigManager().getMessage("system.module-disabled"));
            return;
        }
        staffManager.getVanishManager().toggleVanish(staffPlayer);
    }

    /**
     * Handles freeze action
     */
    private void handleFreezeAction(Player staffPlayer, @Nullable Player targetPlayer) {
        if (!plugin.isModuleEnabled("freeze")) {
            MessageUtils.sendMessageAsync(staffPlayer, plugin.getConfigManager().getMessage("system.module-disabled"));
            return;
        }

        if (targetPlayer != null) {
            staffManager.getFreezeManager().toggleFreezePlayer(staffPlayer, targetPlayer);
        } else {
            MessageUtils.sendMessageAsync(staffPlayer, plugin.getConfigManager().getMessage("system.no-target"));
        }
    }

    /**
     * Handles inspect action
     */
    private void handleInspectAction(Player staffPlayer, @Nullable Player targetPlayer) {
        if (targetPlayer != null) {
            staffManager.getInspectionManager().openInspectInventory(staffPlayer, targetPlayer);
        } else {
            MessageUtils.sendMessageAsync(staffPlayer, plugin.getConfigManager().getMessage("system.no-target"));
        }
    }

    /**
     * Handles commands action
     */
    private void handleCommandsAction(Player staffPlayer, String itemKey, @Nullable Player targetPlayer) {
        if (staffItems.hasCommands(itemKey)) {
            List<String> commands = staffItems.getItemCommands(itemKey);
            for (String cmd : commands) {
                String processedCmd = processCommandVariables(cmd, staffPlayer, targetPlayer);
                if (processedCmd.startsWith("/")) {
                    processedCmd = processedCmd.substring(1);
                }
                CommandExecutor.builder(staffPlayer).withPlaceholderPlayer(staffPlayer).execute(processedCmd);
            }
        }
    }

    /**
     * Handles miner hub action
     */
    private void handleMinerHubAction(Player staffPlayer) {
        if (!plugin.isModuleEnabled("notifications")) {
            MessageUtils.sendMessageAsync(staffPlayer, plugin.getConfigManager().getMessage("system.module-disabled"));
            return;
        }
        staffManager.getMinerHubManager().openMinerHubInventory(staffPlayer);
    }

    /**
     * Handles punishment menu action
     */
    private void handlePunishMenuAction(Player staffPlayer, @Nullable Player targetPlayer) {
        if (!plugin.isModuleEnabled("punish")) {
            MessageUtils.sendMessageAsync(staffPlayer, plugin.getConfigManager().getMessage("system.module-disabled"));
            return;
        }

        if (targetPlayer != null) {
            staffManager.getPunishmentHubManager().openPunishmentInventory(staffPlayer, targetPlayer.getName());
        } else {
            MessageUtils.sendMessageAsync(staffPlayer, plugin.getConfigManager().getMessage("system.no-target"));
        }
    }

    /**
     * Toggles spectator mode for a staff player
     */
    public void toggleSpectator(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            GameMode staffGameMode = getStaffGameMode();
            player.setGameMode(staffGameMode);
        } else {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    /**
     * Stores the player's current inventory and state
     */
    private void storePlayerInventory(Player player) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(uuid);

        if (staffPlayer != null) {
            staffPlayer.setInventory(player.getInventory().getContents());
            staffPlayer.setArmor(player.getInventory().getArmorContents());
            staffPlayer.setOffHandItem(player.getInventory().getItemInOffHand());
            staffPlayer.setExp(player.getExp());
            staffPlayer.setLevel(player.getLevel());
        }
    }

    /**
     * Restores the player's inventory and state
     */
    private void restorePlayerInventory(Player player) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(uuid);

        if (staffPlayer == null || !staffPlayer.hasStoredInventory()) return;

        // Clear current inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        // Restore stored inventory
        player.getInventory().setContents(staffPlayer.getInventory());
        player.getInventory().setArmorContents(staffPlayer.getArmor());
        player.getInventory().setItemInOffHand(staffPlayer.getOffHandItem());
        player.setExp(staffPlayer.getExp());
        player.setLevel(staffPlayer.getLevel());

        // Reset game mode
        player.setGameMode(GameMode.SURVIVAL);
        player.closeInventory();
        player.updateInventory();
    }

    /**
     * Applies staff mode to a player (inventory, gamemode, etc.)
     */
    private void applyStaffMode(Player player) {
        // Clear inventory
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        // Set game mode
        player.setGameMode(getStaffGameMode());

        // Give staff items
        giveStaffItems(player);

        player.updateInventory();

        // Apply vanish if needed
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(uuid);
        if (staffPlayer != null && staffPlayer.isVanished() && plugin.isModuleEnabled("vanish")) {
            staffManager.getVanishManager().applyVanishEffect(uuid, player);
        }

        // Show scoreboard if enabled
        if (plugin.isModuleEnabled("scoreboard")) {
            staffManager.getScoreboardManager().showStaffScoreboard(player);
        }
    }

    /**
     * Gives staff items to a player
     */
    private void giveStaffItems(Player player) {
        Map<String, Integer> itemSlots = staffItems.getItemSlots();

        if (itemSlots.isEmpty()) return;

        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffManager.getStaffPlayer(uuid);
        boolean isVanished = staffPlayer != null && staffPlayer.isVanished();

        for (Map.Entry<String, Integer> entry : itemSlots.entrySet()) {
            String itemKey = entry.getKey();
            int slot = entry.getValue();

            // Check if the module for this item is enabled
            if (!isItemModuleEnabled(itemKey)) {
                continue;
            }

            // Get the appropriate item (normal or alternate state)
            ItemStack item;
            if (itemKey.equals("vanish") && isVanished && staffItems.hasAlternateState("vanish")) {
                item = staffItems.getAlternateStateItem("vanish");
            } else {
                item = staffItems.getItem(itemKey);
            }

            if (item != null && slot >= 0 && slot <= 35) {
                player.getInventory().setItem(slot, item);
            }
        }
    }

    /**
     * Applies staff mode effects to a player
     */
    private void applyStaffModeEffects(Player player) {
        player.closeInventory();
        player.setHealth(20);
        player.setFoodLevel(20);
        player.setInvulnerable(true);
        applyEffects(player, plugin.getConfigManager().getConfig("modules/staff-mode").getStringList("staff-mode.effects"));
    }

    /**
     * Removes staff mode effects from a player
     */
    private void removeStaffModeEffects(Player player) {
        player.setInvulnerable(false);
        removeEffects(player, plugin.getConfigManager().getConfig("modules/staff-mode").getStringList("staff-mode.effects"));
    }

    /**
     * Clears stored inventory data from a staff player
     */
    private void clearStoredInventory(StaffPlayer staffPlayer) {
        staffPlayer.setInventory(null);
        staffPlayer.setArmor(null);
        staffPlayer.setOffHandItem(null);
    }

    /**
     * Gets the configured staff game mode
     */
    private GameMode getStaffGameMode() {
        String gameModeStr = plugin.getConfigManager().getConfig("modules/staff-mode").getString("staff-mode.gamemode", "CREATIVE");
        try {
            return GameMode.valueOf(gameModeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            logWarn("GameMode inválido en config.yml: " + gameModeStr);
            return GameMode.CREATIVE;
        }
    }

    /**
     * Checks if the module for a staff item is enabled
     */
    private boolean isItemModuleEnabled(String itemKey) {
        return switch (itemKey) {
            case "vanish" -> plugin.isModuleEnabled("vanish");
            case "freeze" -> plugin.isModuleEnabled("freeze");
            case "punish_menu" -> plugin.isModuleEnabled("punish");
            default -> true;
        };
    }

    /**
     * Cleans up data for a player (called when unloading)
     */
    public void cleanupPlayer(UUID uuid) {
        staffModeEnabled.remove(uuid);
    }

    /**
     * Checks if a player is in staff mode
     */
    public boolean isInStaffMode(Player player) {
        return staffModeEnabled.contains(player.getUniqueId());
    }

    public boolean isInStaffMode(UUID uuid) {
        return staffModeEnabled.contains(uuid);
    }

    /**
     * Gets the staff items manager
     */
    public StaffItems getStaffItems() {
        return staffItems;
    }

    /**
     * Gets all players currently in staff mode
     */
    public Set<UUID> getPlayersInStaffMode() {
        return Collections.unmodifiableSet(staffModeEnabled);
    }
}