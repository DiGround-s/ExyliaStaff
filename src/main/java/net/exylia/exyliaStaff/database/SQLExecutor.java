package net.exylia.exyliaStaff.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLExecutor {

    private final Connection connection;

    public SQLExecutor(Connection connection) {
        this.connection = connection;
    }

    public void update(String sql, Object... params) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            setParams(stmt, params);
            stmt.executeUpdate();
        }
    }

    public ResultSet query(String sql, Object... params) throws SQLException {
        PreparedStatement stmt = connection.prepareStatement(sql);
        setParams(stmt, params);
        return stmt.executeQuery();
    }

    private void setParams(PreparedStatement stmt, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            stmt.setObject(i + 1, params[i]);
        }
    }
}
