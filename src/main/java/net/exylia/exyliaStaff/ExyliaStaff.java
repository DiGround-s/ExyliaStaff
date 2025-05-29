package net.exylia.exyliaStaff;

import net.exylia.commons.ExyliaCommons;
import net.exylia.commons.ExyliaPlugin;
import net.exylia.commons.command.CommandManager;
import net.exylia.commons.command.ExyliaCommand;
import net.exylia.commons.config.ConfigManager;
import net.exylia.commons.menu.MenuManager;
import net.exylia.commons.utils.DebugUtils;
import net.exylia.exyliaStaff.commands.*;
import net.exylia.exyliaStaff.database.StaffDatabaseLoader;
import net.exylia.exyliaStaff.extensions.PlaceholderAPI;
import net.exylia.exyliaStaff.listeners.StaffModeListenerManager;
import net.exylia.exyliaStaff.managers.StaffManager;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.ArrayList;

import static net.exylia.commons.utils.DebugUtils.logInfo;
import static net.exylia.commons.utils.DebugUtils.logWarn;

public final class ExyliaStaff extends ExyliaPlugin {

    private ConfigManager configManager;
    private StaffDatabaseLoader databaseLoader;
    private StaffManager staffManager;
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
        if (staffManager != null) {
            staffManager.saveAllPlayers();
            staffManager.disableAllStaffMode(false);
        }

        if (databaseLoader != null) {
            databaseLoader.close();
            logInfo("Conexiones de base de datos cerradas correctamente");
        }

        logInfo("¡Plugin deshabilitado correctamente!");
    }

    private void loadListeners() {
        new StaffModeListenerManager(this, staffManager);
    }

    private void loadManagers() {
        MenuManager.initialize(this);
        configManager = new ConfigManager(this, List.of(
                "config",
                "messages",
                "menus/inspect",
                "menus/miner_hub",
                "menus/punishments",
                "modules/notifications",
                "modules/vanish",
                "modules/freeze",
                "modules/scoreboard",
                "modules/staff-mode",
                "modules/punishments",
                "modules/staff-chat"));

        databaseLoader = new StaffDatabaseLoader(this);
        databaseLoader.load();

        if (isDebugMode()) {
            DebugUtils.logDebug(true, "Base de datos inicializada usando: " + databaseLoader.getDatabaseType());
        }

        staffManager = new StaffManager(this);
        this.commandManager = new CommandManager(this);
    }

    private void loadExtensions() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            logInfo("PlaceholderAPI encontrado, registrando expansión...");
            new PlaceholderAPI(this).register();
        }
    }

    private void loadCommands() {
        List<ExyliaCommand> commandsToRegister = new ArrayList<>();
        int enabledCommands = 0;
        int disabledCommands = 0;
        if (isModuleEnabled("staff-mode")) {
            List<String> staffAliases = configManager.getConfig("config").getStringList("aliases.staff_mode");
            StaffModeCommand staffModeCommand = new StaffModeCommand(this, staffManager, staffAliases);
            commandsToRegister.add(staffModeCommand);
            enabledCommands++;
            if (isDebugMode()) {
                DebugUtils.logDebug(true, "Comando 'staff-mode' cargado con aliases: " + staffAliases);
            }
        } else {
            disabledCommands++;
            if (isDebugMode()) {
                DebugUtils.logDebug(true, "Comando 'staff-mode' omitido - módulo deshabilitado");
            }
        }
        if (isModuleEnabled("vanish")) {
            List<String> vanishAliases = configManager.getConfig("config").getStringList("aliases.vanish");
            VanishCommand vanishCommand = new VanishCommand(this, staffManager, vanishAliases);
            commandsToRegister.add(vanishCommand);
            enabledCommands++;
            if (isDebugMode()) {
                DebugUtils.logDebug(true, "Comando 'vanish' cargado con aliases: " + vanishAliases);
            }
        } else {
            disabledCommands++;
            if (isDebugMode()) {
                DebugUtils.logDebug(true, "Comando 'vanish' omitido - módulo deshabilitado");
            }
        }
        if (isModuleEnabled("notifications")) {
            List<String> notificationsAliases = configManager.getConfig("config").getStringList("aliases.notifications");
            NotificationsCommand notificationsCommand = new NotificationsCommand(this, staffManager, notificationsAliases);
            commandsToRegister.add(notificationsCommand);
            enabledCommands++;
            if (isDebugMode()) {
                DebugUtils.logDebug(true, "Comando 'notifications' cargado con aliases: " + notificationsAliases);
            }
        } else {
            disabledCommands++;
            if (isDebugMode()) {
                DebugUtils.logDebug(true, "Comando 'notifications' omitido - módulo deshabilitado");
            }
        }
        if (isModuleEnabled("freeze")) {
            List<String> freezeAliases = configManager.getConfig("config").getStringList("aliases.freeze");
            FreezeCommand freezeCommand = new FreezeCommand(this, staffManager, freezeAliases);
            commandsToRegister.add(freezeCommand);
            enabledCommands++;
            if (isDebugMode()) {
                DebugUtils.logDebug(true, "Comando 'freeze' cargado con aliases: " + freezeAliases);
            }
        } else {
            disabledCommands++;
            if (isDebugMode()) {
                DebugUtils.logDebug(true, "Comando 'freeze' omitido - módulo deshabilitado");
            }
        }

        // Punish Command
        if (isModuleEnabled("punish")) {
            List<String> punishAliases = configManager.getConfig("config").getStringList("aliases.punish");
            PunishCommand punishCommand = new PunishCommand(this, staffManager, punishAliases);
            commandsToRegister.add(punishCommand);
            enabledCommands++;
            if (isDebugMode()) {
                DebugUtils.logDebug(true, "Comando 'punish' cargado con aliases: " + punishAliases);
            }
        } else {
            disabledCommands++;
            if (isDebugMode()) {
                DebugUtils.logDebug(true, "Comando 'punish' omitido - módulo deshabilitado");
            }
        }

        if (!commandsToRegister.isEmpty()) {
            commandManager.registerCommands(commandsToRegister);
        }
        logInfo("Se han cargado " + enabledCommands + " comandos correctamente.");

        if (disabledCommands > 0) {
            logWarn(disabledCommands + " comandos fueron omitidos por tener módulos deshabilitados.");
        }
        if (isDebugMode()) {
            DebugUtils.logDebug(true, "Resumen de carga de comandos:");
            DebugUtils.logDebug(true, "  - Comandos habilitados: " + enabledCommands);
            DebugUtils.logDebug(true, "  - Comandos omitidos: " + disabledCommands);
            DebugUtils.logDebug(true, "  - Total comandos registrados: " + commandManager.getCommands().size());
        }
    }

    public void logModuleStatus() {
        logInfo("Estado actual de los módulos:");
        logInfo("  - staff-mode: " + (isModuleEnabled("staff-mode") ? "✓" : "✗"));
        logInfo("  - vanish: " + (isModuleEnabled("vanish") ? "✓" : "✗"));
        logInfo("  - freeze: " + (isModuleEnabled("freeze") ? "✓" : "✗"));
        logInfo("  - notifications: " + (isModuleEnabled("notifications") ? "✓" : "✗"));
        logInfo("  - scoreboard: " + (isModuleEnabled("scoreboard") ? "✓" : "✗"));
        logInfo("  - staff-chat: " + (isModuleEnabled("staff-chat") ? "✓" : "✗"));
        logInfo("  - punish: " + (isModuleEnabled("punish") ? "✓" : "✗"));
    }

    public void reload() {
        logInfo("Recargando configuración...");
        configManager.reloadAllConfigs();
        commandManager.unregisterAll();
        loadCommands();
        if (isDebugMode()) {
            logModuleStatus();
        }

        logInfo("Configuración recargada correctamente.");
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

    public StaffManager getStaffManager() {
        return staffManager;
    }

    public boolean isModuleEnabled(String moduleName) {
        return configManager.getConfig("config").getBoolean("modules." + moduleName, true);
    }

    public boolean isEnabledExt(String name) {
        return switch (name.toLowerCase()) {
            case "placeholderapi" -> Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
            default -> false;
        };
    }
}