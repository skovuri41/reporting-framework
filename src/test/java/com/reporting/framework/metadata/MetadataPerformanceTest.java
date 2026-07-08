package com.reporting.framework.metadata;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.connection.SimpleConnectionProvider;
import com.reporting.framework.metadata.model.ReportCatalog;
import com.reporting.framework.metadata.model.ReportMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests for metadata loading and catalog retrieval.
 * Validates that performance targets are met:
 * - Single report: < 100ms
 * - Catalog (100 reports): < 1s
 */
class MetadataPerformanceTest {

    private ConnectionProvider connectionProvider;
    private MetadataLoader metadataLoader;

    @BeforeEach
    void setUp() throws Exception {
        // Create H2 in-memory database
        connectionProvider = new SimpleConnectionProvider(
            "jdbc:h2:mem:perftest;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
            "sa",
            ""
        );

        // Create schema
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE REPORT_METADATA (
                    REPORT_ID VARCHAR(100) PRIMARY KEY,
                    REPORT_NAME VARCHAR(255) NOT NULL,
                    REPORT_DESCRIPTION VARCHAR(1000),
                    METADATA_JSON NVARCHAR(MAX) NOT NULL,
                    CREATED_DATE DATETIME DEFAULT CURRENT_TIMESTAMP,
                    MODIFIED_DATE DATETIME DEFAULT CURRENT_TIMESTAMP
                )
                """);
        }

        // Initialize loader (no cache warming for controlled testing)
        metadataLoader = new MetadataLoader(connectionProvider, false);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS REPORT_METADATA");
        }
    }

    @Test
    void testSingleReportLoadPerformance() throws Exception {
        // Given - insert a test report
        insertTestReport("perf-001");

        // When - measure load time
        long startTime = System.currentTimeMillis();
        ReportMetadata metadata = metadataLoader.load("perf-001");
        long duration = System.currentTimeMillis() - startTime;

        // Then - should load in < 100ms
        assertThat(metadata).isNotNull();
        assertThat(duration).isLessThan(100L);
        System.out.printf("Single report load time: %dms (target: <100ms)%n", duration);
    }

    @Test
    void testCatalogLoadPerformanceWith100Reports() throws Exception {
        // Given - insert 100 test reports
        int reportCount = 100;
        for (int i = 1; i <= reportCount; i++) {
            insertTestReport(String.format("perf-%03d", i));
        }

        // When - measure catalog load time
        long startTime = System.currentTimeMillis();
        ReportCatalog catalog = metadataLoader.loadCatalog();
        long duration = System.currentTimeMillis() - startTime;

        // Then - should load in < 1 second
        assertThat(catalog).isNotNull();
        assertThat(catalog.getCount()).isEqualTo(reportCount);
        assertThat(catalog.getReports()).hasSize(reportCount);
        assertThat(duration).isLessThan(1000L);
        System.out.printf("Catalog load time for %d reports: %dms (target: <1000ms)%n",
            reportCount, duration);
    }

    @Test
    void testCachedRetrievalPerformance() throws Exception {
        // Given - insert reports and warm cache
        for (int i = 1; i <= 50; i++) {
            insertTestReport(String.format("cached-%03d", i));
        }

        // Warm cache by loading catalog
        metadataLoader.loadCatalog();

        // When - measure cached retrieval time
        long startTime = System.currentTimeMillis();
        ReportMetadata metadata = metadataLoader.load("cached-025");
        long duration = System.currentTimeMillis() - startTime;

        // Then - cached retrieval should be very fast (< 10ms)
        assertThat(metadata).isNotNull();
        assertThat(duration).isLessThan(10L);
        System.out.printf("Cached report retrieval time: %dms (target: <10ms)%n", duration);
    }

    @Test
    void testCatalogCountAccuracy() throws Exception {
        // Given - insert varying number of reports
        int[] reportCounts = {10, 50, 100};

        for (int count : reportCounts) {
            // Clear and reload
            tearDown();
            setUp();

            // Insert reports
            for (int i = 1; i <= count; i++) {
                insertTestReport(String.format("report-%03d", i));
            }

            // When
            ReportCatalog catalog = metadataLoader.loadCatalog();

            // Then - count should always match
            assertThat(catalog.getCount()).isEqualTo(count);
            assertThat(catalog.getReports()).hasSize(count);
            System.out.printf("Catalog count accuracy verified for %d reports%n", count);
        }
    }

    @Test
    void testCacheRefreshPerformance() throws Exception {
        // Given - initial set of reports
        for (int i = 1; i <= 30; i++) {
            insertTestReport(String.format("initial-%03d", i));
        }
        metadataLoader.loadCatalog(); // Initial cache

        // Add more reports
        for (int i = 1; i <= 20; i++) {
            insertTestReport(String.format("added-%03d", i));
        }

        // When - measure refresh time
        long startTime = System.currentTimeMillis();
        metadataLoader.refreshCache();
        long duration = System.currentTimeMillis() - startTime;

        // Then - refresh should be fast
        ReportCatalog catalog = metadataLoader.loadCatalog();
        assertThat(catalog.getCount()).isEqualTo(50);
        assertThat(duration).isLessThan(500L); // Refresh should be fast
        System.out.printf("Cache refresh time for 50 reports: %dms (target: <500ms)%n", duration);
    }

    // Helper methods

    private void insertTestReport(String reportId) throws Exception {
        String json = createMinimalReportJson(reportId);
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = String.format("""
                INSERT INTO REPORT_METADATA (REPORT_ID, REPORT_NAME, REPORT_DESCRIPTION, METADATA_JSON)
                VALUES ('%s', 'Performance Test Report', 'Test', '%s')
                """, reportId, json.replace("'", "''"));

            stmt.execute(sql);
        }
    }

    private String createMinimalReportJson(String reportId) {
        return String.format("""
            {
                "reportId": "%s",
                "reportName": "Performance Test Report",
                "reportDescription": "Minimal report for performance testing",
                "datasets": [{
                    "datasource": "usp_PerfTest",
                    "datasourceId": "ds-001",
                    "datasourceType": "StoredProc",
                    "datasourceDescription": "Test dataset",
                    "parameters": [{
                        "parameterName": "testParam",
                        "dataType": "INTEGER",
                        "parameterDirection": "Input",
                        "nullable": true
                    }],
                    "columns": [{
                        "sourceColumn": "test_id",
                        "displayName": "testId",
                        "dataType": "INTEGER",
                        "sortable": true,
                        "groupable": false,
                        "filterable": true
                    }]
                }]
            }
            """, reportId);
    }
}
