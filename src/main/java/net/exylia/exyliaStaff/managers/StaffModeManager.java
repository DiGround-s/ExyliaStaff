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

public class StaffModeManager {
    private final ExyliaStaff plugin;
    private final StaffItems staffItems;
    private final Map<UUID, StaffPlayer> staffPlayers;
    private final Set<UUID> staffModeEnabled;
    private final Set<UUID> vanished;

    public StaffModeManager(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.staffItems = new StaffItems(plugin);
        this.staffPlayers = new HashMap<>();
        this.staffModeEnabled = new HashSet<>();
        this.vanished = new HashSet<>();
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

        player.sendMessage(plugin.getConfigManager().getMessage("vanish.disabled"));
        savePlayer(player);
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

        // Añadimos los ítems de staff
        List<String> itemSlots = plugin.getConfigManager().getConfig("config").getStringList("staff-mode.item-slots");
        if (itemSlots.isEmpty()) {
            // Configuración por defecto si no hay slots definidos
            player.getInventory().setItem(0, staffItems.getItem("teleport"));
            player.getInventory().setItem(1, staffItems.getItem("vanish"));
            player.getInventory().setItem(2, staffItems.getItem("freeze"));
            player.getInventory().setItem(3, staffItems.getItem("inspect"));
            player.getInventory().setItem(8, staffItems.getItem("exit"));
        } else {
            for (String itemSlot : itemSlots) {
                String[] parts = itemSlot.split(":");
                if (parts.length != 2) continue;

                try {
                    int slot = Integer.parseInt(parts[0]);
                    String itemKey = parts[1];
                    ItemStack item = staffItems.getItem(itemKey);

                    if (item != null) {
                        player.getInventory().setItem(slot, item);
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Slot inválido en config.yml: " + parts[0]);
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

    public void checkStaffItem(Player player, ItemStack item) {
        if (item == null) return;
        if (!isInStaffMode(player)) return;

        String itemKey = staffItems.getStaffItemKey(item);
        if (itemKey == null) return;

        switch (itemKey) {
            case "exit":
                disableStaffMode(player);
                break;
            case "vanish":
                toggleVanish(player);
                break;
            // Los demás ítems se manejarán con eventos específicos
        }
    }
}