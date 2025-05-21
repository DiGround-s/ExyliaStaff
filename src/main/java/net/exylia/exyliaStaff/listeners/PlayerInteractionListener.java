package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
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
            if (clickedBlock.getState() instanceof Container container) {
                event.setCancelled(true);

                Inventory containerInventory = container.getInventory();
                String containerType = getContainerTypeName(clickedBlock.getType());
                Inventory viewInventory = Bukkit.createInventory(null, containerInventory.getSize(), plugin.getConfigManager().getMessage("inventories.silent-container", "%type%", containerType));
                viewInventory.setContents(containerInventory.getContents());
                player.openInventory(viewInventory);
            }
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
        return switch (material) {
            case ACACIA_DOOR, BIRCH_DOOR, DARK_OAK_DOOR, JUNGLE_DOOR, OAK_DOOR, SPRUCE_DOOR, CRIMSON_DOOR, WARPED_DOOR,
                 IRON_DOOR, ACACIA_TRAPDOOR, BIRCH_TRAPDOOR, DARK_OAK_TRAPDOOR, JUNGLE_TRAPDOOR, OAK_TRAPDOOR,
                 SPRUCE_TRAPDOOR, CRIMSON_TRAPDOOR, WARPED_TRAPDOOR, ACACIA_FENCE_GATE, BIRCH_FENCE_GATE,
                 DARK_OAK_FENCE_GATE, JUNGLE_FENCE_GATE, OAK_FENCE_GATE, SPRUCE_FENCE_GATE, CRIMSON_FENCE_GATE,
                 WARPED_FENCE_GATE, ACACIA_BUTTON, BIRCH_BUTTON, DARK_OAK_BUTTON, JUNGLE_BUTTON, OAK_BUTTON,
                 SPRUCE_BUTTON, CRIMSON_BUTTON, WARPED_BUTTON, STONE_BUTTON, POLISHED_BLACKSTONE_BUTTON, LEVER,
                 REPEATER, COMPARATOR, DAYLIGHT_DETECTOR, DISPENSER, DROPPER, HOPPER, BEACON, CHEST, TRAPPED_CHEST,
                 ENDER_CHEST, FURNACE, BLAST_FURNACE, SMOKER, BREWING_STAND, BARREL, ENCHANTING_TABLE, ANVIL,
                 CHIPPED_ANVIL, DAMAGED_ANVIL, LOOM, CARTOGRAPHY_TABLE, GRINDSTONE, STONECUTTER, SMITHING_TABLE,
                 LECTERN, NOTE_BLOCK, JUKEBOX, CAMPFIRE, SOUL_CAMPFIRE, COMPOSTER, BELL, FLOWER_POT -> true;
            default -> false;
        };
    }

    private String getContainerTypeName(Material material) {
        return switch (material) {
            case CHEST -> "Cofre";
            case TRAPPED_CHEST -> "Cofre Trampa";
            case BARREL -> "Barril";
            case SHULKER_BOX, BLACK_SHULKER_BOX, BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, CYAN_SHULKER_BOX,
                 GRAY_SHULKER_BOX, GREEN_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX, LIME_SHULKER_BOX,
                 MAGENTA_SHULKER_BOX, ORANGE_SHULKER_BOX, PINK_SHULKER_BOX, PURPLE_SHULKER_BOX, RED_SHULKER_BOX,
                 WHITE_SHULKER_BOX, YELLOW_SHULKER_BOX -> "Caja de Shulker";
            case ENDER_CHEST -> "Cofre del End";
            case DISPENSER -> "Dispensador";
            case DROPPER -> "Soltador";
            case HOPPER -> "Tolva";
            case FURNACE -> "Horno";
            case BLAST_FURNACE -> "Alto Horno";
            case SMOKER -> "Ahumador";
            case BREWING_STAND -> "Soporte de Pociones";
            default -> material.name().toLowerCase().replace('_', ' ');
        };
    }
}