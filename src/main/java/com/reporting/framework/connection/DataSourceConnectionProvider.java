package com.reporting.framework.connection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * ConnectionProvider implementation that uses a DataSource.
 * Compatible with connection pooling libraries like HikariCP.
 */
public class DataSourceConnectionProvider implements ConnectionProvider {

    private final DataSource dataSource;

    public DataSourceConnectionProvider(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException("DataSource cannot be null");
        }
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        // DataSource lifecycle is managed externally
        // If using HikariCP, call HikariDataSource.close() from the application
    }
}
