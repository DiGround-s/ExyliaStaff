package net.exylia.exyliaStaff;

import net.exylia.commons.ExyliaCommons;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.menu.MenuManager;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.exyliaStaff.commands.StaffModeCommand;
import net.exylia.exyliaStaff.commands.VanishCommand;
import net.exylia.exyliaStaff.database.StaffDatabaseLoader;
import net.exylia.exyliaStaff.extensions.PlaceholderAPI;
import net.exylia.exyliaStaff.listeners.StaffModeListener;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import static net.exylia.commons.utils.DebugUtils.logDebug;

public final class ExyliaStaff extends JavaPlugin {

    private ConfigManager configManager;
    private StaffDatabaseLoader databaseLoader;
    private StaffModeManager staffModeManager;

    @Override
    public void onEnable() {
        DebugUtils.setPrefix(getName());
        DebugUtils.sendPluginMOTD(getName());

        loadManagers();
        loadCommands();
        loadListeners();

        loadExtensions();

        DebugUtils.logInfo("Plugin habilitado correctamente");
        DebugUtils.logInfo("Usando ExyliaCommons v" + ExyliaCommons.getVersion());
    }

    @Override
    public void onDisable() {
        if (staffModeManager != null) {
            staffModeManager.saveAllPlayers();
            staffModeManager.disableAllStaffMode(false);
        }

        if (databaseLoader != null) {
            databaseLoader.close();
            DebugUtils.logInfo("Conexiones de base de datos cerradas correctamente");
        }

        DebugUtils.logInfo("¡Plugin deshabilitado correctamente!");
    }

    private void loadListeners() {
        getServer().getPluginManager().registerEvents(new StaffModeListener(this, staffModeManager), this);
    }

    private void loadManagers() {
        MenuManager.initialize(this);
        configManager = new ConfigManager(this, List.of("config", "messages", "menus/inspect", "menus/miner_hub"));

        databaseLoader = new StaffDatabaseLoader(this);
        databaseLoader.load();

        if (isDebugMode()) {
            DebugUtils.logDebug(true, "Base de datos inicializada usando: " + databaseLoader.getDatabaseType());
        }

        staffModeManager = new StaffModeManager(this);
    }

    private void loadCommands() {
        StaffModeCommand staffModeCommand = new StaffModeCommand(this, staffModeManager);
        List<String> staffAliases = getConfigManager().getConfig("config").getStringList("aliases.staff_mode");
        registerCommand("staffmode", staffAliases, staffModeCommand, staffModeCommand);

        VanishCommand vanishCommand = new VanishCommand(this, staffModeManager);
        List<String> vanishAliases = getConfigManager().getConfig("config").getStringList("aliases.vanish");
        registerCommand("vanish", vanishAliases, vanishCommand, vanishCommand);
    }

    private void loadExtensions() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("PlaceholderAPI encontrado, registrando expansión...");
            new PlaceholderAPI(this).register();
        }
    }

    /**
     * Registra un comando con sus alias en el servidor
     *
     * @param name El nombre principal del comando
     * @param aliases La lista de alias para el comando
     * @param executor El ejecutor del comando
     * @param tabCompleter El completador de tabulación (puede ser null)
     */
    public void registerCommand(String name, List<String> aliases, CommandExecutor executor, TabCompleter tabCompleter) {
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            constructor.setAccessible(true);
            PluginCommand command = constructor.newInstance(name, this);

            command.setExecutor(executor);
            if (tabCompleter != null) {
                command.setTabCompleter(tabCompleter);
            }

            if (aliases != null && !aliases.isEmpty()) {
                command.setAliases(aliases);
                logDebug(isDebugMode(), "Se cargaron " + aliases.size() + " alias para el comando /" + name + " -> " + aliases);
            }

            Bukkit.getCommandMap().register(getName().toLowerCase(), command);

        } catch (NoSuchMethodException | SecurityException | InstantiationException |
                 IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            getLogger().severe("Error al registrar el comando " + name + ": " + e.getMessage());
            e.printStackTrace();
        }
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
