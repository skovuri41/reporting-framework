package com.reporting.framework.metadata;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.connection.SimpleConnectionProvider;
import com.reporting.framework.exception.MetadataNotFoundException;
import com.reporting.framework.metadata.model.Column;
import com.reporting.framework.metadata.model.Dataset;
import com.reporting.framework.metadata.model.Parameter;
import com.reporting.framework.metadata.model.ParameterDirection;
import com.reporting.framework.metadata.model.ReportCatalog;
import com.reporting.framework.metadata.model.ReportMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for metadata loading with H2 database.
 * Tests the complete flow: database → JSON → validation → DTOs → cache.
 */
class MetadataIntegrationTest {

    private ConnectionProvider connectionProvider;
    private MetadataLoader metadataLoader;

    @BeforeEach
    void setUp() throws Exception {
        // Create H2 in-memory database
        connectionProvider = new SimpleConnectionProvider(
            "jdbc:h2:mem:test;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
            "sa",
            ""
        );

        // Create schema
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create REPORT_METADATA table
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
        // Drop tables
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS REPORT_METADATA");
        }
    }

    @Test
    void testLoadSingleReportFromDatabase() throws Exception {
        // Given - insert test metadata
        insertTestReport("report-001", createEmployeeReportJson());

        // When
        ReportMetadata metadata = metadataLoader.load("report-001");

        // Then - verify structure
        assertThat(metadata).isNotNull();
        assertThat(metadata.getReportId()).isEqualTo("report-001");
        assertThat(metadata.getReportName()).isEqualTo("Employee Report");
        assertThat(metadata.getReportDescription()).isEqualTo("Employee data analysis");
        assertThat(metadata.getDatasets()).hasSize(1);
    }

    @Test
    void testParameterNameTransformation() throws Exception {
        // Given - insert metadata with snake_case parameter name
        insertTestReport("test-report", createEmployeeReportJson());

        // When
        ReportMetadata metadata = metadataLoader.load("test-report");

        // Then - parameter names should be transformed to camelCase
        Dataset dataset = metadata.getDatasets().get(0);
        Parameter param = dataset.getParameters().get(0);

        assertThat(param.getParameterName()).isEqualTo("departmentId");
        assertThat(param.getDataType()).isEqualTo("INTEGER");
        assertThat(param.getParameterDirection()).isEqualTo(ParameterDirection.INPUT);
        assertThat(param.getNullable()).isTrue();
    }

    @Test
    void testColumnDisplayNameTransformation() throws Exception {
        // Given - insert metadata with snake_case display names
        insertTestReport("test-report", createEmployeeReportJson());

        // When
        ReportMetadata metadata = metadataLoader.load("test-report");

        // Then - displayName should be transformed, sourceColumn preserved
        Dataset dataset = metadata.getDatasets().get(0);
        Column column = dataset.getColumns().get(0);

        assertThat(column.getSourceColumn()).isEqualTo("employee_id"); // Preserved exactly
        assertThat(column.getDisplayName()).isEqualTo("employeeId"); // Transformed to camelCase
        assertThat(column.getDataType()).isEqualTo("INTEGER");
    }

    @Test
    void testSourceColumnPreserved() throws Exception {
        // Given
        insertTestReport("test-report", createEmployeeReportJson());

        // When
        ReportMetadata metadata = metadataLoader.load("test-report");

        // Then - sourceColumn should NOT be transformed
        Dataset dataset = metadata.getDatasets().get(0);

        assertThat(dataset.getColumns().get(0).getSourceColumn()).isEqualTo("employee_id");
        assertThat(dataset.getColumns().get(1).getSourceColumn()).isEqualTo("full_name");
        assertThat(dataset.getColumns().get(2).getSourceColumn()).isEqualTo("department_name");
    }

    @Test
    void testLoadNonExistentReport() {
        // When/Then
        assertThatThrownBy(() -> metadataLoader.load("non-existent-report"))
            .isInstanceOf(MetadataNotFoundException.class)
            .hasMessageContaining("non-existent-report");
    }

    @Test
    void testLoadCatalog() throws Exception {
        // Given - insert multiple reports
        insertTestReport("report-001", createEmployeeReportJson());
        insertTestReport("report-002", createSalesReportJson());
        insertTestReport("report-003", createDepartmentReportJson());

        // When
        ReportCatalog catalog = metadataLoader.loadCatalog();

        // Then
        assertThat(catalog).isNotNull();
        assertThat(catalog.getCount()).isEqualTo(3);
        assertThat(catalog.getReports()).hasSize(3);

        // Verify report IDs
        assertThat(catalog.getReports())
            .extracting(ReportMetadata::getReportId)
            .containsExactlyInAnyOrder("report-001", "report-002", "report-003");
    }

    @Test
    void testCacheRefresh() throws Exception {
        // Given - initial report
        insertTestReport("report-001", createEmployeeReportJson());

        // When - load and cache
        ReportMetadata metadata1 = metadataLoader.load("report-001");
        assertThat(metadata1.getReportId()).isEqualTo("report-001");

        // Add new report
        insertTestReport("report-002", createSalesReportJson());

        // Refresh cache
        metadataLoader.refreshCache();

        // Then - new report should be available
        ReportCatalog catalog = metadataLoader.loadCatalog();
        assertThat(catalog.getCount()).isEqualTo(2);
    }

    @Test
    void testImmutabilityOfLoadedMetadata() throws Exception {
        // Given
        insertTestReport("test-report", createEmployeeReportJson());

        // When
        ReportMetadata metadata = metadataLoader.load("test-report");

        // Then - attempting to modify should fail
        assertThatThrownBy(() -> metadata.getDatasets().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // Helper methods

    private void insertTestReport(String reportId, String metadataJson) throws Exception {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = String.format("""
                INSERT INTO REPORT_METADATA (REPORT_ID, REPORT_NAME, REPORT_DESCRIPTION, METADATA_JSON)
                VALUES ('%s', 'Test Report', 'Test Description', '%s')
                """, reportId, metadataJson.replace("'", "''"));

            stmt.execute(sql);
        }
    }

    private String createEmployeeReportJson() {
        return """
            {
                "reportId": "report-001",
                "reportName": "Employee Report",
                "reportDescription": "Employee data analysis",
                "datasets": [{
                    "datasource": "usp_GetEmployees",
                    "datasourceId": "ds-001",
                    "datasourceType": "StoredProc",
                    "datasourceDescription": "Retrieves employee data",
                    "parameters": [{
                        "parameterName": "departmentId",
                        "dataType": "INTEGER",
                        "parameterDirection": "Input",
                        "nullable": true
                    }],
                    "columns": [
                        {
                            "sourceColumn": "employee_id",
                            "displayName": "employeeId",
                            "dataType": "INTEGER",
                            "sortable": true,
                            "groupable": false,
                            "filterable": true
                        },
                        {
                            "sourceColumn": "full_name",
                            "displayName": "fullName",
                            "dataType": "VARCHAR",
                            "sortable": true,
                            "groupable": false,
                            "filterable": true
                        },
                        {
                            "sourceColumn": "department_name",
                            "displayName": "departmentName",
                            "dataType": "VARCHAR",
                            "sortable": true,
                            "groupable": true,
                            "filterable": true
                        }
                    ]
                }]
            }
            """;
    }

    private String createSalesReportJson() {
        return """
            {
                "reportId": "report-002",
                "reportName": "Sales Report",
                "reportDescription": "Sales data analysis",
                "datasets": [{
                    "datasource": "usp_GetSales",
                    "datasourceId": "ds-001",
                    "datasourceType": "StoredProc",
                    "datasourceDescription": "Retrieves sales data",
                    "parameters": [],
                    "columns": [{
                        "sourceColumn": "sale_id",
                        "displayName": "saleId",
                        "dataType": "INTEGER",
                        "sortable": true,
                        "groupable": false,
                        "filterable": true
                    }]
                }]
            }
            """;
    }

    private String createDepartmentReportJson() {
        return """
            {
                "reportId": "report-003",
                "reportName": "Department Report",
                "reportDescription": "Department summary",
                "datasets": [{
                    "datasource": "usp_GetDepartments",
                    "datasourceId": "ds-001",
                    "datasourceType": "StoredProc",
                    "datasourceDescription": "Retrieves department data",
                    "parameters": [],
                    "columns": [{
                        "sourceColumn": "department_id",
                        "displayName": "departmentId",
                        "dataType": "INTEGER",
                        "sortable": true,
                        "groupable": false,
                        "filterable": true
                    }]
                }]
            }
            """;
    }
}
