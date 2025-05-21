package net.exylia.exyliaStaff.managers;

import net.exylia.commons.utils.ColorUtils;
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

import static net.exylia.commons.utils.DebugUtils.logWarn;

public class StaffItems {
    private final ExyliaStaff plugin;
    private final NamespacedKey STAFF_ITEM_KEY;
    private final NamespacedKey STAFF_ITEM_ACTION;
    private final NamespacedKey STAFF_ITEM_STATE;
    private final Map<String, ItemStack> staffItems;
    private final Map<String, ItemStack> alternateStateItems; // For items with alternate states like vanish/un-vanish
    private final Map<String, String> itemActions;
    private final Map<String, List<String>> itemCommands;
    private final Map<String, Integer> itemSlots;

    public StaffItems(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.STAFF_ITEM_KEY = new NamespacedKey(plugin, "staff_item");
        this.STAFF_ITEM_ACTION = new NamespacedKey(plugin, "staff_action");
        this.STAFF_ITEM_STATE = new NamespacedKey(plugin, "staff_state");
        this.staffItems = new HashMap<>();
        this.alternateStateItems = new HashMap<>();
        this.itemActions = new HashMap<>();
        this.itemCommands = new HashMap<>();
        this.itemSlots = new HashMap<>();
        loadItems();
    }

    private void loadItems() {
        ConfigurationSection itemsSection = plugin.getConfigManager().getConfig("config").getConfigurationSection("staff-items");
        if (itemsSection == null) {
            logWarn("No se encontró la sección 'staff-items' en config.yml");
            return;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            // Load the main item
            loadItem(key, itemSection, false);

            // Check for alternate state item (like un-vanish)
            for (String subKey : itemSection.getKeys(false)) {
                if (itemSection.isConfigurationSection(subKey)) {
                    // This is a subsection, like "un-vanish"
                    loadAlternateStateItem(key, subKey, itemSection.getConfigurationSection(subKey));
                }
            }
        }
    }

    private void loadItem(String key, ConfigurationSection itemSection, boolean isAlternateState) {
        String materialName = itemSection.getString("material", "COMPASS");
        Material material = Material.getMaterial(materialName);
        if (material == null) {
            logWarn("Material inválido: " + materialName + " para el ítem: " + key);
            material = Material.COMPASS;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String displayName = itemSection.getString("display-name", "&cStaff Item");
            meta.displayName(ColorUtils.parse(displayName));

            List<String> lore = itemSection.getStringList("lore");
            if (!lore.isEmpty()) {
                meta.lore(ColorUtils.parse(lore));
            }

            if (itemSection.getBoolean("glow", false)) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(STAFF_ITEM_KEY, PersistentDataType.STRING, key);

            String action = itemSection.getString("action", key);
            meta.getPersistentDataContainer().set(STAFF_ITEM_ACTION, PersistentDataType.STRING, action);

            if (isAlternateState) {
                meta.getPersistentDataContainer().set(STAFF_ITEM_STATE, PersistentDataType.STRING, "alternate");
            }

            itemActions.put(key, action);

            if (itemSection.contains("commands")) {
                List<String> commands = itemSection.getStringList("commands");
                itemCommands.put(key, commands);
            }

            if (itemSection.contains("slot")) {
                int slot = itemSection.getInt("slot", -1);
                itemSlots.put(key, slot);
            }

            item.setItemMeta(meta);
        }

        if (isAlternateState) {
            alternateStateItems.put(key, item);
        } else {
            staffItems.put(key, item);
        }
    }

    private void loadAlternateStateItem(String parentKey, String stateKey, ConfigurationSection stateSection) {
        loadItem(parentKey, stateSection, true);
    }

    private ItemStack createDefaultItem(Material material, String displayName, List<String> lore, boolean glow, String action, int slot) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(ColorUtils.parse(displayName));

            meta.lore(ColorUtils.parse(lore));

            if (glow) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(STAFF_ITEM_KEY, PersistentDataType.STRING, action);
            meta.getPersistentDataContainer().set(STAFF_ITEM_ACTION, PersistentDataType.STRING, action);
            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack getItem(String key) {
        return staffItems.getOrDefault(key, null);
    }

    public ItemStack getAlternateStateItem(String key) {
        return alternateStateItems.getOrDefault(key, null);
    }

    public boolean hasAlternateState(String key) {
        return alternateStateItems.containsKey(key);
    }

    public List<ItemStack> getAllItems() {
        return new ArrayList<>(staffItems.values());
    }

    public Map<String, Integer> getItemSlots() {
        return itemSlots;
    }

    public int getSlot(String key) {
        return itemSlots.getOrDefault(key, -1);
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

    public String getItemAction(ItemStack item) {
        if (!isStaffItem(item)) return null;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(STAFF_ITEM_ACTION, PersistentDataType.STRING);
    }

    public boolean isAlternateState(ItemStack item) {
        if (!isStaffItem(item)) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        return meta.getPersistentDataContainer().has(STAFF_ITEM_STATE, PersistentDataType.STRING);
    }

    public String getItemAction(String key) {
        return itemActions.getOrDefault(key, key);
    }

    public List<String> getItemCommands(String key) {
        return itemCommands.getOrDefault(key, Collections.emptyList());
    }

    public boolean hasCommands(String key) {
        return itemCommands.containsKey(key) && !itemCommands.get(key).isEmpty();
    }
}