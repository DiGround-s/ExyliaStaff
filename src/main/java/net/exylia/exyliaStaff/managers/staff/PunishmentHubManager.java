package net.exylia.exyliaStaff.managers.staff;

import net.exylia.commons.menu.MenuBuilder;
import net.exylia.commons.menu.MenuItem;
import net.exylia.commons.menu.PaginationMenu;
import net.exylia.commons.utils.MessageUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.exylia.commons.utils.DebugUtils.logError;
import static net.exylia.commons.utils.DebugUtils.logInfo;
import static net.exylia.commons.utils.MenuUtils.parseSlots;

public class PunishmentHubManager {
    private final ExyliaStaff plugin;
    private FileConfiguration menuConfig;
    private FileConfiguration punishmentConfig;

    public PunishmentHubManager(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.menuConfig = plugin.getConfigManager().getConfig("menus/punishments");
        this.punishmentConfig = plugin.getConfigManager().getConfig("punishments");
    }

    /**
     * Recarga las configuraciones de sanciones y menús
     */
    public void reloadConfigurations() {
        try {
            this.menuConfig = plugin.getConfigManager().getConfig("menus/punishments");
            this.punishmentConfig = plugin.getConfigManager().getConfig("punishments");
            logInfo("Configuraciones de sanciones recargadas");
        } catch (Exception e) {
            logError("Error al recargar las configuraciones de sanciones: " + e.getMessage());
        }
    }

    /**
     * Abre el inventario de sanciones para un staff sobre un jugador objetivo
     *
     * @param staff El miembro del staff que aplica la sanción
     * @param target El jugador objetivo que será sancionado
     */
    public void openPunishmentInventory(Player staff, Player target) {
        if (target == null) return;
        PaginationMenu punishmentMenu = createPunishmentInventory(staff, target);

        if (punishmentMenu != null) {
            punishmentMenu.open(staff);
        } else {
            MessageUtils.sendMessageAsync(staff, plugin.getConfigManager().getMessage("actions.punishment-hub.error", "%target%", target.getName()));
        }
    }

    /**
     * Crea el inventario de sanciones
     *
     * @param staff El miembro del staff que aplica la sanción
     * @param target El jugador objetivo que será sancionado
     * @return El menú de sanciones
     */
    private PaginationMenu createPunishmentInventory(Player staff, Player target) {
        String punishmentSlotsString = menuConfig.getString("punishment_items.slots", "10-26");
        int rows = menuConfig.getInt("rows", 4);
        int[] punishmentSlots = parseSlots(punishmentSlotsString, rows);

        MenuBuilder menuBuilder = new MenuBuilder(plugin);
        PaginationMenu punishmentMenu = menuBuilder.buildPaginationMenu(menuConfig, staff, punishmentSlots);

        addPunishmentItems(punishmentMenu, staff, target);

        return punishmentMenu;
    }

    /**
     * Añade los ítems de sanciones al menú paginado
     *
     * @param paginationMenu El menú donde se añadirán las sanciones
     * @param staff El miembro del staff que aplica la sanción
     * @param target El jugador objetivo que será sancionado
     */
    private void addPunishmentItems(PaginationMenu paginationMenu, Player staff, Player target) {
        boolean usePlaceholders = menuConfig.getBoolean("punishment_items.use_placeholders", true);
        boolean hideAttributes = menuConfig.getBoolean("punishment_items.hide_attributes", true);
        String itemName = menuConfig.getString("punishment_items.name", "<#8a51c4>%punishment_name%");
        List<String> itemLore = menuConfig.getStringList("punishment_items.lore");

        ConfigurationSection punishmentsSection = punishmentConfig.getConfigurationSection("punishments");
        if (punishmentsSection == null) {
            return;
        }

        Set<String> punishmentKeys = punishmentsSection.getKeys(false);

        for (String key : punishmentKeys) {
            ConfigurationSection punishmentSection = punishmentsSection.getConfigurationSection(key);
            if (punishmentSection == null) {
                continue;
            }

            String name = punishmentSection.getString("name", "Unknown");
            String description = punishmentSection.getString("description", "");
            String materialString = punishmentSection.getString("material", "BARRIER");
            String staffCommand = punishmentSection.getString("staff_command", "");

            MenuItem punishmentItem = createPunishmentItem(
                    key,
                    name,
                    description,
                    materialString,
                    staffCommand,
                    itemName,
                    new ArrayList<>(itemLore),
                    usePlaceholders,
                    hideAttributes,
                    staff,
                    target
            );

            paginationMenu.addItem(punishmentItem);
        }
    }

    /**
     * Crea un ítem de menú para una sanción específica
     *
     * @param punishmentKey La clave de la sanción
     * @param punishmentName El nombre de la sanción
     * @param description La descripción de la sanción
     * @param materialString El material del ítem
     * @param staffCommand El comando que se ejecutará
     * @param itemName El nombre del ítem (puede contener placeholders)
     * @param itemLore El lore del ítem (puede contener placeholders)
     * @param usePlaceholders Si se deben usar placeholders
     * @param hideAttributes Si se deben ocultar los atributos del ítem
     * @param staff El miembro del staff que aplica la sanción
     * @param target El jugador objetivo que será sancionado
     * @return El ítem de menú creado
     */
    private MenuItem createPunishmentItem(String punishmentKey, String punishmentName, String description,
                                          String materialString, String staffCommand, String itemName,
                                          List<String> itemLore, boolean usePlaceholders,
                                          boolean hideAttributes, Player staff, Player target) {
        try {
            Material material = Material.valueOf(materialString);
            MenuItem punishmentItem = new MenuItem(material);

            punishmentItem.usePlaceholders(usePlaceholders);

            // Reemplazar placeholders específicos
            String finalItemName = itemName.replace("%punishment_name%", punishmentName);
            punishmentItem.setName(finalItemName);

            List<String> finalLore = new ArrayList<>();
            for (String line : itemLore) {
                finalLore.add(line
                        .replace("%description%", description)
                        .replace("%player_name%", target.getName())
                        .replace("%punishment_key%", punishmentKey)
                );
            }

            punishmentItem.setLoreFromList(finalLore);

            if (hideAttributes) {
                punishmentItem.hideAllAttributes();
            }

            // Handler al hacer clic en la sanción
            punishmentItem.setClickHandler(e -> {
                // Cerrar el inventario
                staff.closeInventory();

                // Reemplazar %player% con el nombre del jugador objetivo
                String finalCommand = staffCommand.replace("%player%", target.getName());

                // Buscar placeholder del scope personalizado
                ConfigurationSection customPlaceholders = punishmentConfig.getConfigurationSection("custom_placeholders");
                if (customPlaceholders != null && finalCommand.contains("%scope%")) {
                    String scope = customPlaceholders.getString("scope", "unknown");
                    finalCommand = finalCommand.replace("%scope%", scope);
                }

                // Ejecutar el comando como el staff
                staff.performCommand(finalCommand);

                // Enviar mensaje de confirmación
                MessageUtils.sendMessageAsync(staff, plugin.getConfigManager().getMessage(
                        "actions.punishment-hub.punish-success",
                        "%target%", target.getName(),
                        "%punishment%", punishmentName
                ));
            });

            return punishmentItem;
        } catch (IllegalArgumentException e) {
            // Si el material no es válido, usar un material predeterminado
            plugin.getLogger().warning("Material inválido para la sanción " + punishmentKey + ": " + materialString);
            return createDefaultPunishmentItem(punishmentKey, punishmentName, description, staffCommand,
                    itemName, itemLore, usePlaceholders, hideAttributes,
                    staff, target);
        }
    }

    /**
     * Crea un ítem de sanción predeterminado para cuando el material definido no es válido
     */
    private MenuItem createDefaultPunishmentItem(String punishmentKey, String punishmentName, String description,
                                                 String staffCommand, String itemName,
                                                 List<String> itemLore, boolean usePlaceholders,
                                                 boolean hideAttributes, Player staff, Player target) {
        MenuItem defaultItem = new MenuItem(Material.BARRIER);

        defaultItem.usePlaceholders(usePlaceholders);

        String finalItemName = itemName.replace("%punishment_name%", punishmentName + " (Error)");
        defaultItem.setName(finalItemName);

        List<String> finalLore = new ArrayList<>();
        for (String line : itemLore) {
            finalLore.add(line
                    .replace("%description%", description)
                    .replace("%player_name%", target.getName())
                    .replace("%punishment_key%", punishmentKey)
            );
        }
        finalLore.add("&c¡Error en la configuración del material!");

        defaultItem.setLoreFromList(finalLore);

        if (hideAttributes) {
            defaultItem.hideAllAttributes();
        }

        // Mismo handler que el normal, solo que con error visual
        defaultItem.setClickHandler(e -> {
            staff.closeInventory();
            String finalCommand = staffCommand.replace("%player%", target.getName());

            // Buscar placeholder del scope personalizado
            ConfigurationSection customPlaceholders = punishmentConfig.getConfigurationSection("custom_placeholders");
            if (customPlaceholders != null && finalCommand.contains("%scope%")) {
                String scope = customPlaceholders.getString("scope", "unknown");
                finalCommand = finalCommand.replace("%scope%", scope);
            }

            staff.performCommand(finalCommand);
            MessageUtils.sendMessageAsync(staff, plugin.getConfigManager().getMessage(
                    "actions.punishment-hub.punish-success",
                    "%target%", target.getName(),
                    "%punishment%", punishmentName
            ));
        });

        return defaultItem;
    }
}