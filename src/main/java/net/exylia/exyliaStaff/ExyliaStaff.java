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
    private DatabaseLoader databaseLoader;
    private StaffModeManager staffModeManager;

    @Override
    public void onEnable() {
        DebugUtils.setPrefix(getName());
        DebugUtils.sendPluginMOTD(getName());

        loadManagers();
        loadCommands();
        loadListeners();

        // Mensaje de habilitación
        DebugUtils.logInfo("Plugin habilitado correctamente");
        DebugUtils.logInfo("Usando ExyliaCommons v" + ExyliaCommons.getVersion());
    }

    @Override
    public void onDisable() {
        // Guarda el estado de todos los jugadores
        if (staffModeManager != null) {
            staffModeManager.saveAllPlayers();
            staffModeManager.disableAllStaffMode(false);
        }

        // Cierra la conexión a la base de datos
        if (databaseLoader.getDatabaseManager() != null) {
            databaseLoader.getDatabaseManager().close();
        }

        DebugUtils.logInfo("¡Plugin deshabilitado correctamente!");
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
    }

    private void loadCommands() {
        // Comando StaffMode
        StaffModeCommand staffModeCommand = new StaffModeCommand(this, staffModeManager);
        List<String> staffAliases = getConfigManager().getConfig("config").getStringList("aliases.staff_mode");
        registerCommand("staffmode", staffAliases, staffModeCommand, staffModeCommand);

        // Comando Vanish
        VanishCommand vanishCommand = new VanishCommand(this, staffModeManager);
        List<String> vanishAliases = getConfigManager().getConfig("config").getStringList("aliases.vanish");
        registerCommand("vanish", vanishAliases, vanishCommand, vanishCommand);
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
            // Crear una instancia de PluginCommand usando reflexión
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            constructor.setAccessible(true);
            PluginCommand command = constructor.newInstance(name, this);

            // Configurar el comando
            command.setExecutor(executor);
            if (tabCompleter != null) {
                command.setTabCompleter(tabCompleter);
            }

            // Si hay alias, los agregamos
            if (aliases != null && !aliases.isEmpty()) {
                command.setAliases(aliases);
                logDebug(isDebugMode(), "Se cargaron " + aliases.size() + " alias para el comando /" + name + " -> " + aliases);
            }

            // Registrar el comando en el CommandMap de Bukkit
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

    public DatabaseLoader getDatabaseLoader() {
        return databaseLoader;
    }

    public StaffModeManager getStaffModeManager() {
        return staffModeManager;
    }
}