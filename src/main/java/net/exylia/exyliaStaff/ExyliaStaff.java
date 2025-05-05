package net.exylia.exyliaStaff;

import net.exylia.exyliaStaff.database.DatabaseLoader;
import net.exylia.exyliaStaff.managers.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static net.exylia.exyliaStaff.utils.DebugUtils.sendMOTD;

public final class ExyliaStaff extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseLoader databaseLoader;

    @Override
    public void onEnable() {
        sendMOTD();
        loadManagers();
        loadListeners();
        loadCommands();
    }

    @Override
    public void onDisable() {

    }

    private void loadListeners() {

    }

    private void loadManagers() {
        configManager = new ConfigManager(this, List.of("config", "messages"));
        databaseLoader = new DatabaseLoader(this);
        databaseLoader.load();
    }


    private void loadCommands() {

    }


    public boolean isDebugMode() {
        return getConfigManager().getConfig("config").getBoolean("debug");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
    public DatabaseLoader getDatabaseLoader() {
        return databaseLoader;
    }

}
