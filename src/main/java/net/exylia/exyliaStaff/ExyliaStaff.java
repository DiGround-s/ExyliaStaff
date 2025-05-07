package net.exylia.exyliaStaff;

import net.exylia.exyliaStaff.commands.StaffModeCommand;
import net.exylia.exyliaStaff.commands.VanishCommand;
import net.exylia.exyliaStaff.database.DatabaseLoader;
import net.exylia.exyliaStaff.listeners.StaffModeListener;
import net.exylia.exyliaStaff.managers.ConfigManager;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

import static net.exylia.exyliaStaff.utils.DebugUtils.sendMOTD;

public final class ExyliaStaff extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseLoader databaseLoader;
    private StaffModeManager staffModeManager;

    @Override
    public void onEnable() {
        sendMOTD();
        loadManagers();
        loadListeners();
        loadCommands();

        // Mensaje de habilitación
        getLogger().info("¡Plugin habilitado correctamente!");
    }

    @Override
    public void onDisable() {
        // Guarda el estado de todos los jugadores
        if (staffModeManager != null) {
            staffModeManager.saveAllPlayers();
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
    }

    private void loadManagers() {
        // Cargamos la configuración
        configManager = new ConfigManager(this, List.of("config", "messages"));

        // Cargamos la base de datos
        databaseLoader = new DatabaseLoader(this);
        databaseLoader.load();

        // Cargamos el manager de modo staff
        staffModeManager = new StaffModeManager(this);
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