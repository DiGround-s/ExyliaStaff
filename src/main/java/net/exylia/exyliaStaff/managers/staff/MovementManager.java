package net.exylia.exyliaStaff.managers.staff;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;

import static net.exylia.commons.utils.ColorUtils.sendPlayerMessage;

/**
 * Handles staff player movement features like phasing and teleportation
 */
public class MovementManager {
    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;

    // Para las colas de teletransporte aleatorio
    private final Map<UUID, Queue<UUID>> playerTeleportQueues;

    public MovementManager(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
        this.playerTeleportQueues = new HashMap<>();
    }

    public void phasePlayer(Player staffPlayer, @Nullable Action action) {
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
            // Didn't find a "safe" location with ground, but did find air blocks
            // We'll teleport anyway, but warn about no ground
            safeLocation.setPitch(player.getLocation().getPitch());
            safeLocation.setYaw(player.getLocation().getYaw());

            player.teleport(safeLocation);
            player.sendMessage(plugin.getConfigManager().getMessage("actions.phase.phased-no-ground"));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("actions.phase.no-safe-location"));
        }
    }

    public void teleportToRandomPlayer(Player staffPlayer) {
        Queue<UUID> queue = playerTeleportQueues.computeIfAbsent(staffPlayer.getUniqueId(), k -> new LinkedList<>());

        if (queue.isEmpty() || !isQueueValid(queue)) {
            queue.clear();
            List<UUID> candidates = new ArrayList<>();

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(staffPlayer) && !staffModeManager.isInStaffMode(online)) {
                    candidates.add(online.getUniqueId());
                }
            }

            if (candidates.isEmpty()) {
                staffPlayer.sendMessage(plugin.getConfigManager().getMessage("system.no-players"));
                return;
            }

            // Shuffle the candidates for randomness
            Collections.shuffle(candidates);
            queue.addAll(candidates);
        }

        UUID targetUUID = queue.poll();
        Player target = Bukkit.getPlayer(targetUUID);

        if (target != null) {
            staffPlayer.teleport(target.getLocation());
            sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("actions.random-tp.teleported", "%player%", target.getName()));
        } else {
            // If the target player is no longer available, try again
            teleportToRandomPlayer(staffPlayer);
        }
    }

    private boolean isQueueValid(Queue<UUID> queue) {
        for (UUID uuid : queue) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && !staffModeManager.isInStaffMode(player)) {
                return true;
            }
        }
        return false;
    }
}