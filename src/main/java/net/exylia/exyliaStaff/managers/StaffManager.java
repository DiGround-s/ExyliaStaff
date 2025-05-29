package net.exylia.exyliaStaff.managers;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.database.tables.StaffPlayerTable;
import net.exylia.exyliaStaff.managers.staff.*;
import net.exylia.exyliaStaff.models.StaffPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Main Staff Manager - Acts as the central coordinator for all staff-related functionality.
 * This class manages the lifecycle of staff players and coordinates between different managers.
 */
public class StaffManager {
    private final ExyliaStaff plugin;
    private final Map<UUID, StaffPlayer> staffPlayers;

    // Sub-managers
    private final StaffModeManager staffModeManager;
    private final VanishManager vanishManager;
    private final FreezeManager freezeManager;
    private final InspectionManager inspectionManager;
    private final MovementManager movementManager;
    private final MinerHubManager minerHubManager;
    private final PunishmentHubManager punishmentHubManager;
    private final BlockBreakNotifier blockBreakNotifier;
    private final ScoreboardManager scoreboardManager;
    private final FlyManager flyManager;

    public StaffManager(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.staffPlayers = new HashMap<>();

        // Initialize all sub-managers
        this.staffModeManager = new StaffModeManager(plugin, this);
        this.vanishManager = new VanishManager(plugin, this);
        this.freezeManager = new FreezeManager(plugin, this);
        this.inspectionManager = new InspectionManager(plugin, this);
        this.movementManager = new MovementManager(plugin, this);
        this.minerHubManager = new MinerHubManager(plugin);
        this.punishmentHubManager = new PunishmentHubManager(plugin);
        this.blockBreakNotifier = new BlockBreakNotifier(plugin);
        this.scoreboardManager = new ScoreboardManager(plugin);
        this.flyManager = new FlyManager(plugin, this);
    }

    /**
     * Loads a staff player from the database and initializes their state
     */
    public void loadPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        new BukkitRunnable() {
            @Override
            public void run() {
                StaffPlayerTable staffPlayerTable = plugin.getDatabaseLoader().getStaffPlayerTable();
                StaffPlayer staffPlayer = staffPlayerTable.loadStaffPlayer(uuid);

                if (staffPlayer != null) {
                    staffPlayers.put(uuid, staffPlayer);

                    // Let sub-managers handle their specific initialization
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            initializePlayerState(player, staffPlayer);
                        }
                    }.runTask(plugin);
                } else {
                    staffPlayers.put(uuid, new StaffPlayer(uuid, false, false));
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Initializes player state based on their stored data
     */
    private void initializePlayerState(Player player, StaffPlayer staffPlayer) {
        UUID uuid = player.getUniqueId();

        // Initialize staff mode if needed
        if (staffPlayer.isInStaffMode() && plugin.isModuleEnabled("staff-mode")) {
            staffModeManager.initializeStaffMode(player);
        }

        // Initialize vanish if needed
        if (staffPlayer.isVanished() && plugin.isModuleEnabled("vanish")) {
            vanishManager.addVanishedPlayer(uuid);
        }
    }

    /**
     * Saves a staff player to the database
     */
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

    /**
     * Saves all staff players asynchronously
     */
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

    /**
     * Saves all staff players synchronously
     */
    public void saveAllPlayers() {
        StaffPlayerTable staffPlayerTable = plugin.getDatabaseLoader().getStaffPlayerTable();
        for (StaffPlayer staffPlayer : staffPlayers.values()) {
            if (staffPlayer != null) {
                staffPlayerTable.saveStaffPlayer(staffPlayer);
            }
        }
    }

    /**
     * Disables staff mode for all players
     */
    public void disableAllStaffMode() {
        disableAllStaffMode(true);
    }

    public void disableAllStaffMode(boolean async) {
        if (!plugin.isModuleEnabled("staff-mode")) {
            return;
        }

        staffModeManager.disableAllStaffMode(async);
    }

    /**
     * Unloads a player and cleans up their data
     */
    public void unloadPlayer(Player player) {
        UUID uuid = player.getUniqueId();

        // Clean up in sub-managers
        staffModeManager.cleanupPlayer(uuid);

        if (plugin.isModuleEnabled("vanish")) {
            vanishManager.removeVanishedPlayer(uuid);
        }

        if (plugin.isModuleEnabled("freeze")) {
            freezeManager.removeFromFrozenPlayers(uuid);
        }

        if (plugin.isModuleEnabled("scoreboard")) {
            scoreboardManager.hideStaffScoreboard(player);
        }

        // Remove from main collection
        staffPlayers.remove(uuid);
    }

    /**
     * Gets a staff player by UUID
     */
    public StaffPlayer getStaffPlayer(UUID uuid) {
        return staffPlayers.get(uuid);
    }

    /**
     * Gets or creates a staff player
     */
    public StaffPlayer getOrCreateStaffPlayer(UUID uuid) {
        return staffPlayers.computeIfAbsent(uuid, k -> new StaffPlayer(k, false, false));
    }

    /**
     * Gets all staff players
     */
    public Map<UUID, StaffPlayer> getStaffPlayers() {
        return Collections.unmodifiableMap(staffPlayers);
    }

    // Getters for sub-managers
    public StaffModeManager getStaffModeManager() {
        return staffModeManager;
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

    public MinerHubManager getMinerHubManager() {
        return minerHubManager;
    }

    public PunishmentHubManager getPunishmentHubManager() {
        return punishmentHubManager;
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

    public ExyliaStaff getPlugin() {
        return plugin;
    }
}