package com.osman.vssfx.db;

import com.osman.vssfx.config.DbConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Db {
    private Db() {}

    static {
        try {
            Class.forName(DbConfig.driver());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("JDBC Driver not found: " + DbConfig.driver(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DbConfig.url(), DbConfig.user(), DbConfig.password());
    }

    public static boolean ping() {
        try (Connection c = getConnection()) {
            return c.isValid(2);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
