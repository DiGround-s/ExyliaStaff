package net.exylia.exyliaStaff.managers.staff;

import net.exylia.commons.utils.MessageUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
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

/**
 * Handles staff player movement features like phasing and teleportation and also the teleport queue
 */
public class MovementManager {
    private final ExyliaStaff plugin;
    private final StaffManager staffManager;

    private final Map<UUID, Queue<UUID>> playerTeleportQueues;

    public MovementManager(ExyliaStaff plugin, StaffManager staffManager) {
        this.plugin = plugin;
        this.staffManager = staffManager;
        this.playerTeleportQueues = new HashMap<>();
    }

    public void phasePlayer(Player staffPlayer, @Nullable Action action) {
        if (action == null) return;

        int maxDistance = plugin.getConfigManager().getConfig("modules/staff-mode").getInt("staff-mode.phase-distance", 100);

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            teleportToTargetBlock(staffPlayer, maxDistance);
        } else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            phaseThrough(staffPlayer, maxDistance);
        }
    }

    private void teleportToTargetBlock(Player player, int maxDistance) {
        Block targetBlock = player.getTargetBlock(null, maxDistance);

        if (targetBlock == null || targetBlock.getType().isAir()) {
            MessageUtils.sendMessageAsync(player, (plugin.getConfigManager().getMessage("actions.phase.no-block-in-sight")));
            return;
        }

        Location baseLocation = targetBlock.getLocation();
        World world = targetBlock.getWorld();
        int x = baseLocation.getBlockX();
        int z = baseLocation.getBlockZ();

        int worldMaxY = world.getMaxHeight();

        for (int y = baseLocation.getBlockY(); y <= worldMaxY - 2; y++) {
            Block current = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);

            if (!current.getType().isSolid() && !above.getType().isSolid()) {
                Location safeLoc = current.getLocation().add(0.5, 0, 0.5);
                safeLoc.setYaw(player.getLocation().getYaw());
                safeLoc.setPitch(player.getLocation().getPitch());

                player.teleport(safeLoc);
                MessageUtils.sendMessageAsync(player, (plugin.getConfigManager().getMessage("actions.phase.teleported")));
                return;
            }
        }

        MessageUtils.sendMessageAsync(player, (plugin.getConfigManager().getMessage("actions.phase.no-safe-location")));
    }

    private void phaseThrough(Player player, int maxDistance) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();

        Location checkLoc = eyeLocation.clone();
        boolean foundSafeLocation = false;
        Location safeLocation = null;

        double step = 0.5;
        int iterations = (int) Math.ceil(maxDistance / step);

        checkLoc.add(direction.clone().multiply(0.5));

        for (int i = 0; i < iterations; i++) {
            checkLoc.add(direction.clone().multiply(step));

            if (checkLoc.getBlockY() < player.getWorld().getMinHeight() ||
                    checkLoc.getBlockY() > player.getWorld().getMaxHeight()) {
                continue;
            }

            boolean inSolid = checkLoc.getBlock().getType().isSolid();

            Block feetBlock = checkLoc.getBlock();
            Block headBlock = checkLoc.getBlock().getRelative(BlockFace.UP);
            Block groundBlock = checkLoc.getBlock().getRelative(BlockFace.DOWN);

            boolean hasTwoAirBlocks = (!feetBlock.getType().isSolid() && !headBlock.getType().isSolid());
            boolean hasGround = groundBlock.getType().isSolid();

            if (inSolid && i > 0) {
                continue;
            } else if (!inSolid && hasTwoAirBlocks) {
                if (hasGround) {
                    safeLocation = groundBlock.getLocation().clone().add(0.5, 1, 0.5);
                    foundSafeLocation = true;
                    break;
                } else if (safeLocation == null) {
                    safeLocation = checkLoc.clone();
                }
            }
        }

        if (foundSafeLocation && safeLocation != null) {
            safeLocation.setPitch(player.getLocation().getPitch());
            safeLocation.setYaw(player.getLocation().getYaw());

            player.teleport(safeLocation);
            MessageUtils.sendMessageAsync(player, (plugin.getConfigManager().getMessage("actions.phase.phased-through")));
        } else if (safeLocation != null) {
            safeLocation.setPitch(player.getLocation().getPitch());
            safeLocation.setYaw(player.getLocation().getYaw());

            player.teleport(safeLocation);
            MessageUtils.sendMessageAsync(player, (plugin.getConfigManager().getMessage("actions.phase.no-ground")));
        } else {
            MessageUtils.sendMessageAsync(player, (plugin.getConfigManager().getMessage("actions.phase.no-safe-location")));
        }
    }

    public void teleportToRandomPlayer(Player staffPlayer) {
        Queue<UUID> queue = playerTeleportQueues.computeIfAbsent(staffPlayer.getUniqueId(), k -> new LinkedList<>());

        if (queue.isEmpty() || !isQueueValid(queue)) {
            queue.clear();
            List<UUID> candidates = new ArrayList<>();

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(staffPlayer) && !staffManager.getStaffModeManager().isInStaffMode(online)) {
                    candidates.add(online.getUniqueId());
                }
            }

            if (candidates.isEmpty()) {
                MessageUtils.sendMessageAsync(staffPlayer, (plugin.getConfigManager().getMessage("system.no-players")));
                return;
            }

            Collections.shuffle(candidates);
            queue.addAll(candidates);
        }

        UUID targetUUID = queue.poll();
        Player target = Bukkit.getPlayer(targetUUID);

        if (target != null) {
            staffPlayer.teleport(target.getLocation());
            MessageUtils.sendMessageAsync(staffPlayer, plugin.getConfigManager().getMessage("actions.random-tp.teleported", "%player%", target.getName()));
        } else {
            teleportToRandomPlayer(staffPlayer);
        }
    }

    private boolean isQueueValid(Queue<UUID> queue) {
        for (UUID uuid : queue) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && !staffManager.getStaffModeManager().isInStaffMode(player)) {
                return true;
            }
        }
        return false;
    }
}