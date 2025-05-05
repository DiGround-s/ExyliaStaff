package net.exylia.exyliaStaff.database;

import net.exylia.exyliaStaff.database.enums.DatabaseType;

public class DatabaseCredentials {
    private final DatabaseType type;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String sqliteFile;

    public DatabaseCredentials(DatabaseType type, String host, int port, String database, String username, String password, String sqliteFile) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.sqliteFile = sqliteFile;
    }

    public DatabaseType getType() {
        return type;
    }

    public String getJdbcUrl() {
        return switch (type) {
            case MYSQL, MARIADB ->
                    "jdbc:" + (type == DatabaseType.MYSQL ? "mysql" : "mariadb") + "://" + host + ":" + port + "/" + database;
            case SQLITE -> "jdbc:sqlite:" + sqliteFile;
            default -> throw new IllegalArgumentException("Unsupported database type: " + type);
        };
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
