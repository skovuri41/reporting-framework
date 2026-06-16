package com.reporting.framework.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Simple ConnectionProvider implementation using DriverManager.
 * Useful for testing but not recommended for production (use DataSourceConnectionProvider instead).
 */
public class SimpleConnectionProvider implements ConnectionProvider {

    private final String url;
    private final String username;
    private final String password;

    public SimpleConnectionProvider(String url, String username, String password) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Database URL cannot be null or empty");
        }
        this.url = url;
        this.username = username;
        this.password = password;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public void close() {
        // No resources to close with DriverManager
    }
}
