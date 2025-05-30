package net.exylia.exyliaStaff.managers.staff;

import net.exylia.commons.menu.MenuBuilder;
import net.exylia.commons.menu.MenuItem;
import net.exylia.commons.menu.PaginationMenu;
import net.exylia.commons.utils.MessageUtils;
import net.exylia.exyliaStaff.ExyliaStaff;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

import static net.exylia.commons.utils.MenuUtils.parseSlots;

public class MinerHubManager {
    private final ExyliaStaff plugin;
    private final FileConfiguration menuConfig;

    public MinerHubManager(ExyliaStaff plugin) {
        this.plugin = plugin;
        this.menuConfig = plugin.getConfigManager().getConfig("menus/miner_hub");
    }

    public void openMinerHubInventory(Player player) {
        PaginationMenu minerHubMenu = createMinerHubInventory(player);

        if (minerHubMenu != null) {
            minerHubMenu.open(player);
        } else {
            MessageUtils.sendMessageAsync(player, plugin.getConfigManager().getMessage("actions.miner-hub.no-players", "%height%", String.valueOf(plugin.getConfigManager().getConfig().getInt("miner-hub.min-height", 10))));
        }
    }

    private PaginationMenu createMinerHubInventory(Player staff) {
        List<Player> players = getPlayersByHeight(plugin.getConfigManager().getConfig().getInt("miner-hub.min-height", 10));

        if (players.isEmpty()) {
            return null;
        }


        String playerSlotsString = menuConfig.getString("players.slots", "0-26");
        int rows = menuConfig.getInt("rows", 4);
        int[] playerSlots = parseSlots(playerSlotsString, rows);

        MenuBuilder menuBuilder = new MenuBuilder(plugin);
        PaginationMenu minerHubMenu = menuBuilder.buildPaginationMenu(menuConfig, staff, playerSlots);

        addPlayerItems(minerHubMenu, players, staff);

        return minerHubMenu;
    }

    private List<Player> getPlayersByHeight(int height) {
        return plugin.getServer().getOnlinePlayers().stream()
                .filter(p -> p.getLocation().getY() < height && !p.hasPermission("exylia.staff"))
                .collect(Collectors.toList());
    }

    /**
     * Añade los ítems de jugadores al menú paginado
     *
     * @param paginationMenu El menú donde se añadirán los jugadores
     * @param players Jugadores a mostrar
     */
    private void addPlayerItems(PaginationMenu paginationMenu, List<Player> players, Player staff) {
        boolean playersUsePlaceholders = menuConfig.getBoolean("players.use_placeholders", true);
        boolean playersDynamicUpdate = menuConfig.getBoolean("players.dynamic_update", false);
        int playersUpdateInterval = menuConfig.getInt("players.update_interval", 100);
        String playerItemName = menuConfig.getString("players.name", "<#8a51c4>%player_name%");
        List<String> playerItemLore = menuConfig.getStringList("players.lore");
        String playerMaterial = menuConfig.getString("players.material", "PLAYER_HEAD");
        boolean hideAttributes = menuConfig.getBoolean("players.hide_attributes", false);


        for (Player player : players) {

            MenuItem playerItem = createPlayerItem(player, playerMaterial,
                    playerItemName, playerItemLore,
                    playersUsePlaceholders, playersDynamicUpdate,
                    playersUpdateInterval, hideAttributes, staff);

            paginationMenu.addItem(playerItem);
        }
    }
    /**
     * Crea un ítem de menú para un jugador específico
     *
     * @param player El jugador para quien se crea el ítem
     * @param material El material del ítem
     * @param itemName El nombre del ítem (puede contener placeholders)
     * @param itemLore El lore del ítem (puede contener placeholders)
     * @param usePlaceholders Si se deben usar placeholders
     * @param dynamicUpdate Si el ítem se actualiza dinámicamente
     * @param updateInterval El intervalo de actualización
     * @return El ítem de menú creado
     */
    private MenuItem createPlayerItem(Player player, String material,
                                      String itemName, List<String> itemLore,
                                      boolean usePlaceholders, boolean dynamicUpdate,
                                      int updateInterval, boolean hideAttributes, Player staff) {

        MenuItem playerItem = new MenuItem(material);

        playerItem.usePlaceholders(usePlaceholders);
        playerItem.setDynamicUpdate(dynamicUpdate);
        playerItem.setUpdateInterval(updateInterval);
        playerItem.setPlaceholderPlayer(player);
        playerItem.setName(itemName);
        if (hideAttributes) {
            playerItem.hideAllAttributes();
        }

        playerItem.setLoreFromList(itemLore);

        playerItem.applySkullOwner(player);

        playerItem.setClickHandler(e -> staff.teleport(player));

        return playerItem;
    }
}
