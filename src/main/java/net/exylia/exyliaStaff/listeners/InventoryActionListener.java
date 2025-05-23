package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles all inventory-related events for staff mode
 */
public class InventoryActionListener extends StaffModeListenerBase {

    public InventoryActionListener(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        super(plugin, staffModeManager);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (isFrozen(player)) {
            event.setCancelled(true);
            return;
        }

        if (isInStaffMode(player)) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && staffModeManager.getStaffItems().isStaffItem(clickedItem)) {
                tryExecuteStaffItemAction(player, clickedItem, null, null);
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                player.setItemOnCursor(null);
                player.updateInventory();
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (isFrozen(player)) {
            event.setCancelled(true);
            return;
        }

        if (isInStaffMode(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (isFrozen(player)) {
            event.setCancelled(true);
            return;
        }

        if (isInStaffMode(player)) {
            event.setCancelled(true);
        }
    }
}