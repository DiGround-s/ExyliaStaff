package net.exylia.exyliaStaff.database.tables;

import net.exylia.commons.database.core.DatabaseTable;
import net.exylia.commons.database.executor.SQLExecutor;
import net.exylia.commons.database.util.DatabaseErrors;
import net.exylia.commons.database.util.SerializationUtil;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.models.StaffPlayer;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class StaffPlayerTable implements DatabaseTable {
    private static final Logger LOGGER = Logger.getLogger(StaffPlayerTable.class.getName());
    private static final String TABLE_NAME = "staff_players";

    private final ExyliaStaff plugin;

    public StaffPlayerTable(ExyliaStaff plugin) {
        this.plugin = plugin;
    }

    @Override
    public void createTable() {
        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection();
             SQLExecutor executor = new SQLExecutor(conn)) {

            executor.update("""
                CREATE TABLE IF NOT EXISTS %s (
                    uuid VARCHAR(36) PRIMARY KEY,
                    vanished INTEGER NOT NULL DEFAULT 0,
                    staff_mode INTEGER NOT NULL DEFAULT 0,
                    inventory TEXT,
                    armor TEXT,
                    offhand TEXT,
                    exp REAL DEFAULT 0.0,
                    level INTEGER DEFAULT 0
                )
            """.formatted(TABLE_NAME));

            if (plugin.getDatabaseLoader().getDatabaseType() == net.exylia.commons.database.enums.DatabaseType.MYSQL ||
                    plugin.getDatabaseLoader().getDatabaseType() == net.exylia.commons.database.enums.DatabaseType.MARIADB) {

                ResultSet rs = executor.query("""
                    SELECT COUNT(*) 
                    FROM information_schema.columns 
                    WHERE table_name = '%s' 
                    AND column_name = 'created_at'
                """.formatted(TABLE_NAME));

                if (rs.next() && rs.getInt(1) == 0) {
                    executor.update("ALTER TABLE %s ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP".formatted(TABLE_NAME));
                }

                rs.close();

                rs = executor.query("""
                    SELECT COUNT(*) 
                    FROM information_schema.columns 
                    WHERE table_name = '%s' 
                    AND column_name = 'updated_at'
                """.formatted(TABLE_NAME));

                if (rs.next() && rs.getInt(1) == 0) {
                    executor.update("ALTER TABLE %s ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP".formatted(TABLE_NAME));
                }

                rs.close();
            } else if (plugin.getDatabaseLoader().getDatabaseType() == net.exylia.commons.database.enums.DatabaseType.SQLITE) {
                ResultSet rs = executor.query("PRAGMA table_info(%s)".formatted(TABLE_NAME));

                boolean hasCreatedAt = false;
                boolean hasUpdatedAt = false;

                while (rs.next()) {
                    String columnName = rs.getString("name");
                    if ("created_at".equals(columnName)) {
                        hasCreatedAt = true;
                    } else if ("updated_at".equals(columnName)) {
                        hasUpdatedAt = true;
                    }
                }

                rs.close();

                if (!hasCreatedAt) {
                    executor.update("ALTER TABLE %s ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP".formatted(TABLE_NAME));
                }

                if (!hasUpdatedAt) {
                    executor.update("ALTER TABLE %s ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP".formatted(TABLE_NAME));
                }
            }

        } catch (SQLException e) {
            DatabaseErrors.logTableCreationError(TABLE_NAME, e);
        }
    }

    /**
     * Gets the name of the table.
     *
     * @return The table name
     */
    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    /**
     * Saves a staff player to the database.
     *
     * @param player The staff player to save
     * @return true if the save was successful, false otherwise
     */
    public boolean saveStaffPlayer(StaffPlayer player) {
        if (player == null) {
            return false;
        }

        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection();
             SQLExecutor executor = new SQLExecutor(conn)) {

            String inventoryBase64 = null;
            String armorBase64 = null;
            String offhandBase64 = null;

            if (player.getInventory() != null) {
                inventoryBase64 = SerializationUtil.itemStackArrayToBase64(player.getInventory());
                armorBase64 = SerializationUtil.itemStackArrayToBase64(player.getArmor());
                offhandBase64 = SerializationUtil.itemStackToBase64(player.getOffHandItem());
            }

            boolean hasTimestampColumns = hasTimestampColumns(executor);

            boolean playerExists = executor.exists("SELECT 1 FROM %s WHERE uuid = ?".formatted(TABLE_NAME), player.getUuid().toString());

            int rows;

            if (playerExists) {
                if (hasTimestampColumns) {
                    rows = executor.update("""
                        UPDATE %s SET 
                            vanished = ?, 
                            staff_mode = ?, 
                            inventory = ?, 
                            armor = ?, 
                            offhand = ?, 
                            exp = ?, 
                            level = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE uuid = ?
                    """.formatted(TABLE_NAME),
                            player.isVanished() ? 1 : 0,
                            player.isInStaffMode() ? 1 : 0,
                            inventoryBase64,
                            armorBase64,
                            offhandBase64,
                            player.getExp(),
                            player.getLevel(),
                            player.getUuid().toString()
                    );
                } else {
                    rows = executor.update("""
                        UPDATE %s SET 
                            vanished = ?, 
                            staff_mode = ?, 
                            inventory = ?, 
                            armor = ?, 
                            offhand = ?, 
                            exp = ?, 
                            level = ?
                        WHERE uuid = ?
                    """.formatted(TABLE_NAME),
                            player.isVanished() ? 1 : 0,
                            player.isInStaffMode() ? 1 : 0,
                            inventoryBase64,
                            armorBase64,
                            offhandBase64,
                            player.getExp(),
                            player.getLevel(),
                            player.getUuid().toString()
                    );
                }
            }
            else {
                if (hasTimestampColumns) {
                    rows = executor.update("""
                        INSERT INTO %s (uuid, vanished, staff_mode, inventory, armor, offhand, exp, level, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """.formatted(TABLE_NAME),
                            player.getUuid().toString(),
                            player.isVanished() ? 1 : 0,
                            player.isInStaffMode() ? 1 : 0,
                            inventoryBase64,
                            armorBase64,
                            offhandBase64,
                            player.getExp(),
                            player.getLevel()
                    );
                } else {
                    rows = executor.update("""
                        INSERT INTO %s (uuid, vanished, staff_mode, inventory, armor, offhand, exp, level)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.formatted(TABLE_NAME),
                            player.getUuid().toString(),
                            player.isVanished() ? 1 : 0,
                            player.isInStaffMode() ? 1 : 0,
                            inventoryBase64,
                            armorBase64,
                            offhandBase64,
                            player.getExp(),
                            player.getLevel()
                    );
                }
            }

            return rows > 0;

        } catch (SQLException e) {
            DatabaseErrors.logUpdateError(TABLE_NAME, e);
            return false;
        }
    }

    /**
     * Checks if the table has timestamp columns.
     *
     * @param executor The SQL executor
     * @return true if the table has timestamp columns, false otherwise
     */
    private boolean hasTimestampColumns(SQLExecutor executor) {
        try {
            if (plugin.getDatabaseLoader().getDatabaseType() == net.exylia.commons.database.enums.DatabaseType.MYSQL ||
                    plugin.getDatabaseLoader().getDatabaseType() == net.exylia.commons.database.enums.DatabaseType.MARIADB) {

                ResultSet rs = executor.query("""
                    SELECT COUNT(*) 
                    FROM information_schema.columns 
                    WHERE table_name = '%s' 
                    AND column_name = 'updated_at'
                """.formatted(TABLE_NAME));

                if (rs.next() && rs.getInt(1) > 0) {
                    rs.close();
                    return true;
                }

                rs.close();
                return false;

            } else if (plugin.getDatabaseLoader().getDatabaseType() == net.exylia.commons.database.enums.DatabaseType.SQLITE) {

                ResultSet rs = executor.query("PRAGMA table_info(%s)".formatted(TABLE_NAME));

                while (rs.next()) {
                    String columnName = rs.getString("name");
                    if ("updated_at".equals(columnName)) {
                        rs.close();
                        return true;
                    }
                }

                rs.close();
                return false;
            }

            return false;

        } catch (SQLException e) {
            DatabaseErrors.logQueryError(TABLE_NAME, e);
            return false;
        }
    }

    /**
     * Saves a staff player to the database asynchronously.
     *
     * @param player The staff player to save
     * @return A CompletableFuture that will be completed with the result of the save operation
     */
    public CompletableFuture<Boolean> saveStaffPlayerAsync(StaffPlayer player) {
        return CompletableFuture.supplyAsync(() -> saveStaffPlayer(player));
    }

    /**
     * Loads a staff player from the database.
     *
     * @param uuid The UUID of the player to load
     * @return The loaded staff player, or a new staff player if none was found
     */
    public StaffPlayer loadStaffPlayer(UUID uuid) {
        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection();
             SQLExecutor executor = new SQLExecutor(conn)) {

            ResultSet rs = executor.query("""
                SELECT vanished, staff_mode, inventory, armor, offhand, exp, level 
                FROM %s WHERE uuid = ?
            """.formatted(TABLE_NAME), uuid.toString());

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
                    inventory = SerializationUtil.base64ToItemStackArray(inventoryBase64);
                    armor = SerializationUtil.base64ToItemStackArray(armorBase64);
                    offHandItem = SerializationUtil.base64ToItemStack(offhandBase64);
                }

                return new StaffPlayer(uuid, vanished, staffMode, inventory, armor, offHandItem, exp, level);
            }

            return new StaffPlayer(uuid, false, false);

        } catch (SQLException e) {
            DatabaseErrors.logQueryError(TABLE_NAME, e);
            return new StaffPlayer(uuid, false, false);
        }
    }

    public CompletableFuture<StaffPlayer> loadStaffPlayerAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> loadStaffPlayer(uuid));
    }

    public boolean deleteStaffPlayer(UUID uuid) {
        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection();
             SQLExecutor executor = new SQLExecutor(conn)) {

            int rows = executor.update("DELETE FROM %s WHERE uuid = ?".formatted(TABLE_NAME), uuid.toString());
            return rows > 0;

        } catch (SQLException e) {
            DatabaseErrors.logDeleteError(TABLE_NAME, e);
            return false;
        }
    }

    /**
     * Deletes a staff player from the database asynchronously.
     *
     * @param uuid The UUID of the player to delete
     * @return A CompletableFuture that will be completed with the result of the delete operation
     */
    public CompletableFuture<Boolean> deleteStaffPlayerAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> deleteStaffPlayer(uuid));
    }

    /**
     * Checks if a staff player exists in the database.
     *
     * @param uuid The UUID of the player to check
     * @return true if the player exists, false otherwise
     */
    public boolean staffPlayerExists(UUID uuid) {
        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection();
             SQLExecutor executor = new SQLExecutor(conn)) {

            return executor.exists("SELECT 1 FROM %s WHERE uuid = ? LIMIT 1".formatted(TABLE_NAME), uuid.toString());

        } catch (SQLException e) {
            DatabaseErrors.logQueryError(TABLE_NAME, e);
            return false;
        }
    }

    /**
     * Checks if a staff player exists in the database asynchronously.
     *
     * @param uuid The UUID of the player to check
     * @return A CompletableFuture that will be completed with whether the player exists
     */
    public CompletableFuture<Boolean> staffPlayerExistsAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> staffPlayerExists(uuid));
    }

    /**
     * Updates a specific field for a staff player.
     *
     * @param uuid The UUID of the player to update
     * @param field The field to update
     * @param value The new value for the field
     * @return true if the update was successful, false otherwise
     */
    public boolean updateStaffPlayerField(UUID uuid, String field, Object value) {
        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection();
             SQLExecutor executor = new SQLExecutor(conn)) {

            boolean hasTimestampColumns = hasTimestampColumns(executor);

            int rows;
            if (hasTimestampColumns) {
                rows = executor.update(
                        "UPDATE %s SET %s = ?, updated_at = CURRENT_TIMESTAMP WHERE uuid = ?".formatted(TABLE_NAME, field),
                        value, uuid.toString()
                );
            } else {
                rows = executor.update(
                        "UPDATE %s SET %s = ? WHERE uuid = ?".formatted(TABLE_NAME, field),
                        value, uuid.toString()
                );
            }

            return rows > 0;

        } catch (SQLException e) {
            DatabaseErrors.logUpdateError(TABLE_NAME, e);
            return false;
        }
    }

    /**
     * Updates a specific field for a staff player asynchronously.
     *
     * @param uuid The UUID of the player to update
     * @param field The field to update
     * @param value The new value for the field
     * @return A CompletableFuture that will be completed with the result of the update operation
     */
    public CompletableFuture<Boolean> updateStaffPlayerFieldAsync(UUID uuid, String field, Object value) {
        return CompletableFuture.supplyAsync(() -> updateStaffPlayerField(uuid, field, value));
    }

    /**
     * Gets all staff players with staff mode enabled.
     *
     * @return A CompletableFuture that will be completed with a list of staff player UUIDs
     */
    public CompletableFuture<List<UUID>> getStaffModePlayersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> staffPlayers = new ArrayList<>();

            try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection();
                 SQLExecutor executor = new SQLExecutor(conn)) {

                ResultSet rs = executor.query("SELECT uuid FROM %s WHERE staff_mode = 1".formatted(TABLE_NAME));

                while (rs.next()) {
                    staffPlayers.add(UUID.fromString(rs.getString("uuid")));
                }

            } catch (SQLException e) {
                DatabaseErrors.logQueryError(TABLE_NAME, e);
            }

            return staffPlayers;
        });
    }

    /**
     * Gets all staff players with vanish enabled.
     *
     * @return A CompletableFuture that will be completed with a list of staff player UUIDs
     */
    public CompletableFuture<List<UUID>> getVanishedPlayersAsync() {
        return CompletableFuture.supplyAsync(() -> {
            List<UUID> vanishedPlayers = new ArrayList<>();

            try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection();
                 SQLExecutor executor = new SQLExecutor(conn)) {

                ResultSet rs = executor.query("SELECT uuid FROM %s WHERE vanished = 1".formatted(TABLE_NAME));

                while (rs.next()) {
                    vanishedPlayers.add(UUID.fromString(rs.getString("uuid")));
                }

            } catch (SQLException e) {
                DatabaseErrors.logQueryError(TABLE_NAME, e);
            }

            return vanishedPlayers;
        });
    }
}
