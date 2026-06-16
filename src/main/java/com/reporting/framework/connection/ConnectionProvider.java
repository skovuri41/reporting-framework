package com.reporting.framework.connection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for providing database connections to the reporting framework.
 * Implementations can use connection pools, data sources, or custom connection strategies.
 */
public interface ConnectionProvider {

    /**
     * Get a database connection.
     *
     * @return A database connection
     * @throws SQLException if connection cannot be obtained
     */
    Connection getConnection() throws SQLException;

    /**
     * Close the connection provider and release any resources.
     */
    void close();
}
