package net.exylia.exyliaStaff.listeners;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffManager;
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
    private final StaffManager staffManager;
    private final List<Listener> listeners = new ArrayList<>();

    public StaffModeListenerManager(ExyliaStaff plugin, StaffManager staffManager) {
        this.plugin = plugin;
        this.staffManager = staffManager;
        registerListeners();
    }

    /**
     * Register all staff mode related listeners
     */
    private void registerListeners() {
        listeners.add(new PlayerSessionListener(plugin, staffManager));
        listeners.add(new PlayerInteractionListener(plugin, staffManager));
        listeners.add(new InventoryActionListener(plugin, staffManager));
        listeners.add(new BlockInteractionListener(plugin, staffManager));
        listeners.add(new EntityInteractionListener(plugin, staffManager));
        listeners.add(new FrozenPlayerListener(plugin, staffManager));

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