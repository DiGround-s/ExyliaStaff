package net.exylia.exyliaStaff.database;

import net.exylia.commons.database.connection.DatabaseCredentials;
import net.exylia.commons.database.core.DatabaseLoader;
import net.exylia.exyliaStaff.ExyliaStaff;
import net.exylia.exyliaStaff.database.tables.StaffPlayerTable;

/**
 * Database loader for ExyliaStaff plugin.
 * Extends the DatabaseLoader from ExyliaCommons.
 */
public class StaffDatabaseLoader extends DatabaseLoader {
    private final ExyliaStaff plugin;
    private StaffDatabaseManager databaseManager;

    /**
     * Creates a new database loader for the ExyliaStaff plugin.
     *
     * @param plugin The ExyliaStaff plugin instance
     */
    public StaffDatabaseLoader(ExyliaStaff plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads the database using credentials from the plugin's config.
     */
    public void load() {
        // Create credentials from config
        DatabaseCredentials credentials = StaffDatabaseManager.createCredentialsFromConfig(plugin).build();

        // Create database manager
        this.databaseManager = new StaffDatabaseManager(plugin, credentials);

        // Load the database with tables
        super.load(credentials);
    }

    /**
     * Registers the database tables for ExyliaStaff.
     */
    @Override
    protected void registerTables() {
        addTable(new StaffPlayerTable(plugin));

        // Add more tables here as needed
        // addTable(new SomeOtherTable(plugin));
    }

    /**
     * Gets the staff player table.
     *
     * @return The staff player table
     */
    public StaffPlayerTable getStaffPlayerTable() {
        return getTable(StaffPlayerTable.class);
    }

    /**
     * Gets the database manager.
     *
     * @return The database manager
     */
    public StaffDatabaseManager getDatabaseManager() {
        return databaseManager;
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
