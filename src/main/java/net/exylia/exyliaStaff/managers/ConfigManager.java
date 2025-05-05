package net.exylia.exyliaStaff.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {
    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private String prefix = "";

    public ConfigManager(JavaPlugin plugin, List<String> files) {
        this.plugin = plugin;
        for (String file : files) {
            loadConfig(file);
        }
        if (configs.containsKey("messages")) {
            this.prefix = getConfig("messages").getString("prefix", "");
        }
    }

    private void loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName + ".yml");
        if (!file.exists()) {
            plugin.saveResource(fileName + ".yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(fileName, config);
    }

    public String getMessage(String path) {
        String message = getConfig("messages").getString(path, "<#a33b53>" + path + " not found in messages.yml");
        return applyPlaceholders(message);
    }

    private String applyPlaceholders(String message) {
        return message.replace("%prefix%", prefix);
    }

    public FileConfiguration getConfig(String fileName) {
        return configs.get(fileName);
    }

    public void reloadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        configs.put(fileName, config);
    }

    public void reloadAllConfigs() {
        for (String fileName : configs.keySet()) {
            reloadConfig(fileName);
        }
        if (configs.containsKey("messages")) {
            this.prefix = getConfig("messages").getString("prefix", "");
        }
    }
}
