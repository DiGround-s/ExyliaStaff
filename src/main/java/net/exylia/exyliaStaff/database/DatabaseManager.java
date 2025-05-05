package net.exylia.exyliaStaff.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.exylia.exyliaStaff.database.enums.DatabaseType;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {

    private final HikariDataSource dataSource;

    public DatabaseManager(DatabaseCredentials credentials) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(credentials.getJdbcUrl());

        if (credentials.getType() != DatabaseType.SQLITE) {
            config.setUsername(credentials.getUsername());
            config.setPassword(credentials.getPassword());
        }

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(60000);
        config.setConnectionTimeout(30000);
        config.setLeakDetectionThreshold(15000);
        config.setPoolName("Exylia-HikariPool");

        if (credentials.getType() == DatabaseType.SQLITE) {
            config.setDriverClassName("org.sqlite.JDBC");
        }

        this.dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}