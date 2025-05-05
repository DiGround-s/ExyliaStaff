package net.exylia.exyliaStaff.database.tables;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.database.SQLExecutor;
import net.exylia.exyliaStaff.database.core.DatabaseTable;
import net.exylia.exyliaStaff.models.StaffPlayer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class StaffPlayerTable implements DatabaseTable {

    private final ExyliaStaff plugin;

    public StaffPlayerTable(ExyliaStaff plugin) {
        this.plugin = plugin;
    }

    public void createTable() {
        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection()) {
            SQLExecutor executor = new SQLExecutor(conn);
            executor.update("""
                CREATE TABLE IF NOT EXISTS staff_players (
                    uuid TEXT PRIMARY KEY,
                    vanished INTEGER NOT NULL,
                    staff_mode INTEGER NOT NULL
                )
            """);
        } catch (SQLException e) {
            plugin.getLogger().severe("No se pudo crear la tabla 'staff_players': " + e.getMessage());
        }
    }

    public void saveStaffPlayer(StaffPlayer player) {
        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection()) {
            SQLExecutor executor = new SQLExecutor(conn);
            executor.update("""
                INSERT OR REPLACE INTO staff_players (uuid, vanished, staff_mode)
                VALUES (?, ?, ?)
            """,
                    player.getUuid().toString(),
                    player.isVanished() ? 1 : 0,
                    player.isInStaffMode() ? 1 : 0
            );
        } catch (SQLException e) {
            plugin.getLogger().severe("Error guardando StaffPlayer: " + e.getMessage());
        }
    }

    public StaffPlayer loadStaffPlayer(UUID uuid) {
        try (Connection conn = plugin.getDatabaseLoader().getDatabaseManager().getConnection()) {
            SQLExecutor executor = new SQLExecutor(conn);
            ResultSet rs = executor.query("""
                SELECT vanished, staff_mode FROM staff_players WHERE uuid = ?
            """, uuid.toString());

            if (rs.next()) {
                boolean vanished = rs.getInt("vanished") == 1;
                boolean staffMode = rs.getInt("staff_mode") == 1;
                return new StaffPlayer(uuid, vanished, staffMode);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error cargando StaffPlayer: " + e.getMessage());
        }
        return null;
    }
}
