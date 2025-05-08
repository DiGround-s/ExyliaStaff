package net.exylia.exyliaStaff.managers;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.database.tables.StaffPlayerTable;
import net.exylia.exyliaStaff.models.StaffPlayer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static net.exylia.commons.utils.ColorUtils.sendPlayerMessage;

public class StaffModeManager {
    private final ExyliaStaff plugin;
    private final StaffItems staffItems;
    private final Map<UUID, StaffPlayer> staffPlayers;
    private final Set<UUID> staffModeEnabled;
    private final Set<UUID> vanished;
    private final Set<UUID> frozenPlayers;

    public StaffModeManager(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.staffItems = new StaffItems(plugin);
        this.staffPlayers = new HashMap<>();
        this.staffModeEnabled = new HashSet<>();
        this.vanished = new HashSet<>();
        this.frozenPlayers = new HashSet<>();
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
                        vanished.add(uuid);
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
        UUID uuid = player.getUniqueId();
        if (!staffPlayers.containsKey(uuid)) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                StaffPlayerTable staffPlayerTable = plugin.getDatabaseLoader().getStaffPlayerTable();
                staffPlayerTable.saveStaffPlayer(staffPlayers.get(uuid));
            }
        }.runTaskAsynchronously(plugin);
    }

    public void saveAllPlayers() {
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

    public void unloadPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        staffPlayers.remove(uuid);
        staffModeEnabled.remove(uuid);
        vanished.remove(uuid);
        frozenPlayers.remove(uuid);
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
        enableVanish(player);

        // Notificamos y guardamos en DB
        player.sendMessage(plugin.getConfigManager().getMessage("staff-mode.enabled"));
        savePlayer(player);

        // Aplicamos los cambios en el inventario
        applyStaffMode(player);
    }

    public void disableStaffMode(Player player) {
        UUID uuid = player.getUniqueId();

        if (!staffPlayers.containsKey(uuid)) return;

        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        if (!staffPlayer.isInStaffMode()) return;

        // Actualizamos el estado
        staffPlayer.setInStaffMode(false);
        staffModeEnabled.remove(uuid);

        // Si estaba vanished, lo desvanecemos
        if (staffPlayer.isVanished()) {
            staffPlayer.setVanished(false);
            vanished.remove(uuid);
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(plugin, player);
            }
        }

        // Notificamos y guardamos en DB
        player.sendMessage(plugin.getConfigManager().getMessage("staff-mode.disabled"));

        // Restauramos el inventario original
        restorePlayerInventory(player);

        // Eliminamos el inventario almacenado
        staffPlayer.setInventory(null);
        staffPlayer.setArmor(null);
        staffPlayer.setOffHandItem(null);

        // Guardamos en DB
        savePlayer(player);
    }

    public void toggleVanish(Player player) {
        UUID uuid = player.getUniqueId();

        if (!staffPlayers.containsKey(uuid)) {
            staffPlayers.put(uuid, new StaffPlayer(uuid, false, false));
        }

        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        if (staffPlayer.isVanished()) {
            disableVanish(player);
        } else {
            enableVanish(player);
        }
    }

    public void enableVanish(Player player) {
        UUID uuid = player.getUniqueId();

        if (!staffPlayers.containsKey(uuid)) {
            staffPlayers.put(uuid, new StaffPlayer(uuid, false, false));
        }

        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        if (staffPlayer.isVanished()) return;

        staffPlayer.setVanished(true);
        vanished.add(uuid);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission("exyliastaff.see-vanished")) {
                online.hidePlayer(plugin, player);
            }
        }

        // Update the vanish item if player is in staff mode
        if (staffPlayer.isInStaffMode()) {
            updateVanishItem(player, true);
        }

        player.sendMessage(plugin.getConfigManager().getMessage("vanish.enabled"));
        savePlayer(player);
    }

    public void disableVanish(Player player) {
        UUID uuid = player.getUniqueId();

        if (!staffPlayers.containsKey(uuid)) return;

        StaffPlayer staffPlayer = staffPlayers.get(uuid);

        if (!staffPlayer.isVanished()) return;

        staffPlayer.setVanished(false);
        vanished.remove(uuid);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }

        // Update the vanish item if player is in staff mode
        if (staffPlayer.isInStaffMode()) {
            updateVanishItem(player, false);
        }

        player.sendMessage(plugin.getConfigManager().getMessage("vanish.disabled"));
        savePlayer(player);
    }

    // New method to update the vanish item based on vanish state
    private void updateVanishItem(Player player, boolean vanished) {
        // Check if we have the vanish item and if it has alternative state
        if (!staffItems.hasAlternateState("vanish")) return;

        // Find the slot where the vanish item is supposed to be
        int slot = staffItems.getSlot("vanish");
        if (slot == -1) return;

        // Get the appropriate item based on the vanish state
        ItemStack vanishItem;
        if (vanished) {
            // If vanished, get the alternate state item (e.g., LIME_DYE)
            vanishItem = staffItems.getAlternateStateItem("vanish");
        } else {
            // If not vanished, get the normal state item (e.g., GRAY_DYE)
            vanishItem = staffItems.getItem("vanish");
        }

        if (vanishItem != null) {
            player.getInventory().setItem(slot, vanishItem);
            player.updateInventory();
        }
    }

    public void toggleFreezePlayer(Player staffPlayer, Player targetPlayer) {
        UUID targetUUID = targetPlayer.getUniqueId();

        if (frozenPlayers.contains(targetUUID)) {
            unfreezePlayer(staffPlayer, targetPlayer);
        } else {
            freezePlayer(staffPlayer, targetPlayer);
        }
    }

    public void freezePlayer(Player staffPlayer, Player targetPlayer) {
        UUID targetUUID = targetPlayer.getUniqueId();

        if (frozenPlayers.contains(targetUUID)) return;

        frozenPlayers.add(targetUUID);
        targetPlayer.setWalkSpeed(0);
        targetPlayer.setFlySpeed(0);
        targetPlayer.setInvulnerable(true);

        // Notificar al staff y al jugador congelado
        sendPlayerMessage(targetPlayer, plugin.getConfigManager().getMessage("freeze.player-frozen", "%staff%", staffPlayer.getName()));
        sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("freeze.target-frozen", "%player%", targetPlayer.getName()));
    }

    public void unfreezePlayer(Player staffPlayer, Player targetPlayer) {
        UUID targetUUID = targetPlayer.getUniqueId();

        if (!frozenPlayers.contains(targetUUID)) return;

        frozenPlayers.remove(targetUUID);
        targetPlayer.setWalkSpeed(0.2f); // Valor predeterminado
        targetPlayer.setFlySpeed(0.1f);  // Valor predeterminado
        targetPlayer.setInvulnerable(false);

        // Notificar al staff y al jugador descongelado
        sendPlayerMessage(targetPlayer, plugin.getConfigManager().getMessage("freeze.player-unfrozen", "%staff%", staffPlayer.getName()));
        sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("freeze.target-unfrozen","%player%", targetPlayer.getName()));
    }

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
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
            vanished.add(uuid);
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.hasPermission("exyliastaff.see-vanished")) {
                    online.hidePlayer(plugin, player);
                }
            }
        }
    }

    public boolean isInStaffMode(Player player) {
        return staffModeEnabled.contains(player.getUniqueId());
    }

    public boolean isVanished(Player player) {
        return vanished.contains(player.getUniqueId());
    }

    public StaffItems getStaffItems() {
        return staffItems;
    }

    public void executeStaffItemAction(Player staffPlayer, ItemStack item, Player targetPlayer) {
        if (item == null) return;
        if (!isInStaffMode(staffPlayer)) return;

        if (!staffItems.isStaffItem(item)) return;

        String itemKey = staffItems.getStaffItemKey(item);
        String action = staffItems.getItemAction(item);

        if (itemKey == null || action == null) return;

        switch (action) {
            case "exit":
                disableStaffMode(staffPlayer);
                break;
            case "vanish":
            case "un_vanish":  // Handle both vanish states
                toggleVanish(staffPlayer);
                break;
            case "freeze":
                if (targetPlayer != null) {
                    toggleFreezePlayer(staffPlayer, targetPlayer);
                } else {
                    staffPlayer.sendMessage(plugin.getConfigManager().getMessage("freeze.no-target"));
                }
                break;
            case "inspect":
                if (targetPlayer != null) {
                    openInspectInventory(staffPlayer, targetPlayer);
                } else {
                    staffPlayer.sendMessage(plugin.getConfigManager().getMessage("inspect.no-target"));
                }
                break;
            case "player_command":
                if (staffItems.hasCommands(itemKey)) {
                    executePlayerCommands(staffPlayer, targetPlayer, itemKey);
                }
                break;
            case "console_command":
                if (staffItems.hasCommands(itemKey)) {
                    executeConsoleCommands(staffPlayer, targetPlayer, itemKey);
                }
                break;
            case "random_player_tp":
                teleportToRandomPlayer(staffPlayer);
                break;
            case "online_players":
                openOnlinePlayersMenu(staffPlayer);
                break;
            // Otras acciones personalizadas aquí
        }
    }

    private void openInspectInventory(Player staffPlayer, Player targetPlayer) {
        // Implementación para abrir el inventario del jugador objetivo
        sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("inspect.opened", "%player%", targetPlayer.getName()));

        // Aquí deberías implementar la lógica para abrir un inventario con los items del jugador objetivo
        // Por ejemplo, usando un sistema de GUI personalizado
    }

    private void executePlayerCommands(Player staffPlayer, Player targetPlayer, String itemKey) {
        List<String> commands = staffItems.getItemCommands(itemKey);

        for (String cmd : commands) {
            // Reemplazar variables
            String processedCmd = cmd
                    .replace("%staff%", staffPlayer.getName())
                    .replace("%player%", targetPlayer != null ? targetPlayer.getName() : "");

            // Eliminar el / inicial si existe
            if (processedCmd.startsWith("/")) {
                processedCmd = processedCmd.substring(1);
            }

            // Ejecutar el comando como el jugador
            staffPlayer.performCommand(processedCmd);
        }
    }

    private void executeConsoleCommands(Player staffPlayer, Player targetPlayer, String itemKey) {
        List<String> commands = staffItems.getItemCommands(itemKey);

        for (String cmd : commands) {
            // Reemplazar variables
            String processedCmd = cmd
                    .replace("%staff%", staffPlayer.getName())
                    .replace("%player%", targetPlayer != null ? targetPlayer.getName() : "");

            // Eliminar el / inicial si existe
            if (processedCmd.startsWith("/")) {
                processedCmd = processedCmd.substring(1);
            }

            // Ejecutar el comando como la consola
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd);
        }
    }

    private void teleportToRandomPlayer(Player staffPlayer) {
        List<Player> availablePlayers = new ArrayList<>();

        // Obtener jugadores que no estén en modo staff
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(staffPlayer) && !isInStaffMode(online)) {
                availablePlayers.add(online);
            }
        }

        if (availablePlayers.isEmpty()) {
            staffPlayer.sendMessage(plugin.getConfigManager().getMessage("random-tp.no-players"));
            return;
        }

        // Seleccionar un jugador aleatorio
        Player randomPlayer = availablePlayers.get(new Random().nextInt(availablePlayers.size()));

        // Teleportar al staff al jugador aleatorio
        staffPlayer.teleport(randomPlayer.getLocation());
        sendPlayerMessage(randomPlayer, plugin.getConfigManager().getMessage("random-tp.teleported", "%player%", staffPlayer.getName()));
    }

    private void openOnlinePlayersMenu(Player staffPlayer) {
        // Aquí implementarías la lógica para mostrar un menú con los jugadores en línea
        // Por ejemplo, usando un sistema de GUI personalizado

        staffPlayer.sendMessage(plugin.getConfigManager().getMessage("online-players.opened"));

        // Esta es una implementación simple que muestra los jugadores en el chat
        StringBuilder playerList = new StringBuilder();
        int count = 0;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(staffPlayer)) {
                count++;
                playerList.append("\n").append(count).append(". ").append(online.getName());
                if (isInStaffMode(online)) {
                    playerList.append(" (Staff)");
                }
                if (isVanished(online)) {
                    playerList.append(" (Vanished)");
                }
                if (isFrozen(online)) {
                    playerList.append(" (Frozen)");
                }
            }
        }

        if (count > 0) {
            sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("online-players.list", "%count%", String.valueOf(count), "%players%", playerList.toString()));
        } else {
            staffPlayer.sendMessage(plugin.getConfigManager().getMessage("online-players.no-players"));
        }
    }
}