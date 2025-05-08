package net.exylia.exyliaStaff.database.tables;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.database.SQLExecutor;
import net.exylia.exyliaStaff.database.core.DatabaseTable;
import net.exylia.exyliaStaff.models.StaffPlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.UUID;

import static net.exylia.commons.utils.DebugUtils.logError;

public class StaffPlayerTable implements DatabaseTable {

    private final ExyliaStaff plugin;

    public StaffPlayerTable(ExyliaStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public void createTable() {
        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection()) {
            SQLExecutor executor = new SQLExecutor(conn);
            executor.update("""
                CREATE TABLE IF NOT EXISTS staff_players (
                    uuid TEXT PRIMARY KEY,
                    vanished INTEGER NOT NULL,
                    staff_mode INTEGER NOT NULL,
                    inventory TEXT,
                    armor TEXT,
                    offhand TEXT,
                    exp REAL,
                    level INTEGER
                )
            """);
        } catch (SQLException e) {
            logError("No se pudo crear la tabla 'staff_players': " + e.getMessage());
        }
    }

    public void saveStaffPlayer(StaffPlayer player) {
        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection()) {
            SQLExecutor executor = new SQLExecutor(conn);

            String inventoryBase64 = null;
            String armorBase64 = null;
            String offhandBase64 = null;

            if (player.getInventory() != null) {
                inventoryBase64 = itemStackArrayToBase64(player.getInventory());
                armorBase64 = itemStackArrayToBase64(player.getArmor());
                offhandBase64 = itemStackToBase64(player.getOffHandItem());
            }

            executor.update("""
                INSERT OR REPLACE INTO staff_players (uuid, vanished, staff_mode, inventory, armor, offhand, exp, level)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
                    player.getUuid().toString(),
                    player.isVanished() ? 1 : 0,
                    player.isInStaffMode() ? 1 : 0,
                    inventoryBase64,
                    armorBase64,
                    offhandBase64,
                    player.getExp(),
                    player.getLevel()
            );
        } catch (Exception e) {
            logError("Error guardando StaffPlayer: " + e.getMessage());
        }
    }

    public StaffPlayer loadStaffPlayer(UUID uuid) {
        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection()) {
            SQLExecutor executor = new SQLExecutor(conn);
            ResultSet rs = executor.query("""
                SELECT vanished, staff_mode, inventory, armor, offhand, exp, level 
                FROM staff_players WHERE uuid = ?
            """, uuid.toString());

            if (rs.next()) {
                boolean vanished = rs.getInt("vanished") == 1;
                boolean staffMode = rs.getInt("staff_mode") == 1;

                String inventoryBase64 = rs.getString("inventory");
                String armorBase64 = rs.getString("armor");
                String offhandBase64 = rs.getString("offhand");
                float exp = rs.getFloat("exp");
                int level = rs.getInt("level");

                ItemStack[] inventory = null;
                ItemStack[] armor = null;
                ItemStack offHandItem = null;

                if (inventoryBase64 != null) {
                    inventory = base64ToItemStackArray(inventoryBase64);
                    armor = base64ToItemStackArray(armorBase64);
                    offHandItem = base64ToItemStack(offhandBase64);
                }

                return new StaffPlayer(uuid, vanished, staffMode, inventory, armor, offHandItem, exp, level);
            }
        } catch (Exception e) {
            logError("Error cargando StaffPlayer: " + e.getMessage());
        }
        return new StaffPlayer(uuid, false, false);
    }

    // Métodos para serializar y deserializar ItemStacks a Base64
    private String itemStackArrayToBase64(ItemStack[] items) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeInt(items.length);

            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }

    private ItemStack[] base64ToItemStackArray(String base64) throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            ItemStack[] items = new ItemStack[dataInput.readInt()];

            for (int i = 0; i < items.length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }

            return items;
        }
    }

    private String itemStackToBase64(ItemStack item) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {

            dataOutput.writeObject(item);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }

    private ItemStack base64ToItemStack(String base64) throws Exception {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {

            return (ItemStack) dataInput.readObject();
        }
    }
}