package net.exylia.exyliaStaff.managers;

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

import static net.exylia.commons.utils.ColorUtils.sendPlayerMessage;

/**
 * Main manager for Staff Mode functionality.
 * Acts as a facade for different staff mode sub-systems.
 */
public class StaffModeManager {
    private final ExyliaStaff plugin;
    private final StaffItems staffItems;
    private final Map<UUID, StaffPlayer> staffPlayers;
    private final Set<UUID> staffModeEnabled;

    // Sub-managers for specific functionality
    private final VanishManager vanishManager;
    private final FreezeManager freezeManager;
    private final InspectionManager inspectionManager;
    private final MovementManager movementManager;
    private final CommandManager commandManager;

    public StaffModeManager(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.staffItems = new StaffItems(plugin);
        this.staffPlayers = new HashMap<>();
        this.staffModeEnabled = new HashSet<>();

        // Initialize sub-managers
        this.vanishManager = new VanishManager(plugin, this);
        this.freezeManager = new FreezeManager(plugin, this);
        this.inspectionManager = new InspectionManager(plugin, this);
        this.movementManager = new MovementManager(plugin, this);
        this.commandManager = new CommandManager(plugin, this);
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

                    // Si el jugador estaba en modo staff al desconectarse, restauramos su estado
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
        savePlayer(player, true); // Por defecto, usar modo asíncrono
    }

    public void savePlayer(Player player, boolean async) {
        UUID uuid = player.getUniqueId();
        if (!staffPlayers.containsKey(uuid)) return;

        if (async) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    StaffPlayerTable staffPlayerTable = plugin.getDatabaseLoader().getStaffPlayerTable();
                    staffPlayerTable.saveStaffPlayer(staffPlayers.get(uuid));
                }
            }.runTaskAsynchronously(plugin);
        } else {
            // Versión síncrona para usar durante onDisable
            StaffPlayerTable staffPlayerTable = plugin.getDatabaseLoader().getStaffPlayerTable();
            staffPlayerTable.saveStaffPlayer(staffPlayers.get(uuid));
        }
    }

    public void saveAllPlayersAsync() {
        new BukkitRunnable() {
            @Override
            public void run() {
                StaffPlayerTable staffPlayerTable = plugin.getDatabaseLoader().getStaffPlayerTable();
                for (StaffPlayer staffPlayer : staffPlayers.values()) {
                    staffPlayerTable.saveStaffPlayer(staffPlayer);
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
        disableAllStaffMode(true); // Por defecto, usar modo asíncrono
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

        // Guardamos el inventario del jugador
        storePlayerInventory(player);

        // Aplicamos el modo staff
        staffPlayer.setInStaffMode(true);
        staffModeEnabled.add(uuid);
        vanishManager.enableVanish(player);

        // Notificamos y guardamos en DB
        player.sendMessage(plugin.getConfigManager().getMessage("actions.staff-mode.enabled"));
        savePlayer(player);

        // Aplicamos los cambios en el inventario
        applyStaffMode(player);
    }

    public void disableStaffMode(Player player) {
        disableStaffMode(player, true); // Por defecto, usar modo asíncrono
    }

    public void disableStaffMode(Player player, boolean async) {
        UUID uuid = player.getUniqueId();

        if (!staffPlayers.containsKey(uuid)) return;

        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        if (!staffPlayer.isInStaffMode()) return;

        // Actualizamos el estado
        staffPlayer.setInStaffMode(false);
        staffModeEnabled.remove(uuid);

        // Si estaba vanished, lo desvanecemos
        if (staffPlayer.isVanished()) {
            vanishManager.disableVanish(player, async);
        }

        // Notificamos y guardamos en DB
        player.sendMessage(plugin.getConfigManager().getMessage("actions.staff-mode.disabled"));

        // Restauramos el inventario original
        restorePlayerInventory(player);

        // Eliminamos el inventario almacenado
        staffPlayer.setInventory(null);
        staffPlayer.setArmor(null);
        staffPlayer.setOffHandItem(null);

        // Guardamos en DB
        savePlayer(player, async);
        player.setGameMode(GameMode.SURVIVAL);
    }

    private void storePlayerInventory(Player player) {
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        // Guardamos el inventario
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

        // Limpiamos el inventario actual
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        // Restauramos el inventario guardado
        player.getInventory().setContents(staffPlayer.getInventory());
        player.getInventory().setArmorContents(staffPlayer.getArmor());
        player.getInventory().setItemInOffHand(staffPlayer.getOffHandItem());
        player.setExp(staffPlayer.getExp());
        player.setLevel(staffPlayer.getLevel());

        // Actualizamos el inventario
        player.updateInventory();
    }

    private void applyStaffMode(Player player) {
        // Limpiamos el inventario
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);

        // Configuramos el modo de juego
        GameMode staffGameMode = GameMode.CREATIVE;
        String gameModeStr = plugin.getConfigManager().getConfig("config").getString("staff-mode.gamemode", "CREATIVE");
        try {
            staffGameMode = GameMode.valueOf(gameModeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("GameMode inválido en config.yml: " + gameModeStr);
        }
        player.setGameMode(staffGameMode);

        // Añadimos los ítems de staff usando los slots configurados
        Map<String, Integer> itemSlots = staffItems.getItemSlots();

        if (!itemSlots.isEmpty()) {
            // Usamos los slots configurados en el sistema nuevo
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

        // Actualizamos el inventario
        player.updateInventory();

        // Aplicamos vanish si es necesario
        UUID uuid = player.getUniqueId();
        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        if (staffPlayer.isVanished()) {
            vanishManager.applyVanishEffect(uuid, player);
        }
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
                    staffPlayer.sendMessage(plugin.getConfigManager().getMessage("system.no-target"));
                }
                break;
            case "inspect":
                if (targetPlayer != null) {
                    inspectionManager.openInspectInventory(staffPlayer, targetPlayer);
                } else {
                    staffPlayer.sendMessage(plugin.getConfigManager().getMessage("system.no-target"));
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
            case "online_players":
                inspectionManager.openOnlinePlayersMenu(staffPlayer);
                break;
            // Otras acciones personalizadas aquí
        }
    }

    // Getters para los componentes y estado
    public boolean isInStaffMode(Player player) {
        return staffModeEnabled.contains(player.getUniqueId());
    }

    public boolean isVanished(Player player) {
        return vanishManager.isVanished(player.getUniqueId());
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
}