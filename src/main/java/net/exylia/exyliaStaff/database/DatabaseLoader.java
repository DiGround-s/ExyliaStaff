package net.exylia.exyliaStaff.database;

import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.database.core.DatabaseTable;
import net.exylia.exyliaStaff.database.enums.DatabaseType;
import net.exylia.exyliaStaff.database.tables.StaffPlayerTable;

import java.util.ArrayList;
import java.util.List;

public class DatabaseLoader {

    private final ExyliaStaff plugin;
    private DatabaseManager databaseManager;
    private final List<DatabaseTable> tables = new ArrayList<>();

    public DatabaseLoader(ExyliaStaff plugin) {
        this.plugin = plugin;
    }

    public void load() {
        var config = plugin.getConfigManager().getConfig("config");

        String typeStr = config.getString("database.type", "SQLITE").toUpperCase();
        DatabaseType type = DatabaseType.valueOf(typeStr);

        String fileName = config.getString("database.sqlite.file_name", "default.db");

        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String dbName = config.getString("database.mysql.database", "test");
        String user = config.getString("database.mysql.username", "root");
        String pass = config.getString("database.mysql.password", "");

        DatabaseCredentials credentials = new DatabaseCredentials(
                type,
                host,
                port,
                dbName,
                user,
                pass,
                "plugins/" + plugin.getName() + "/" + fileName
        );

        this.databaseManager = new DatabaseManager(credentials);

        registerTables();
        createTables();
    }

    private void registerTables() {
        tables.add(new StaffPlayerTable(plugin));
    }

    private void createTables() {
        for (DatabaseTable table : tables) {
            table.createTable();
        }
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
