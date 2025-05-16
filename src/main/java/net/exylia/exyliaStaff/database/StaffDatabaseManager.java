package net.exylia.exyliaStaff.database;

import net.exylia.commons.database.connection.DatabaseCredentials;
import net.exylia.commons.database.core.AbstractDatabaseManager;
import net.exylia.commons.database.enums.DatabaseType;
import net.exylia.exyliaStaff.ExyliaStaff;

/**
 * Database manager for ExyliaStaff plugin.
 * Extends the AbstractDatabaseManager from ExyliaCommons.
 */
public class StaffDatabaseManager extends AbstractDatabaseManager {
    private final ExyliaStaff plugin;

    /**
     * Creates a new database manager with credentials from the plugin config.
     *
     * @param plugin The ExyliaStaff plugin instance
     * @param credentials The database credentials
     */
    public StaffDatabaseManager(ExyliaStaff plugin, DatabaseCredentials credentials) {
        super(credentials);
        this.plugin = plugin;
    }

    /**
     * Creates a database credentials builder with values from the plugin's config.
     *
     * @param plugin The ExyliaStaff plugin instance
     * @return The database credentials builder
     */
    public static DatabaseCredentials.Builder createCredentialsFromConfig(ExyliaStaff plugin) {
        var config = plugin.getConfigManager().getConfig("config");

        // Get database type
        String typeStr = config.getString("database.type", "SQLITE").toUpperCase();
        DatabaseType type = DatabaseType.valueOf(typeStr);

        // Create initial builder with the type
        DatabaseCredentials.Builder builder = new DatabaseCredentials.Builder(type);

        // Configure SQLite settings if needed
        if (type == DatabaseType.SQLITE) {
            String fileName = config.getString("database.sqlite.file_name", "exyliastaff.db");
            builder.sqliteFile("plugins/" + plugin.getName() + "/" + fileName);
        }
        // Configure MySQL/MariaDB settings if needed
        else {
            builder.host(config.getString("database.mysql.host", "localhost"))
                    .port(config.getInt("database.mysql.port", 3306))
                    .database(config.getString("database.mysql.database", "exyliastaff"))
                    .username(config.getString("database.mysql.username", "root"))
                    .password(config.getString("database.mysql.password", ""))
                    .poolName("ExyliaStaff-HikariPool");
        }

        // Set connection pool properties if specified in config
        if (config.contains("database.pool.max_size")) {
            builder.maxPoolSize(config.getInt("database.pool.max_size", 10));
        }

        if (config.contains("database.pool.min_idle")) {
            builder.minIdle(config.getInt("database.pool.min_idle", 2));
        }

        return builder;
    }

    /**
     * Gets the ExyliaStaff plugin instance.
     *
     * @return The plugin instance
     */
    public ExyliaStaff getPlugin() {
        return plugin;
    }
}
