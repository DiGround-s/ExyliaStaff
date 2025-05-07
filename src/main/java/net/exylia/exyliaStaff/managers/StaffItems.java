package net.exylia.exyliaStaff.managers;

import net.exylia.exyliaStaff.ExyliaStaff;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class StaffItems {
    private final ExyliaStaff plugin;
    private final NamespacedKey STAFF_ITEM_KEY;
    private final Map<String, ItemStack> staffItems;

    public StaffItems(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.STAFF_ITEM_KEY = new NamespacedKey(plugin, "staff_item");
        this.staffItems = new HashMap<>();
        loadItems();
    }

    private void loadItems() {
        ConfigurationSection itemsSection = plugin.getConfigManager().getConfig("config").getConfigurationSection("staff-items");
        if (itemsSection == null) {
            plugin.getLogger().warning("No se encontró la sección 'staff-items' en config.yml");
            createDefaultItems();
            return;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            String materialName = itemSection.getString("material", "COMPASS");
            Material material = Material.getMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("Material inválido: " + materialName + " para el ítem: " + key);
                material = Material.COMPASS;
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            if (meta != null) {
                String displayName = itemSection.getString("display-name", "&cStaff Item");
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));

                List<String> lore = itemSection.getStringList("lore");
                if (!lore.isEmpty()) {
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                    }
                    meta.setLore(coloredLore);
                }

                if (itemSection.getBoolean("glow", false)) {
                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                meta.getPersistentDataContainer().set(STAFF_ITEM_KEY, PersistentDataType.STRING, key);

                item.setItemMeta(meta);
            }

            staffItems.put(key, item);
        }

        if (staffItems.isEmpty()) {
            createDefaultItems();
        }
    }

    private void createDefaultItems() {
        // Teleport Tool
        ItemStack teleportTool = new ItemStack(Material.COMPASS);
        ItemMeta teleportMeta = teleportTool.getItemMeta();
        if (teleportMeta != null) {
            teleportMeta.setDisplayName(ChatColor.GOLD + "Teleport Tool");
            teleportMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Right-click to teleport to the target block",
                    ChatColor.GRAY + "Left-click on a player to teleport to them"
            ));
            teleportMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            teleportMeta.getPersistentDataContainer().set(STAFF_ITEM_KEY, PersistentDataType.STRING, "teleport");
            teleportTool.setItemMeta(teleportMeta);
        }
        staffItems.put("teleport", teleportTool);

        // Vanish Toggle
        ItemStack vanishTool = new ItemStack(Material.ENDER_EYE);
        ItemMeta vanishMeta = vanishTool.getItemMeta();
        if (vanishMeta != null) {
            vanishMeta.setDisplayName(ChatColor.AQUA + "Vanish Toggle");
            vanishMeta.setLore(Collections.singletonList(
                    ChatColor.GRAY + "Right-click to toggle vanish mode"
            ));
            vanishMeta.addEnchant(Enchantment.DURABILITY, 1, true);
            vanishMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            vanishMeta.getPersistentDataContainer().set(STAFF_ITEM_KEY, PersistentDataType.STRING, "vanish");
            vanishTool.setItemMeta(vanishMeta);
        }
        staffItems.put("vanish", vanishTool);

        // Freeze Tool
        ItemStack freezeTool = new ItemStack(Material.PACKED_ICE);
        ItemMeta freezeMeta = freezeTool.getItemMeta();
        if (freezeMeta != null) {
            freezeMeta.setDisplayName(ChatColor.BLUE + "Freeze Player");
            freezeMeta.setLore(Collections.singletonList(
                    ChatColor.GRAY + "Right-click on a player to freeze/unfreeze them"
            ));
            freezeMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            freezeMeta.getPersistentDataContainer().set(STAFF_ITEM_KEY, PersistentDataType.STRING, "freeze");
            freezeTool.setItemMeta(freezeMeta);
        }
        staffItems.put("freeze", freezeTool);

        // Inspection Tool
        ItemStack inspectTool = new ItemStack(Material.BOOK);
        ItemMeta inspectMeta = inspectTool.getItemMeta();
        if (inspectMeta != null) {
            inspectMeta.setDisplayName(ChatColor.GREEN + "Player Inspector");
            inspectMeta.setLore(Collections.singletonList(
                    ChatColor.GRAY + "Right-click on a player to inspect their inventory"
            ));
            inspectMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            inspectMeta.getPersistentDataContainer().set(STAFF_ITEM_KEY, PersistentDataType.STRING, "inspect");
            inspectTool.setItemMeta(inspectMeta);
        }
        staffItems.put("inspect", inspectTool);

        // Exit Staff Mode
        ItemStack exitTool = new ItemStack(Material.BARRIER);
        ItemMeta exitMeta = exitTool.getItemMeta();
        if (exitMeta != null) {
            exitMeta.setDisplayName(ChatColor.RED + "Exit Staff Mode");
            exitMeta.setLore(Collections.singletonList(
                    ChatColor.GRAY + "Right-click to exit staff mode"
            ));
            exitMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            exitMeta.getPersistentDataContainer().set(STAFF_ITEM_KEY, PersistentDataType.STRING, "exit");
            exitTool.setItemMeta(exitMeta);
        }
        staffItems.put("exit", exitTool);
    }

    public ItemStack getItem(String key) {
        return staffItems.getOrDefault(key, null);
    }

    public List<ItemStack> getAllItems() {
        return new ArrayList<>(staffItems.values());
    }

    public boolean isStaffItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(STAFF_ITEM_KEY, PersistentDataType.STRING);
    }

    public String getStaffItemKey(ItemStack item) {
        if (!isStaffItem(item)) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(STAFF_ITEM_KEY, PersistentDataType.STRING);
    }
}