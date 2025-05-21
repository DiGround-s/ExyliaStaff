package net.exylia.exyliaStaff;

import net.exylia.commons.ExyliaCommons;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.command.CommandManager;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.menu.MenuManager;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.exyliaStaff.commands.NotificationsCommand;
import net.exylia.exyliaStaff.commands.StaffModeCommand;
import net.exylia.exyliaStaff.commands.VanishCommand;
import net.exylia.exyliaStaff.database.StaffDatabaseLoader;
import net.exylia.exyliaStaff.extensions.PlaceholderAPI;
import net.exylia.exyliaStaff.listeners.StaffModeListenerManager;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;

import java.util.List;

import static net.exylia.commons.utils.DebugUtils.logInfo;

public final class ExyliaStaff extends ExyliaPlugin {

    private ConfigManager configManager;
    private StaffDatabaseLoader databaseLoader;
    private StaffModeManager staffModeManager;
    private CommandManager commandManager;

    @Override
    public void onExyliaEnable() {
        DebugUtils.setPrefix(getName());
        DebugUtils.sendPluginMOTD(getName());

        loadManagers();
        loadCommands();
        loadListeners();

        loadExtensions();

        logInfo("Plugin habilitado correctamente");
        logInfo("Usando ExyliaCommons v" + ExyliaCommons.getVersion());
    }

    @Override
    public void onExyliaDisable() {
        if (staffModeManager != null) {
            staffModeManager.saveAllPlayers();
            staffModeManager.disableAllStaffMode(false);
        }

        if (databaseLoader != null) {
            databaseLoader.close();
            logInfo("Conexiones de base de datos cerradas correctamente");
        }

        logInfo("¡Plugin deshabilitado correctamente!");
    }

    private void loadListeners() {
        new StaffModeListenerManager(this, staffModeManager);
    }

    private void loadManagers() {
        MenuManager.initialize(this);
        configManager = new ConfigManager(this, List.of("config", "messages", "punishments", "menus/inspect", "menus/miner_hub", "menus/punishments"));

        databaseLoader = new StaffDatabaseLoader(this);
        databaseLoader.load();

        if (isDebugMode()) {
            DebugUtils.logDebug(true, "Base de datos inicializada usando: " + databaseLoader.getDatabaseType());
        }

        staffModeManager = new StaffModeManager(this);
        this.commandManager = new CommandManager(this);
    }

    private void loadExtensions() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            logInfo("PlaceholderAPI encontrado, registrando expansión...");
            new PlaceholderAPI(this).register();
        }
    }

    private void loadCommands() {
        List<String> staffAliases = configManager.getConfig("config").getStringList("aliases.staff_mode");
        List<String> vanishAliases = configManager.getConfig("config").getStringList("aliases.vanish");
        List<String> notificationsAliases = configManager.getConfig("config").getStringList("aliases.notifications");

        StaffModeCommand staffModeCommand = new StaffModeCommand(this, staffModeManager, staffAliases);
        VanishCommand vanishCommand = new VanishCommand(this, staffModeManager, vanishAliases);
        NotificationsCommand notificationsCommand = new NotificationsCommand(this, staffModeManager, notificationsAliases);

        commandManager.registerCommands(
                staffModeCommand,
                vanishCommand,
                notificationsCommand
        );

        logInfo("Se han cargado " + commandManager.getCommands().size() + " comandos correctamente.");
    }

    public boolean isDebugMode() {
        return getConfigManager().getConfig("config").getBoolean("debug");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StaffDatabaseLoader getDatabaseLoader() {
        return databaseLoader;
    }

    public StaffModeManager getStaffModeManager() {
        return staffModeManager;
    }
}
