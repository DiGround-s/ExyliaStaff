package net.exylia.exyliaStaff.managers.staff;

import net.exylia.commons.command.CommandExecutor;
import net.exylia.commons.utils.MessageUtils;
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

import static net.exylia.commons.utils.DebugUtils.logWarn;
import static net.exylia.commons.utils.TeleportUtils.teleportToGround;

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
    private final Map<UUID, UUID> frozenByStaff; // playerUUID -> staffUUID

    public FreezeManager(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
        this.frozenPlayers = new HashSet<>();
        this.frozenPlayerTasks = new HashMap<>();
        this.frozenPlayerLocations = new HashMap<>();
        this.frozenPlayerHelmets = new HashMap<>();
        this.frozenByStaff = new HashMap<>();
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

        if (staffModeManager.isInStaffMode(targetPlayer) || targetPlayer.hasPermission("exyliastaff.staff")) {
            MessageUtils.sendMessageAsync(staffPlayer, plugin.getConfigManager().getMessage("actions.freeze.cannot-freeze-staff", "%player%", targetPlayer.getName()));
            return;
        }

        if (frozenPlayers.contains(targetUUID)) return;

        frozenPlayers.add(targetUUID);
        frozenByStaff.put(targetUUID, staffPlayer.getUniqueId()); // Agregar esta línea

        teleportToGround(targetPlayer);
        teleportToGround(staffPlayer);

        startFrozenPlayerTask(targetPlayer, staffPlayer);

        MessageUtils.sendMessageAsync(targetPlayer, plugin.getConfigManager().getMessage("actions.freeze.frozen", "%staff%", staffPlayer.getName()));
        MessageUtils.sendMessageAsync(staffPlayer, plugin.getConfigManager().getMessage("actions.freeze.frozen-staff", "%player%", targetPlayer.getName()));

        frozenPlayerHelmets.put(targetUUID, targetPlayer.getInventory().getHelmet());
        targetPlayer.getInventory().setHelmet(new ItemStack(Material.ICE, 1));
    }

    public void unfreezePlayer(Player staffPlayer, Player targetPlayer) {
        UUID targetUUID = targetPlayer.getUniqueId();

        if (!frozenPlayers.contains(targetUUID)) return;

        frozenPlayers.remove(targetUUID);
        frozenByStaff.remove(targetUUID); // Agregar esta línea
        targetPlayer.setWalkSpeed(0.2f);
        targetPlayer.setFlySpeed(0.1f);
        targetPlayer.setInvulnerable(false);

        stopFrozenPlayerTask(targetUUID);

        MessageUtils.sendMessageAsync(targetPlayer, plugin.getConfigManager().getMessage("actions.freeze.un-frozen", "%staff%", staffPlayer.getName()));
        MessageUtils.sendMessageAsync(staffPlayer, plugin.getConfigManager().getMessage("actions.freeze.un-frozen-staff", "%player%", targetPlayer.getName()));

        ItemStack helmet = frozenPlayerHelmets.get(targetUUID);
        targetPlayer.getInventory().setHelmet(helmet);

        frozenPlayerHelmets.remove(targetUUID);
    }

    private void startFrozenPlayerTask(Player targetPlayer, Player staffPlayer) {
        UUID targetUUID = targetPlayer.getUniqueId();
        String staffName = staffPlayer.getName();

        Location loc = targetPlayer.getLocation();
        frozenPlayerLocations.put(targetUUID, new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch()));

        if (frozenPlayerTasks.containsKey(targetUUID)) {
            frozenPlayerTasks.get(targetUUID).cancel();
        }

        int taskDelay = plugin.getConfigManager().getConfig("config").getInt("frozen.task_delay", 100);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(targetUUID);

                if (player == null) {
                    if (plugin.getConfigManager().getConfig("config").getBoolean("frozen.commands-on-disconnect.enabled", true)) {
                        handlePlayerDisconnectWhileFrozen(targetUUID, staffPlayer);
                    }

                    frozenPlayers.remove(targetUUID);
                    frozenPlayerLocations.remove(targetUUID);
                    frozenPlayerTasks.remove(targetUUID);
                    this.cancel();
                    return;
                }

                Location savedLocation = frozenPlayerLocations.get(targetUUID);
//                if (savedLocation != null) {
//                    player.teleport(savedLocation);
//                }

                if (plugin.getConfigManager().getConfig("config").getBoolean("frozen.sound.enabled", true)) {
                    applySoundToFrozenPlayer(player);
                }

                if (plugin.getConfigManager().getConfig("config").getBoolean("frozen.effects.enabled", true)) {
                    applyEffectsToFrozenPlayer(player);
                }

                MessageUtils.sendMessageAsync(player, plugin.getConfigManager().getMessage("actions.freeze.repetitive-to-target", "%staff%", staffName));
            }
        }.runTaskTimer(plugin, 1, taskDelay);

        frozenPlayerTasks.put(targetUUID, task);
    }

    private void stopFrozenPlayerTask(UUID targetUUID) {
        if (frozenPlayerTasks.containsKey(targetUUID)) {
            frozenPlayerTasks.get(targetUUID).cancel();
            frozenPlayerTasks.remove(targetUUID);
        }
        frozenPlayerLocations.remove(targetUUID);
    }

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
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
            }
        } catch (Exception e) {
            logWarn("Error al reproducir sonido para jugador congelado: " + e.getMessage());
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.0f);
        }
    }

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
                logWarn("Error al aplicar efecto de poción a jugador congelado: " + e.getMessage());
            }
        }
    }

    public void handlePlayerDisconnectWhileFrozen(UUID playerUUID, Player staffPlayer) {
        Player player = Bukkit.getPlayer(playerUUID);

        unfreezePlayer(staffPlayer, player);

        boolean enabled = plugin.getConfigManager().getConfig("config").getBoolean("frozen.commands-on-disconnect.enabled", true);

        if (enabled) {
            List<String> commands = plugin.getConfigManager().getConfig("config").getStringList("frozen.commands-on-disconnect.commands");

            if (!commands.isEmpty()) {
                CommandExecutor.builder(staffPlayer)
                        .withPlaceholderPlayer(player)
                        .execute(commands);
            }

        }
    }


    public boolean isFrozen(Player player) {
        return frozenPlayers.contains(player.getUniqueId());
    }

    public void removeFromFrozenPlayers(UUID playerUuid) {
        frozenPlayers.remove(playerUuid);
        frozenByStaff.remove(playerUuid);
        stopFrozenPlayerTask(playerUuid);
        frozenPlayerLocations.remove(playerUuid);
        frozenPlayerHelmets.remove(playerUuid);
    }

    public UUID getStaffWhoFroze(UUID playerUUID) {
        return frozenByStaff.get(playerUUID);
    }

    public Player getStaffPlayerWhoFroze(UUID playerUUID) {
        UUID staffUUID = frozenByStaff.get(playerUUID);
        if (staffUUID != null) {
            return Bukkit.getPlayer(staffUUID);
        }
        return null;
    }

    public String getStaffNameWhoFroze(UUID playerUUID) {
        UUID staffUUID = frozenByStaff.get(playerUUID);
        if (staffUUID != null) {
            Player staffPlayer = Bukkit.getPlayer(staffUUID);
            if (staffPlayer != null) {
                return staffPlayer.getName();
            } else {
                // Si el staff no está online, obtener el nombre del OfflinePlayer
                return Bukkit.getOfflinePlayer(staffUUID).getName();
            }
        }
        return null;
    }

    public boolean wasFrozenBy(UUID playerUUID, UUID staffUUID) {
        UUID frozenByUUID = frozenByStaff.get(playerUUID);
        return frozenByUUID != null && frozenByUUID.equals(staffUUID);
    }
}