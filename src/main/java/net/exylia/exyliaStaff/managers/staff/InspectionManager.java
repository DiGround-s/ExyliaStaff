package net.exylia.exyliaStaff.managers.staff;

import net.exylia.commons.menu.CustomPlaceholderManager;
import net.exylia.commons.menu.Menu;
import net.exylia.commons.menu.MenuBuilder;
import net.exylia.commons.menu.MenuItem;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.managers.StaffModeManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static net.exylia.commons.utils.ColorUtils.sendPlayerMessage;
import static net.exylia.commons.utils.DebugUtils.logWarn;

/**
 * Handles player inspection functionality for staff members
 */
public class InspectionManager {
    private final ExyliaStaff plugin;
    private final StaffModeManager staffModeManager;

    public InspectionManager(ExyliaStaff plugin, StaffModeManager staffModeManager) {
        this.plugin = plugin;
        this.staffModeManager = staffModeManager;
    }

    public void openInspectInventory(Player staffPlayer, Player targetPlayer) {
        CustomPlaceholderManager.register("target", context -> {
            if (context instanceof Player) {
                return ((Player) context).getName();
            }
            return "";
        });

        sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("actions.inspect.opened", "%target%", targetPlayer.getName()));

        FileConfiguration config = plugin.getConfigManager().getConfig("menus/inspect");
        MenuBuilder menuBuilder = new MenuBuilder(plugin);
        Menu inspectMenu = menuBuilder.buildMenu(config, targetPlayer, targetPlayer);

        inspectMenu.usePlaceholdersInTitle(true)
                .setTitlePlaceholderContext(targetPlayer);

        // default items
        setupArmorItems(inspectMenu, config, targetPlayer);
        setupHandItems(inspectMenu, config, targetPlayer);
        setupInventoryContents(inspectMenu, config, targetPlayer);
        setupEnderChestButton(inspectMenu, config, staffPlayer, targetPlayer);

        // Open the menu
        inspectMenu.open(staffPlayer);
    }

    private void setupArmorItems(Menu menu, FileConfiguration config, Player targetPlayer) {
        // Helmet
        ItemStack helmetFallback = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta helmetMeta = helmetFallback.getItemMeta();
        helmetMeta.displayName(plugin.getConfigManager().getMessage("extras.no-helmet"));
        helmetFallback.setItemMeta(helmetMeta);
        setArmorItem(menu, config, "helmet", targetPlayer.getInventory().getHelmet(), helmetFallback);

        // Chestplate
        ItemStack chestplateFallback = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta chestMeta = chestplateFallback.getItemMeta();
        chestMeta.displayName(plugin.getConfigManager().getMessage("extras.no-chestplate"));
        chestplateFallback.setItemMeta(chestMeta);
        setArmorItem(menu, config, "chest_plate", targetPlayer.getInventory().getChestplate(), chestplateFallback);

        // Leggings
        ItemStack leggingsFallback = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta leggingsMeta = leggingsFallback.getItemMeta();
        leggingsMeta.displayName(plugin.getConfigManager().getMessage("extras.no-leggings"));
        leggingsFallback.setItemMeta(leggingsMeta);
        setArmorItem(menu, config, "leggings", targetPlayer.getInventory().getLeggings(), leggingsFallback);

        // Boots
        ItemStack bootsFallback = new ItemStack(Material.STRUCTURE_VOID);
        ItemMeta bootsMeta = bootsFallback.getItemMeta();
        bootsMeta.displayName(plugin.getConfigManager().getMessage("extras.no-boots"));
        bootsFallback.setItemMeta(bootsMeta);
        setArmorItem(menu, config, "boots", targetPlayer.getInventory().getBoots(), bootsFallback);
    }

    private void setupHandItems(Menu menu, FileConfiguration config, Player targetPlayer) {
        // Main hand
        ItemStack mainHand = targetPlayer.getInventory().getItemInMainHand();
        if (mainHand.getType() == Material.AIR) {
            mainHand = new ItemStack(Material.STRUCTURE_VOID);
            ItemMeta meta = mainHand.getItemMeta();
            meta.displayName(plugin.getConfigManager().getMessage("extras.no-hand"));
            mainHand.setItemMeta(meta);
        }
        menu.setItem(config.getInt("slots.hand", 7), new MenuItem(mainHand));

        // Off hand
        ItemStack offHand = targetPlayer.getInventory().getItemInOffHand();
        if (offHand.getType() == Material.AIR) {
            offHand = new ItemStack(Material.STRUCTURE_VOID);
            ItemMeta meta = offHand.getItemMeta();
            meta.displayName(plugin.getConfigManager().getMessage("extras.no-offhand"));
            offHand.setItemMeta(meta);
        }
        menu.setItem(config.getInt("slots.offhand", 8), new MenuItem(offHand));
    }

    private void setupInventoryContents(Menu menu, FileConfiguration config, Player targetPlayer) {
        // Set hotbar items (slots 0-8)
        setInventoryRange(menu, config, "hot_bar", targetPlayer.getInventory().getContents(), 0);

        // Set main inventory items (slots 9-35)
        setInventoryRange(menu, config, "inventory", targetPlayer.getInventory().getContents(), 9);
    }

    private void setupEnderChestButton(Menu menu, FileConfiguration config, Player staffPlayer, Player targetPlayer) {
        // Create enderchest button with placeholders
        ItemStack enderChestItem = new ItemStack(Material.valueOf(config.getString("enderchest.material", "ENDER_CHEST")));
        MenuItem enderChestMenuItem = new MenuItem(enderChestItem)
                .setName(config.getString("enderchest.name", "Ver enderchest"))
                .setLoreFromList(config.getStringList("enderchest.lore"))
                .setGlowing(config.getBoolean("enderchest.glow", false))
                .setClickHandler(e -> staffPlayer.openInventory(targetPlayer.getEnderChest()))
                .usePlaceholders(true)
                .setPlaceholderContext(targetPlayer)
                .setPlaceholderPlayer(targetPlayer);

        menu.setItem(config.getInt("enderchest.slot", 5), enderChestMenuItem);
    }

    private void setArmorItem(Menu menu, FileConfiguration config, String type, ItemStack item, ItemStack fallback) {
        ItemStack displayItem = (item != null && item.getType() != Material.AIR) ? item.clone() : fallback;
        int slot = config.getInt("slots." + type, 0);
        menu.setItem(slot, new MenuItem(displayItem));
    }

    private void setInventoryRange(Menu menu, FileConfiguration config, String configPath, ItemStack[] contents, int inventoryStartIndex) {
        String rangeStr = config.getString("slots." + configPath);
        if (rangeStr == null || !rangeStr.contains("-")) return;

        String[] parts = rangeStr.split("-");
        try {
            int menuStart = Integer.parseInt(parts[0]);
            int menuEnd = Integer.parseInt(parts[1]);

            for (int i = menuStart, invIndex = inventoryStartIndex; i <= menuEnd && invIndex < contents.length; i++, invIndex++) {
                ItemStack item = contents[invIndex];
                if (item == null || item.getType() == Material.AIR) continue;
                menu.setItem(i, new MenuItem(item.clone()));
            }
        } catch (NumberFormatException e) {
            logWarn("Error al parsear rango de slots para " + configPath + ": " + rangeStr);
        }
    }

    public void openOnlinePlayersMenu(Player staffPlayer) {
        staffPlayer.sendMessage(plugin.getConfigManager().getMessage("online-players.opened"));

        StringBuilder playerList = new StringBuilder();
        int count = 0;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(staffPlayer)) {
                count++;
                playerList.append("\n").append(count).append(". ").append(online.getName());

                if (staffModeManager.isInStaffMode(online)) {
                    playerList.append(" (Staff)");
                }
                if (staffModeManager.isVanished(online)) {
                    playerList.append(" (Vanished)");
                }
                if (staffModeManager.getFreezeManager().isFrozen(online)) {
                    playerList.append(" (Frozen)");
                }
            }
        }

        if (count > 0) {
            sendPlayerMessage(staffPlayer, plugin.getConfigManager().getMessage("online-players.list",
                    "%count%", String.valueOf(count),
                    "%players%", playerList.toString()));
        } else {
            staffPlayer.sendMessage(plugin.getConfigManager().getMessage("system.no-players"));
        }
    }
}