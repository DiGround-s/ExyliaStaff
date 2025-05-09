package net.exylia.exyliaStaff.managers;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.database.tables.StaffPlayerTable;
import net.exylia.exyliaStaff.models.StaffPlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;

import static net.exylia.commons.utils.ColorUtils.sendPlayerMessage;

public class StaffModeManager {
    private final ExyliaStaff plugin;
    private final StaffItems staffItems;
    private final Map<UUID, StaffPlayer> staffPlayers;
    private final Set<UUID> staffModeEnabled;
    private final Set<UUID> vanished;
    private final Set<UUID> frozenPlayers;

    // Para las tareas individuales de jugadores congelados
    private final Map<UUID, BukkitTask> frozenPlayerTasks;
    private final Map<UUID, Location> frozenPlayerLocations;
    private final Map<UUID, ItemStack> frozenPlayerHelmets;

    public StaffModeManager(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.staffItems = new StaffItems(plugin);
        this.staffPlayers = new HashMap<>();
        this.staffModeEnabled = new HashSet<>();
        this.vanished = new HashSet<>();
        this.frozenPlayers = new HashSet<>();
        this.frozenPlayerTasks = new HashMap<>();
        this.frozenPlayerLocations = new HashMap<>();
        this.frozenPlayerHelmets = new HashMap<>();
    }

    /**
     * Inicia una tarea individual para un jugador congelado
     * @param targetPlayer El jugador congelado
     * @param staffPlayer El miembro del staff que congeló al jugador
     */
    private void startFrozenPlayerTask(Player targetPlayer, Player staffPlayer) {
        UUID targetUUID = targetPlayer.getUniqueId();
        String staffName = staffPlayer.getName();

        // Guardamos la ubicación inicial
        Location loc = targetPlayer.getLocation();
        frozenPlayerLocations.put(targetUUID, new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ()));

        // Cancelamos cualquier tarea existente para este jugador
        if (frozenPlayerTasks.containsKey(targetUUID)) {
            frozenPlayerTasks.get(targetUUID).cancel();
        }

        // Obtenemos la frecuencia de la tarea desde la configuración
        int taskDelay = plugin.getConfigManager().getConfig("config").getInt("frozen.task_delay", 100);

        // Creamos la nueva tarea para este jugador
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(targetUUID);

                // Si el jugador no está en línea, ejecutar comandos de desconexión
                if (player == null) {
                    // Ejecutar comandos configurados para desconexión
                    if (plugin.getConfigManager().getConfig("config").getBoolean("frozen.commands-on-disconnect.enabled", true)) {
                        handlePlayerDisconnectWhileFrozen(targetUUID);
                    }

                    // Limpiar las referencias
                    frozenPlayers.remove(targetUUID);
                    frozenPlayerLocations.remove(targetUUID);
                    frozenPlayerTasks.remove(targetUUID);
                    this.cancel();
                    return;
                }

                // Verificar que el jugador no se ha movido
                Location savedLocation = frozenPlayerLocations.get(targetUUID);
                if (savedLocation != null) {
                    player.teleport(savedLocation);
                }

                // Aplicar efectos de sonido
                if (plugin.getConfigManager().getConfig("config").getBoolean("frozen.sound.enabled", true)) {
                    applySoundToFrozenPlayer(player);
                }

                // Aplicar efectos de poción
                if (plugin.getConfigManager().getConfig("config").getBoolean("frozen.effects.enabled", true)) {
                    applyEffectsToFrozenPlayer(player);
                }

                // Enviar mensaje recordatorio
                sendPlayerMessage(player, plugin.getConfigManager().getMessage("actions.freeze.repetitive-to-target", "%staff%", staffName));
            }
        }.runTaskTimer(plugin, 1, taskDelay); // Inicia rápidamente (1 tick) y luego sigue con el intervalo configurado

        // Guardamos la referencia a la tarea
        frozenPlayerTasks.put(targetUUID, task);
    }

    /**
     * Detiene la tarea de un jugador congelado
     * @param targetUUID UUID del jugador a descongelar
     */
    private void stopFrozenPlayerTask(UUID targetUUID) {
        if (frozenPlayerTasks.containsKey(targetUUID)) {
            frozenPlayerTasks.get(targetUUID).cancel();
            frozenPlayerTasks.remove(targetUUID);
        }
        frozenPlayerLocations.remove(targetUUID);
    }

    /**
     * Aplica efectos de sonido al jugador congelado
     */
    private void applySoundToFrozenPlayer(Player player) {
        String soundConfig = plugin.getConfigManager().getConfig("config").getString("frozen.sound.sound", "BLOCK_NOTE_BLOCK_CHIME|1.0|1.0");

        try {
            String[] parts = soundConfig.split("\\|");
            if (parts.length >= 3) {
                Sound sound = Sound.valueOf(parts[0]);
                float volume = Float.parseFloat(parts[1]);
                float pitch = Float.parseFloat(parts[2]);

                player.playSound(player.getLocation(), sound, volume, pitch);
            } else {
                // Fallback si el formato no es correcto
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error al reproducir sonido para jugador congelado: " + e.getMessage());
            // Fallback con un sonido predeterminado
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        }
    }

    /**
     * Aplica efectos de poción al jugador congelado
     */
    private void applyEffectsToFrozenPlayer(Player player) {
        List<String> effectsList = plugin.getConfigManager().getConfig("config").getStringList("frozen.effects.effects");

        for (String effectString : effectsList) {
            try {
                String[] parts = effectString.split("\\|");
                if (parts.length >= 3) {
                    PotionEffectType type = PotionEffectType.getByName(parts[0]);
                    int amplifier = Integer.parseInt(parts[1]);
                    int durationTicks = Integer.parseInt(parts[2]);

                    if (type != null) {
                        PotionEffect effect = new PotionEffect(type, durationTicks, amplifier, false, true, true);
                        player.addPotionEffect(effect);
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error al aplicar efecto de poción a jugador congelado: " + e.getMessage());
            }
        }
    }

    /**
     * Maneja la desconexión de un jugador mientras está congelado
     */
    private void handlePlayerDisconnectWhileFrozen(UUID playerUUID) {
        String playerName = Bukkit.getOfflinePlayer(playerUUID).getName();
        if (playerName == null) playerName = playerUUID.toString();

        // Ejecutar comandos de consola
        if (plugin.getConfigManager().getConfig("config").getBoolean("frozen.commands-on-disconnect.enabled", true)) {
            List<String> consoleCommands = plugin.getConfigManager().getConfig("config").getStringList("frozen.commands-on-disconnect.console_commands");

            for (String cmd : consoleCommands) {
                String processedCmd = cmd.replace("%player%", playerName);

                // Quitar el slash inicial si existe
                if (processedCmd.startsWith("/")) {
                    processedCmd = processedCmd.substring(1);
                }

                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCmd);
            }

            // Notificar a todos los staff y ejecutar comandos como staff
            List<String> staffCommands = plugin.getConfigManager().getConfig("config").getStringList("frozen.commands-on-disconnect.staff_commands");

            for (Player staffPlayer : Bukkit.getOnlinePlayers()) {
                if (staffPlayer.hasPermission("exyliastaff.notify")) {
                    sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("actions.freeze.disconnect", "%player%", playerName));

                    // Ejecutar comandos como cada miembro del staff
                    for (String cmd : staffCommands) {
                        String processedCmd = cmd.replace("%player%", playerName);

                        // Quitar el slash inicial si existe
                        if (processedCmd.startsWith("/")) {
                            processedCmd = processedCmd.substring(1);
                        }

                        staffPlayer.performCommand(processedCmd);
                    }
                }
            }
        }
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
        for (UUID uuid : staffModeEnabled) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                disableStaffMode(player);
            }
        }
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
        player.sendMessage(plugin.getConfigManager().getMessage("actions.staff-mode.enabled"));
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
        player.sendMessage(plugin.getConfigManager().getMessage("actions.staff-mode.disabled"));

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

        player.sendMessage(plugin.getConfigManager().getMessage("actions.vanish.enabled"));
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

        player.sendMessage(plugin.getConfigManager().getMessage("actions.vanish.disabled"));
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

        startFrozenPlayerTask(targetPlayer, staffPlayer);

        // Notificar al staff y al jugador congelado
        sendPlayerMessage(targetPlayer, plugin.getConfigManager().getMessage("actions.freeze.frozen", "%staff%", staffPlayer.getName()));
        sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("actions.freeze.frozen-staff", "%player%", targetPlayer.getName()));

        frozenPlayerHelmets.put(targetUUID, targetPlayer.getInventory().getHelmet());
        targetPlayer.getInventory().setHelmet(new ItemStack(Material.ICE, 1));
    }

    public void unfreezePlayer(Player staffPlayer, Player targetPlayer) {
        UUID targetUUID = targetPlayer.getUniqueId();

        if (!frozenPlayers.contains(targetUUID)) return;

        frozenPlayers.remove(targetUUID);
        targetPlayer.setWalkSpeed(0.2f); // Valor predeterminado
        targetPlayer.setFlySpeed(0.1f);  // Valor predeterminado
        targetPlayer.setInvulnerable(false);


        stopFrozenPlayerTask(targetUUID);

        // Notificar al staff y al jugador descongelado
        sendPlayerMessage(targetPlayer, plugin.getConfigManager().getMessage("actions.freeze.un-frozen", "%staff%", staffPlayer.getName()));
        sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("actions.freeze.un-frozen-staff", "%player%", targetPlayer.getName()));

        ItemStack helmet = frozenPlayerHelmets.get(targetUUID);
        if (helmet != null) {
            targetPlayer.getInventory().setHelmet(helmet);
        }

        frozenPlayerHelmets.remove(targetUUID);
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

    public void executeStaffItemAction(Player staffPlayer, ItemStack item, @Nullable Player targetPlayer, @Nullable Action action) {
        if (item == null) return;
        if (!isInStaffMode(staffPlayer)) return;

        if (!staffItems.isStaffItem(item)) return;

        String itemKey = staffItems.getStaffItemKey(item);
        String itemAction = staffItems.getItemAction(item);

        if (itemKey == null || itemAction == null) return;

        switch (itemAction) {
            case "phase":
                phasePlayer(staffPlayer, action);
                break;
            case "exit":
                disableStaffMode(staffPlayer);
                break;
            case "vanish":
            case "un_vanish":
                toggleVanish(staffPlayer);
                break;
            case "freeze":
                if (targetPlayer != null) {
                    toggleFreezePlayer(staffPlayer, targetPlayer);
                } else {
                    staffPlayer.sendMessage(plugin.getConfigManager().getMessage("system.no-target"));
                }
                break;
            case "inspect":
                if (targetPlayer != null) {
                    openInspectInventory(staffPlayer, targetPlayer);
                } else {
                    staffPlayer.sendMessage(plugin.getConfigManager().getMessage("system.no-target"));
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

    private void phasePlayer(Player staffPlayer, @Nullable Action action) {
        if (action == null) return;

        // Maximum distance for the raytracing
        int maxDistance = plugin.getConfigManager().getConfig("config").getInt("staff-mode.phase-distance", 100);

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            // Left-click: Teleport to the top of the block being looked at
            teleportToTargetBlock(staffPlayer, maxDistance);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            // Right-click: Phase through blocks until finding an open space
            phaseThrough(staffPlayer, maxDistance);
        }
    }

    private void teleportToTargetBlock(Player player, int maxDistance) {
        Block targetBlock = player.getTargetBlock(null, maxDistance);

        if (targetBlock == null || targetBlock.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().getMessage("actions.phase.no-block-in-sight"));
            return;
        }

        Location baseLocation = targetBlock.getLocation();
        World world = targetBlock.getWorld();
        int x = baseLocation.getBlockX();
        int z = baseLocation.getBlockZ();

        // Buscar hacia arriba desde la posición del bloque objetivo
        int worldMaxY = world.getMaxHeight();

        for (int y = baseLocation.getBlockY(); y <= worldMaxY - 2; y++) {
            Block current = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);

            if (!current.getType().isSolid() && !above.getType().isSolid()) {
                Location safeLoc = current.getLocation().add(0.5, 0, 0.5);
                safeLoc.setYaw(player.getLocation().getYaw());
                safeLoc.setPitch(player.getLocation().getPitch());

                player.teleport(safeLoc);
                player.sendMessage(plugin.getConfigManager().getMessage("actions.phase.teleported"));
                return;
            }
        }

        player.sendMessage(plugin.getConfigManager().getMessage("actions.phase.no-safe-location"));
    }


    private void phaseThrough(Player player, int maxDistance) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();

        // Start from current location and move in the player's looking direction
        Location checkLoc = eyeLocation.clone();
        boolean foundSafeLocation = false;
        Location safeLocation = null;

        // Increment step for better precision
        double step = 0.5;
        int iterations = (int) Math.ceil(maxDistance / step);

        // Start checking a bit forward from the player to avoid self-collision
        checkLoc.add(direction.clone().multiply(0.5));

        // Phase through blocks until we find an open space
        for (int i = 0; i < iterations; i++) {
            checkLoc.add(direction.clone().multiply(step));

            // Skip if we're outside the world
            if (checkLoc.getBlockY() < player.getWorld().getMinHeight() ||
                    checkLoc.getBlockY() > player.getWorld().getMaxHeight()) {
                continue;
            }

            // Check if current position is in a solid block
            boolean inSolid = checkLoc.getBlock().getType().isSolid();

            // Check if we have two air blocks for the player to stand in
            Block feetBlock = checkLoc.getBlock();
            Block headBlock = checkLoc.getBlock().getRelative(BlockFace.UP);
            Block groundBlock = checkLoc.getBlock().getRelative(BlockFace.DOWN);

            boolean hasTwoAirBlocks = (!feetBlock.getType().isSolid() && !headBlock.getType().isSolid());
            boolean hasGround = groundBlock.getType().isSolid();

            // Logic for finding a safe teleport location:
            // - If we're transitioning from solid to air AND
            // - We have room for the player (two air blocks vertically) AND
            // - There's a solid block below
            if (inSolid && i > 0) {
                // We're in a solid, keep going
                continue;
            } else if (!inSolid && hasTwoAirBlocks) {
                // Found potential safe spot (non-solid area)
                if (hasGround) {
                    // Found safe spot with ground
                    safeLocation = groundBlock.getLocation().clone().add(0.5, 1, 0.5);
                    foundSafeLocation = true;
                    break;
                } else if (safeLocation == null) {
                    // Remember this location but keep looking for one with ground
                    safeLocation = checkLoc.clone();
                }
            }
        }

        // If we found a safe location, teleport the player there
        if (foundSafeLocation && safeLocation != null) {
            // Preserve pitch and yaw
            safeLocation.setPitch(player.getLocation().getPitch());
            safeLocation.setYaw(player.getLocation().getYaw());

            player.teleport(safeLocation);
            player.sendMessage(plugin.getConfigManager().getMessage("actions.phase.phased-through"));
        } else if (safeLocation != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("actions.phase.phased-no-ground"));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("actions.phase.no-safe-location"));
        }
    }

    private void openInspectInventory(Player staffPlayer, Player targetPlayer) {
        // Implementación para abrir el inventario del jugador objetivo
        sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("actions.inspect.opened", "%player%", targetPlayer.getName()));

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

    private final Map<UUID, Queue<UUID>> playerTeleportQueues = new HashMap<>();

    private void teleportToRandomPlayer(Player staffPlayer) {
        Queue<UUID> queue = playerTeleportQueues.computeIfAbsent(staffPlayer.getUniqueId(), k -> new LinkedList<>());

        if (queue.isEmpty() || !isQueueValid(queue)) {
            queue.clear();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(staffPlayer) && !isInStaffMode(online)) {
                    queue.add(online.getUniqueId());
                }
            }

            if (queue.isEmpty()) {
                staffPlayer.sendMessage(plugin.getConfigManager().getMessage("system.no-players"));
                return;
            }

            List<UUID> shuffled = new ArrayList<>(queue);
            Collections.shuffle(shuffled);
            queue.addAll(shuffled);
        }

        UUID targetUUID = queue.poll();
        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null) {
            staffPlayer.teleport(target.getLocation());
            sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("actions.random-tp.teleported", "%player%", target.getName()));
        } else {
            teleportToRandomPlayer(staffPlayer);
        }
    }

    private boolean isQueueValid(Queue<UUID> queue) {
        for (UUID uuid : queue) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && !isInStaffMode(player)) {
                return true;
            }
        }
        return false;
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
            staffPlayer.sendMessage(plugin.getConfigManager().getMessage("system.no-players"));
        }
    }
}