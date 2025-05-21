package net.exylia.exyliaStaff.managers;

import net.exylia.commons.scoreboard.ExyliaScoreboardManager;
import net.exylia.commons.utils.MessageUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.database.tables.StaffPlayerTable;
import net.exylia.exyliaStaff.managers.staff.*;
import net.exylia.exyliaStaff.models.StaffPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import javax.annotation.Nullable;
import java.util.*;

import static net.exylia.commons.utils.DebugUtils.logWarn;

/**
 * Main manager for Staff Mode functionality.
 * Acts as a facade for different staff mode sub-systems.
 */
public class StaffModeManager {
    private final ExyliaStaff plugin;
    private final StaffItems staffItems;
    private final Map<UUID, StaffPlayer> staffPlayers;
    private final Set<UUID> staffModeEnabled;

    private final VanishManager vanishManager;
    private final FreezeManager freezeManager;
    private final InspectionManager inspectionManager;
    private final MovementManager movementManager;
    private final CommandManager commandManager;
    private final MinerHubManager minerHubManager;
    private final PunishmentHubManager punishmentHubManager;
    private final BlockBreakNotifier blockBreakNotifier;
    private final ScoreboardManager scoreboardManager;
    private final FlyManager flyManager;

    private final Map<UUID, Boolean> staffFlyingStates = new HashMap<>();

    public StaffModeManager(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.staffItems = new StaffItems(plugin);
        this.staffPlayers = new HashMap<>();
        this.staffModeEnabled = new HashSet<>();

        this.vanishManager = new VanishManager(plugin, this);
        this.freezeManager = new FreezeManager(plugin, this);
        this.inspectionManager = new InspectionManager(plugin, this);
        this.movementManager = new MovementManager(plugin, this);
        this.commandManager = new CommandManager(plugin, this);
        this.minerHubManager = new MinerHubManager(plugin);
        this.punishmentHubManager = new PunishmentHubManager(plugin);
        this.blockBreakNotifier = new BlockBreakNotifier(plugin);
        this.scoreboardManager = new ScoreboardManager(plugin, new ExyliaScoreboardManager(plugin));
        this.flyManager = new FlyManager(plugin, this);
    }

    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        new BukkitRunnable() {
            @Override
            public void run() {
                StaffPlayerTable staffPlayerTable = plugin.getDatabaseLoader().getStaffPlayerTable();
                StaffPlayer staffPlayer = staffPlayerTable.loadStaffPlayer(uuid);

                if (staffPlayer != null) {
                    staffPlayers.put(uuid, staffPlayer);

                    if (staffPlayer.isInStaffMode()) {
                        staffModeEnabled.add(uuid);
                    }

                    if (staffPlayer.isVanished()) {
                        vanishManager.addVanishedPlayer(uuid);
                    }

                    if (staffPlayer.isInStaffMode() && staffPlayer.hasStoredInventory()) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                applyStaffMode(player);
                            }
                        }.runTask(plugin);
                    }
                } else {
                    staffPlayers.put(uuid, new StaffPlayer(uuid, false, false));
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void savePlayer(Player player) {
        savePlayer(player, true);
    }

    public void savePlayer(Player player, boolean async) {
        UUID uuid = player.getUniqueId();
        if (!staffPlayers.containsKey(uuid)) return;

        StaffPlayer staffPlayer = staffPlayers.get(uuid);
        if (staffPlayer == null) return;

        if (async) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    StaffPlayerTable staffPlayerTable = plugin.getDatabaseLoader().getStaffPlayerTable();
                    staffPlayerTable.saveStaffPlayer(staffPlayer);
                }
            }.runTaskAsynchronously(plugin);
        } else {
            StaffPlayerTable staffPlayerTable = plugin.getDatabaseLoader().getStaffPlayerTable();
            staffPlayerTable.saveStaffPlayer(staffPlayer);
        }
    }

    public void saveAllPlayersAsync() {
        new BukkitRunnable() {
            @Override
            public void run() {
                StaffPlayerTable staffPlayerTable = plugin.getDatabaseLoader().getStaffPlayerTable();
                for (StaffPlayer staffPlayer : staffPlayers.values()) {
                    if (staffPlayer != null) {
                        staffPlayerTable.saveStaffPlayer(staffPlayer);
                    }
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public void saveAllPlayers() {
        StaffPlayerTable staffPlayerTable = plugin.getDatabaseLoader().getStaffPlayerTable();
        for (StaffPlayer staffPlayer : staffPlayers.values()) {
            staffPlayerTable.saveStaffPlayer(staffPlayer);
        }
    }

    public void disableAllStaffMode() {
        disableAllStaffMode(true);
    }

    public void disableAllStaffMode(boolean async) {
        for (UUID uuid : new HashSet<>(staffModeEnabled)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                disableStaffMode(player, async);
            }
        }
    }

    public void unloadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        staffPlayers.remove(uuid);
        staffModeEnabled.remove(uuid);
        vanishManager.removeVanishedPlayer(uuid);
        freezeManager.removeFromFrozenPlayers(uuid);
        scoreboardManager.hideStaffScoreboard(player);
    }

    public void toggleStaffMode(Player player) {
        UUID uuid = player.getUniqueId();

        if (!staffPlayers.containsKey(uuid)) {
            staffPlayers.put(uuid, new StaffPlayer(uuid, false, false));
        }

        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        if (staffPlayer.isInStaffMode()) {
            disableStaffMode(player);
        } else {
            enableStaffMode(player);
        }
    }

    public void enableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();

        if (!staffPlayers.containsKey(uuid)) {
            staffPlayers.put(uuid, new StaffPlayer(uuid, false, false));
        }

        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        if (staffPlayer.isInStaffMode()) return;

        storePlayerInventory(player);

        staffPlayer.setInStaffMode(true);
        staffModeEnabled.add(uuid);
        vanishManager.enableVanish(player);

        MessageUtils.sendMessageAsync(player, (plugin.getConfigManager().getMessage("actions.staff-mode.enabled")));
        savePlayer(player);

        applyStaffMode(player);
        player.closeInventory();
    }

    public void disableStaffMode(Player player) {
        disableStaffMode(player, true);
    }

    public void disableStaffMode(Player player, boolean async) {
        UUID uuid = player.getUniqueId();

        if (!staffPlayers.containsKey(uuid)) return;

        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        if (!staffPlayer.isInStaffMode()) return;

        staffPlayer.setInStaffMode(false);
        staffModeEnabled.remove(uuid);

        if (staffPlayer.isVanished()) {
            vanishManager.disableVanish(player, async);
        }

        MessageUtils.sendMessageAsync(player, (plugin.getConfigManager().getMessage("actions.staff-mode.disabled")));

        restorePlayerInventory(player);

        scoreboardManager.hideStaffScoreboard(player);

        staffPlayer.setInventory(null);
        staffPlayer.setArmor(null);
        staffPlayer.setOffHandItem(null);

        savePlayer(player, async);
        player.setGameMode(GameMode.SURVIVAL);
        player.closeInventory();
    }

    private void storePlayerInventory(Player player) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        staffPlayer.setInventory(player.getInventory().getContents());
        staffPlayer.setArmor(player.getInventory().getArmorContents());
        staffPlayer.setOffHandItem(player.getInventory().getItemInOffHand());
        staffPlayer.setExp(player.getExp());
        staffPlayer.setLevel(player.getLevel());
    }

    private void restorePlayerInventory(Player player) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        if (!staffPlayer.hasStoredInventory()) return;

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        player.getInventory().setContents(staffPlayer.getInventory());
        player.getInventory().setArmorContents(staffPlayer.getArmor());
        player.getInventory().setItemInOffHand(staffPlayer.getOffHandItem());
        player.setExp(staffPlayer.getExp());
        player.setLevel(staffPlayer.getLevel());

        player.updateInventory();
    }

    private void applyStaffMode(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        GameMode staffGameMode = GameMode.CREATIVE;
        String gameModeStr = plugin.getConfigManager().getConfig("config").getString("staff-mode.gamemode", "CREATIVE");
        try {
            staffGameMode = GameMode.valueOf(gameModeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            logWarn("GameMode inválido en config.yml: " + gameModeStr);
        }
        player.setGameMode(staffGameMode);

        Map<String, Integer> itemSlots = staffItems.getItemSlots();

        if (!itemSlots.isEmpty()) {
            for (Map.Entry<String, Integer> entry : itemSlots.entrySet()) {
                String itemKey = entry.getKey();
                int slot = entry.getValue();

                // Check if this is the vanish item and if the player is already vanished
                UUID uuid = player.getUniqueId();
                StaffPlayer staffPlayer = staffPlayers.get(uuid);
                boolean isVanished = staffPlayer != null && staffPlayer.isVanished();

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

        player.updateInventory();

        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        if (staffPlayer.isVanished()) {
            vanishManager.applyVanishEffect(uuid, player);
        }

        scoreboardManager.showStaffScoreboard(player);
    }

    public void executeStaffItemAction(Player staffPlayer, ItemStack item, @Nullable Player targetPlayer, @Nullable Action action) {
        if (item == null) return;
        if (!isInStaffMode(staffPlayer)) return;

        if (!staffItems.isStaffItem(item)) return;

        String itemKey = staffItems.getStaffItemKey(item);
        String itemAction = staffItems.getItemAction(item);

        if (itemKey == null || itemAction == null) return;

        switch (itemAction) {
            case "phase":
                movementManager.phasePlayer(staffPlayer, action);
                break;
            case "exit":
                disableStaffMode(staffPlayer);
                break;
            case "vanish":
            case "un_vanish":
                vanishManager.toggleVanish(staffPlayer);
                break;
            case "freeze":
                if (targetPlayer != null) {
                    freezeManager.toggleFreezePlayer(staffPlayer, targetPlayer);
                } else {
                    MessageUtils.sendMessageAsync(staffPlayer, (plugin.getConfigManager().getMessage("system.no-target")));
                }
                break;
            case "inspect":
                if (targetPlayer != null) {
                    inspectionManager.openInspectInventory(staffPlayer, targetPlayer);
                } else {
                    MessageUtils.sendMessageAsync(staffPlayer, (plugin.getConfigManager().getMessage("system.no-target")));
                }
                break;
            case "player_command":
                if (staffItems.hasCommands(itemKey)) {
                    commandManager.executePlayerCommands(staffPlayer, targetPlayer, itemKey);
                }
                break;
            case "console_command":
                if (staffItems.hasCommands(itemKey)) {
                    commandManager.executeConsoleCommands(staffPlayer, targetPlayer, itemKey);
                }
                break;
            case "random_player_tp":
                movementManager.teleportToRandomPlayer(staffPlayer);
                break;
            case "miner_hub":
                minerHubManager.openMinerHubInventory(staffPlayer);
                break;
            case "punish_menu":
                punishmentHubManager.openPunishmentInventory(staffPlayer, targetPlayer);
                break;
            case "toggle_spectator":
                toggleSpectator(staffPlayer);
                break;
            case "online_players":
                inspectionManager.openOnlinePlayersMenu(staffPlayer);
                break;
        }
    }

    public void toggleSpectator(Player player) {
        if (player.getGameMode() == GameMode.SPECTATOR) {
            GameMode staffGameMode = GameMode.CREATIVE;
            String gameModeStr = plugin.getConfigManager().getConfig("config").getString("staff-mode.gamemode", "CREATIVE");
            try {
                staffGameMode = GameMode.valueOf(gameModeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                logWarn("GameMode inválido en config.yml: " + gameModeStr);
            }
            player.setGameMode(staffGameMode);
        } else {
            player.setGameMode(GameMode.SPECTATOR);
        }
    }

    private void startFlyMonitor() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : staffPlayers.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    checkAndRestoreFly(player);
                }
            }
        }, 5L, 5L);
    }

    private void checkAndRestoreFly(Player player) {
        UUID playerId = player.getUniqueId();
        boolean currentAllowFlight = player.getAllowFlight();
        boolean previousAllowFlight = staffFlyingStates.getOrDefault(playerId, false);

        if (previousAllowFlight && !currentAllowFlight) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                if (player.isOnline() && isInStaffMode(player)) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                }
            }, 1L);
        }

        staffFlyingStates.put(playerId, player.getAllowFlight());
    }

    public boolean isInStaffMode(Player player) {
        return staffModeEnabled.contains(player.getUniqueId());
    }

    public boolean isVanished(Player player) {
        return vanishManager.isVanished(player.getUniqueId());
    }

    public Map<UUID, StaffPlayer> getStaffPlayers() {
        return staffPlayers;
    }

    public StaffItems getStaffItems() {
        return staffItems;
    }

    public StaffPlayer getStaffPlayer(UUID uuid) {
        return staffPlayers.get(uuid);
    }

    public ExyliaStaff getPlugin() {
        return plugin;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }

    public FreezeManager getFreezeManager() {
        return freezeManager;
    }

    public InspectionManager getInspectionManager() {
        return inspectionManager;
    }

    public MovementManager getMovementManager() {
        return movementManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public MinerHubManager getMinerHubManager() {
        return minerHubManager;
    }

    public BlockBreakNotifier getBlockBreakNotifier() {
        return blockBreakNotifier;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public FlyManager getFlyManager() {
        return flyManager;
    }
}