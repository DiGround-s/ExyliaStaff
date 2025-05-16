package net.exylia.exyliaStaff.managers.staff;

import net.exylia.commons.utils.SoundUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockBreakNotifier {

    private final ExyliaStaff plugin;
    private final Map<Material, String> watchedBlocks = new HashMap<>();
    private boolean enabled = false;
    private String sound = null;

    public BlockBreakNotifier(ExyliaStaff plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        watchedBlocks.clear();

        ConfigurationSection section = plugin.getConfigManager().getConfig("config").getConfigurationSection("staff-notifications.block-break");
        if (section == null) return;

        enabled = section.getBoolean("enabled", false);
        sound = section.getString("sound", null);

        List<String> blocks = section.getStringList("blocks");
        for (String entry : blocks) {
            String[] parts = entry.split("\\|", 2);
            if (parts.length != 2) continue;

            try {
                Material material = Material.valueOf(parts[0]);
                watchedBlocks.put(material, parts[1]);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in block-break notifications: " + parts[0]);
            }
        }
    }

    public void notifyStaff(Player player, Block block) {
        if (!enabled || !watchedBlocks.containsKey(block.getType())) return;

        String color = watchedBlocks.get(block.getType());
        String blockName = formatBlockName(block.getType().name());
        Component message = plugin.getConfigManager().getMessage("notifications.block-break", "%player%", player.getName(), "%block%", blockName, "%color%", color, "%world%", player.getWorld().getName(), "%x%", String.valueOf(block.getX()), "%y%", String.valueOf(block.getY()), "%z%", String.valueOf(block.getZ()));

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.hasPermission("exyliastaff.notifications.block-break")) {
                staff.sendMessage(message);
                if (sound != null && !sound.isEmpty()) {
                    SoundUtils.playSound(staff, sound);
                }
            }
        }
    }

    private String formatBlockName(String materialName) {
        return materialName.replace('_', ' ').toLowerCase();
    }

    public boolean isWatchedBlock(Material material) {
        return enabled && watchedBlocks.containsKey(material);
    }
}