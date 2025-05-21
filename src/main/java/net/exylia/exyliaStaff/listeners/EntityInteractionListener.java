package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * Handles all entity-related events for staff mode
 */
public class EntityInteractionListener extends StaffModeListenerBase {

    public EntityInteractionListener(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        super(plugin, staffModeManager);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (isInStaffMode(player) || isFrozen(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;

        if (isFrozen(damager)) {
            event.setCancelled(true);
            return;
        }

        if (isInStaffMode(damager) &&
                !plugin.getConfigManager().getConfig("config").getBoolean("staff-mode.can-damage-entities", false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityTarget(EntityTargetEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;

        if (isInStaffMode(player) || isFrozen(player)) {
            event.setCancelled(true);
        }
    }
}