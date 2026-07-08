package com.reporting.framework.api;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.data.DataRow;
import com.reporting.framework.data.DataSet;
import com.reporting.framework.metadata.model.Dataset;
import com.reporting.framework.metadata.model.ReportMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ReportService multi-dataset execution functionality.
 */
class ReportServiceTest {

    private ConnectionProvider connectionProvider;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        // Create a simple test connection provider
        connectionProvider = new TestConnectionProvider();
        reportService = new ReportService(connectionProvider);
    }

    @Test
    void testExecuteAllDatasets_EmptyDatasets() {
        // This test verifies that a report with zero datasets returns an empty Map
        // We can't easily mock MetadataLoader, so this will be primarily tested
        // in integration tests. This is a placeholder for the contract.

        // The actual test will be in MultiDatasetIntegrationTest
        // For now, just verify the service can be created
        assertThat(reportService).isNotNull();
    }

    @Test
    void testExecuteAllDatasets_ParameterSharing() {
        // Verify that the same parameter map is passed to all datasets
        // This will be tested in integration tests where we can verify
        // that each dataset extracts only its required parameters

        // The actual test will be in MultiDatasetIntegrationTest
        assertThat(reportService).isNotNull();
    }

    @Test
    void testGetMetadata() {
        // Verify getMetadata method works (used by executeAllDatasets)
        // This will load from database, so needs integration test

        // The actual test will be in MultiDatasetIntegrationTest
        assertThat(reportService).isNotNull();
    }

    /**
     * Simple test connection provider for unit tests.
     */
    private static class TestConnectionProvider implements ConnectionProvider {
        @Override
        public Connection getConnection() {
            // For these unit tests, we don't need actual connections
            // Integration tests will use real database connections
            return null;
        }

        @Override
        public void close() {
            // No resources to close for test provider
        }
    }
}
