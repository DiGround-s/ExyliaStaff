package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all player interaction events related to staff mode
 */
public class PlayerInteractionListener extends StaffModeListenerBase {

    public PlayerInteractionListener(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        super(plugin, staffModeManager);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isFrozen(player)) {
            event.setCancelled(true);
            return;
        }

        if (!isInStaffMode(player)) return;

        ItemStack item = event.getItem();
        if (item != null && staffModeManager.getStaffItems().isStaffItem(item)) {
            event.setCancelled(true);
            tryExecuteStaffItemAction(player, item, null, event.getAction());
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null && isInteractableBlock(clickedBlock.getType())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!isInStaffMode(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!(event.getRightClicked() instanceof Player targetPlayer)) return;

        event.setCancelled(true);
        tryExecuteStaffItemAction(player, item, targetPlayer, null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerLeftClickPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player targetPlayer)) return;

        if (!isInStaffMode(player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        event.setCancelled(true);
        tryExecuteStaffItemAction(player, item, targetPlayer, null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractPhysical(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isInStaffMode(player)) return;

        if (event.getAction() == Action.PHYSICAL) {
            event.setCancelled(true);
        }
    }

    /**
     * Checks if a block type is considered "interactable" and should be blocked in staff mode
     * @param material The block material to check
     * @return true if the block is interactable
     */
    private boolean isInteractableBlock(Material material) {
        switch (material) {
            // Doors
            case ACACIA_DOOR:
            case BIRCH_DOOR:
            case DARK_OAK_DOOR:
            case JUNGLE_DOOR:
            case OAK_DOOR:
            case SPRUCE_DOOR:
            case CRIMSON_DOOR:
            case WARPED_DOOR:
            case IRON_DOOR:
                // Trapdoors
            case ACACIA_TRAPDOOR:
            case BIRCH_TRAPDOOR:
            case DARK_OAK_TRAPDOOR:
            case JUNGLE_TRAPDOOR:
            case OAK_TRAPDOOR:
            case SPRUCE_TRAPDOOR:
            case CRIMSON_TRAPDOOR:
            case WARPED_TRAPDOOR:
                // Gates
            case ACACIA_FENCE_GATE:
            case BIRCH_FENCE_GATE:
            case DARK_OAK_FENCE_GATE:
            case JUNGLE_FENCE_GATE:
            case OAK_FENCE_GATE:
            case SPRUCE_FENCE_GATE:
            case CRIMSON_FENCE_GATE:
            case WARPED_FENCE_GATE:
                // Buttons
            case ACACIA_BUTTON:
            case BIRCH_BUTTON:
            case DARK_OAK_BUTTON:
            case JUNGLE_BUTTON:
            case OAK_BUTTON:
            case SPRUCE_BUTTON:
            case CRIMSON_BUTTON:
            case WARPED_BUTTON:
            case STONE_BUTTON:
            case POLISHED_BLACKSTONE_BUTTON:
                // Redstone
            case LEVER:
            case REPEATER:
            case COMPARATOR:
            case DAYLIGHT_DETECTOR:
                // Containers
            case DISPENSER:
            case DROPPER:
            case HOPPER:
            case BEACON:
            case CHEST:
            case TRAPPED_CHEST:
            case ENDER_CHEST:
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
            case BREWING_STAND:
            case BARREL:
                // Workstations
            case ENCHANTING_TABLE:
            case ANVIL:
            case CHIPPED_ANVIL:
            case DAMAGED_ANVIL:
            case LOOM:
            case CARTOGRAPHY_TABLE:
            case GRINDSTONE:
            case STONECUTTER:
            case SMITHING_TABLE:
            case LECTERN:
                // Miscellaneous
            case NOTE_BLOCK:
            case JUKEBOX:
            case CAMPFIRE:
            case SOUL_CAMPFIRE:
            case COMPOSTER:
            case BELL:
                return true;
            default:
                return false;
        }
    }
}