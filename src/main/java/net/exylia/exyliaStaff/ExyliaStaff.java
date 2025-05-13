package net.exylia.exyliaStaff;

import net.exylia.commons.ExyliaCommons;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.menu.MenuManager;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.exyliaStaff.commands.StaffModeCommand;
import net.exylia.exyliaStaff.commands.VanishCommand;
import net.exylia.exyliaStaff.database.DatabaseLoader;
import net.exylia.exyliaStaff.listeners.StaffModeListener;
import net.exylia.exyliaStaff.managers.SilentChestManager;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class ExyliaStaff extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseLoader databaseLoader;
    private StaffModeManager staffModeManager;
    private boolean debugEnabled;

    @Override
    public void onEnable() {
        DebugUtils.setPrefix(getName());
        DebugUtils.sendPluginMOTD(getName());

        loadManagers();
        loadListeners();
        loadCommands();

        // Mensaje de habilitación
        DebugUtils.logInfo("Plugin habilitado correctamente");
        DebugUtils.logInfo("Usando ExyliaCommons v" + ExyliaCommons.getVersion());
    }

    @Override
    public void onDisable() {
        // Guarda el estado de todos los jugadores
        if (staffModeManager != null) {
            staffModeManager.saveAllPlayers();
            staffModeManager.disableAllStaffMode();
        }

        // Cierra la conexión a la base de datos
        if (databaseLoader.getDatabaseManager() != null) {
            databaseLoader.getDatabaseManager().close();
        }

        getLogger().info("¡Plugin deshabilitado correctamente!");
    }

    private void loadListeners() {
        // Registramos los listeners
        getServer().getPluginManager().registerEvents(new StaffModeListener(this, staffModeManager), this);
        getServer().getPluginManager().registerEvents(new SilentChestManager(this, staffModeManager), this);
    }

    private void loadManagers() {
        MenuManager.initialize(this);
        // Cargamos la configuración
        configManager = new ConfigManager(this, List.of("config", "messages", "menus/inspect"));

        // Cargamos la base de datos
        databaseLoader = new DatabaseLoader(this);
        databaseLoader.load();

        // Cargamos el manager de modo staff
        staffModeManager = new StaffModeManager(this);

        debugEnabled = configManager.getConfig("config").getBoolean("debug", false);
    }

    private void loadCommands() {
        // Comando de modo staff
        PluginCommand staffModeCmd = getCommand("staffmode");
        if (staffModeCmd != null) {
            StaffModeCommand staffModeCommand = new StaffModeCommand(this, staffModeManager);
            staffModeCmd.setExecutor(staffModeCommand);
            staffModeCmd.setTabCompleter(staffModeCommand);
        }

        // Comando de vanish
        PluginCommand vanishCmd = getCommand("vanish");
        if (vanishCmd != null) {
            VanishCommand vanishCommand = new VanishCommand(this, staffModeManager);
            vanishCmd.setExecutor(vanishCommand);
            vanishCmd.setTabCompleter(vanishCommand);
        }
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

    public StaffModeManager getStaffModeManager() {
        return staffModeManager;
    }
}