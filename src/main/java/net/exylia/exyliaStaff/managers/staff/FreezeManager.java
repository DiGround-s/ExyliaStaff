package net.exylia.exyliaStaff.managers.staff;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static net.exylia.commons.utils.ColorUtils.sendPlayerMessage;

/**
 * Manages the freezing of players by staff members
 */
public class FreezeManager {
    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;
    private final Set<UUID> frozenPlayers;

    private final Map<UUID, BukkitTask> frozenPlayerTasks;
    private final Map<UUID, Location> frozenPlayerLocations;
    private final Map<UUID, ItemStack> frozenPlayerHelmets;

    public FreezeManager(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
        this.frozenPlayers = new HashSet<>();
        this.frozenPlayerTasks = new HashMap<>();
        this.frozenPlayerLocations = new HashMap<>();
        this.frozenPlayerHelmets = new HashMap<>();
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
        targetPlayer.setWalkSpeed(0.2f);
        targetPlayer.setFlySpeed(0.1f);
        targetPlayer.setInvulnerable(false);

        stopFrozenPlayerTask(targetUUID);

        sendPlayerMessage(targetPlayer, plugin.getConfigManager().getMessage("actions.freeze.un-frozen", "%staff%", staffPlayer.getName()));
        sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("actions.freeze.un-frozen-staff", "%player%", targetPlayer.getName()));

        ItemStack helmet = frozenPlayerHelmets.get(targetUUID);
        targetPlayer.getInventory().setHelmet(helmet);

        frozenPlayerHelmets.remove(targetUUID);
    }

    private void startFrozenPlayerTask(Player targetPlayer, Player staffPlayer) {
        UUID targetUUID = targetPlayer.getUniqueId();
        String staffName = staffPlayer.getName();

        // Guardamos la ubicación inicial
        Location loc = targetPlayer.getLocation();
        frozenPlayerLocations.put(targetUUID, new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));

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

    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    public void removeFromFrozenPlayers(UUID playerUuid) {
        frozenPlayers.remove(playerUuid);
        stopFrozenPlayerTask(playerUuid);
        frozenPlayerLocations.remove(playerUuid);
        frozenPlayerHelmets.remove(playerUuid);
    }
}