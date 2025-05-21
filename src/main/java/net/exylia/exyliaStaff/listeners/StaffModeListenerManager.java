package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages all staff mode related listeners
 */
public class StaffModeListenerManager {

    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;
    private final List<Listener> listeners = new ArrayList<>();

    public StaffModeListenerManager(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
        registerListeners();
    }

    /**
     * Register all staff mode related listeners
     */
    private void registerListeners() {
        listeners.add(new PlayerSessionListener(plugin, staffModeManager));
        listeners.add(new PlayerInteractionListener(plugin, staffModeManager));
        listeners.add(new InventoryActionListener(plugin, staffModeManager));
        listeners.add(new BlockInteractionListener(plugin, staffModeManager));
        listeners.add(new EntityInteractionListener(plugin, staffModeManager));
        listeners.add(new FrozenPlayerListener(plugin, staffModeManager));

        for (Listener listener : listeners) {
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        }
    }

    /**
     * Unregister all listeners
     */
    public void unregisterListeners() {
        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }
        listeners.clear();
    }
}