package com.reporting.framework.metadata;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.connection.SimpleConnectionProvider;
import com.reporting.framework.metadata.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive end-to-end test validating the complete metadata flow:
 * database → load → JSON Schema validation → naming transformation → cache → retrieve
 *
 * This test validates Phase 1-4 integration and verifies all requirements:
 * - JSON Schema validation (T051)
 * - Naming transformation (T052)
 * - Performance targets (T053)
 * - Immutability (T054)
 */
class EndToEndMetadataTest {

    private ConnectionProvider connectionProvider;
    private MetadataLoader metadataLoader;
    private JsonSchemaValidator schemaValidator;

    @BeforeEach
    void setUp() throws Exception {
        connectionProvider = new SimpleConnectionProvider(
            "jdbc:h2:mem:e2etest;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
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

        schemaValidator = new JsonSchemaValidator();
        metadataLoader = new MetadataLoader(connectionProvider, true);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS REPORT_METADATA");
        }
    }

    @Test
    void testCompleteMetadataFlow_DatabaseToCache() throws Exception {
        // Given - insert metadata with various naming conventions in database
        String reportJson = createTestMetadataWithVariousNamingConventions();
        insertReport("e2e-test-001", reportJson);

        // When - load metadata (triggers: validation → deserialization → naming transform → cache)
        long startTime = System.currentTimeMillis();
        ReportMetadata metadata = metadataLoader.load("e2e-test-001");
        long loadTime = System.currentTimeMillis() - startTime;

        // Then - verify complete flow
        // 1. Metadata loaded successfully
        assertThat(metadata).isNotNull();
        assertThat(metadata.getReportId()).isEqualTo("e2e-test-001");

        // 2. Performance target met (T053)
        assertThat(loadTime).as("Single report load time").isLessThan(100L);

        // 3. Structure validated
        assertThat(metadata.getDatasets()).hasSize(1);
        Dataset dataset = metadata.getDatasets().get(0);
        assertThat(dataset.getDatasource()).isEqualTo("usp_TestProcedure");
        assertThat(dataset.getParameters()).hasSize(3);
        assertThat(dataset.getColumns()).hasSize(4);

        // 4. Naming transformation applied (T052)
        verifyNamingTransformation(dataset);

        // 5. Immutability verified (T054)
        verifyImmutability(metadata);

        // 6. Cache is populated
        assertThat(metadataLoader.getCacheSize()).isEqualTo(1);

        // 7. Cached retrieval is fast
        startTime = System.currentTimeMillis();
        ReportMetadata cachedMetadata = metadataLoader.load("e2e-test-001");
        long cachedLoadTime = System.currentTimeMillis() - startTime;
        assertThat(cachedLoadTime).as("Cached load time").isLessThan(10L);
        assertThat(cachedMetadata).isSameAs(metadata); // Same instance from cache
    }

    @Test
    void testJsonSchemaValidation_ValidMetadata() throws Exception {
        // Given - valid metadata JSON
        String validJson = createTestMetadataWithVariousNamingConventions();

        // When/Then - validation should pass (T051)
        assertThatCode(() -> schemaValidator.validate("test", validJson))
            .doesNotThrowAnyException();
    }

    @Test
    void testJsonSchemaValidation_InvalidMetadata() {
        // Given - invalid metadata (missing required field)
        String invalidJson = """
            {
                "reportId": "test",
                "datasets": []
            }
            """;

        // When/Then - validation should fail (T051)
        assertThatThrownBy(() -> schemaValidator.validate("test", invalidJson))
            .isInstanceOf(com.reporting.framework.exception.MetadataValidationException.class);
    }

    @Test
    void testNamingTransformation_AllConventions() throws Exception {
        // Given - metadata with various naming conventions (T052)
        insertReport("naming-test", createTestMetadataWithVariousNamingConventions());

        // When
        ReportMetadata metadata = metadataLoader.load("naming-test");
        Dataset dataset = metadata.getDatasets().get(0);

        // Then - verify all naming transformations
        // snake_case → camelCase
        Parameter param1 = findParameter(dataset, "departmentId");
        assertThat(param1).isNotNull();

        // UPPER_SNAKE_CASE → camelCase
        Parameter param2 = findParameter(dataset, "employeeCount");
        assertThat(param2).isNotNull();

        // PascalCase → camelCase
        Parameter param3 = findParameter(dataset, "startDate");
        assertThat(param3).isNotNull();

        // Column display names transformed
        Column col1 = findColumn(dataset, "employeeId");
        assertThat(col1.getDisplayName()).isEqualTo("employeeId");

        Column col2 = findColumn(dataset, "fullName");
        assertThat(col2.getDisplayName()).isEqualTo("fullName");

        // sourceColumn preserved exactly (not transformed)
        assertThat(col1.getSourceColumn()).isEqualTo("employee_id");
        assertThat(col2.getSourceColumn()).isEqualTo("full_name");
    }

    @Test
    void testCatalogPerformance_100Reports() throws Exception {
        // Given - 100 reports in database (T053)
        for (int i = 1; i <= 100; i++) {
            insertReport(String.format("report-%03d", i),
                createMinimalReport(String.format("report-%03d", i)));
        }

        // When - load catalog
        long startTime = System.currentTimeMillis();
        ReportCatalog catalog = metadataLoader.loadCatalog();
        long catalogTime = System.currentTimeMillis() - startTime;

        // Then - performance target met
        assertThat(catalogTime).as("Catalog load time for 100 reports").isLessThan(1000L);
        assertThat(catalog.getCount()).isEqualTo(100);
        assertThat(catalog.getReports()).hasSize(100);
    }

    @Test
    void testImmutability_MetadataCannotBeModified() throws Exception {
        // Given (T054)
        insertReport("immutable-test", createTestMetadataWithVariousNamingConventions());
        ReportMetadata metadata = metadataLoader.load("immutable-test");

        // When/Then - attempting to modify should fail
        assertThatThrownBy(() -> metadata.getDatasets().clear())
            .isInstanceOf(UnsupportedOperationException.class);

        Dataset dataset = metadata.getDatasets().get(0);
        assertThatThrownBy(() -> dataset.getParameters().clear())
            .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> dataset.getColumns().clear())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    // Helper methods

    private void insertReport(String reportId, String metadataJson) throws Exception {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {

            String sql = String.format("""
                INSERT INTO REPORT_METADATA (REPORT_ID, REPORT_NAME, REPORT_DESCRIPTION, METADATA_JSON)
                VALUES ('%s', 'Test Report', 'Test', '%s')
                """, reportId, metadataJson.replace("'", "''"));

            stmt.execute(sql);
        }
    }

    private String createTestMetadataWithVariousNamingConventions() {
        return """
            {
                "reportId": "e2e-test-001",
                "reportName": "End-to-End Test Report",
                "reportDescription": "Tests all naming conventions",
                "datasets": [{
                    "datasource": "usp_TestProcedure",
                    "datasourceId": "ds-001",
                    "datasourceType": "StoredProc",
                    "datasourceDescription": "Test dataset",
                    "parameters": [
                        {
                            "parameterName": "department_id",
                            "dataType": "INTEGER",
                            "parameterDirection": "Input",
                            "nullable": true
                        },
                        {
                            "parameterName": "EMPLOYEE_COUNT",
                            "dataType": "INTEGER",
                            "parameterDirection": "Output",
                            "nullable": false
                        },
                        {
                            "parameterName": "StartDate",
                            "dataType": "DATE",
                            "parameterDirection": "Input",
                            "nullable": true
                        }
                    ],
                    "columns": [
                        {
                            "sourceColumn": "employee_id",
                            "displayName": "employee_id",
                            "dataType": "INTEGER",
                            "sortable": true,
                            "groupable": false,
                            "filterable": true
                        },
                        {
                            "sourceColumn": "full_name",
                            "displayName": "FULL_NAME",
                            "dataType": "VARCHAR",
                            "sortable": true,
                            "groupable": false,
                            "filterable": true
                        },
                        {
                            "sourceColumn": "department_name",
                            "displayName": "DepartmentName",
                            "dataType": "VARCHAR",
                            "sortable": true,
                            "groupable": true,
                            "filterable": true
                        },
                        {
                            "sourceColumn": "hire_date",
                            "displayName": "hireDate",
                            "dataType": "DATE",
                            "sortable": true,
                            "groupable": false,
                            "filterable": true
                        }
                    ]
                }]
            }
            """;
    }

    private String createMinimalReport(String reportId) {
        return String.format("""
            {
                "reportId": "%s",
                "reportName": "Test Report",
                "reportDescription": "Test",
                "datasets": [{
                    "datasource": "usp_Test",
                    "datasourceId": "ds-001",
                    "datasourceType": "StoredProc",
                    "datasourceDescription": "Test",
                    "parameters": [],
                    "columns": [{
                        "sourceColumn": "id",
                        "displayName": "id",
                        "dataType": "INTEGER",
                        "sortable": true,
                        "groupable": false,
                        "filterable": true
                    }]
                }]
            }
            """, reportId);
    }

    private void verifyNamingTransformation(Dataset dataset) {
        // Verify parameter names transformed to camelCase
        assertThat(dataset.getParameters())
            .extracting(Parameter::getParameterName)
            .containsExactly("departmentId", "employeeCount", "startDate");

        // Verify column display names transformed to camelCase
        assertThat(dataset.getColumns())
            .extracting(Column::getDisplayName)
            .containsExactly("employeeId", "fullName", "departmentName", "hireDate");

        // Verify sourceColumn preserved exactly (NOT transformed)
        assertThat(dataset.getColumns())
            .extracting(Column::getSourceColumn)
            .containsExactly("employee_id", "full_name", "department_name", "hire_date");
    }

    private void verifyImmutability(ReportMetadata metadata) {
        assertThat(metadata.getDatasets()).isUnmodifiable();
        Dataset dataset = metadata.getDatasets().get(0);
        assertThat(dataset.getParameters()).isUnmodifiable();
        assertThat(dataset.getColumns()).isUnmodifiable();
    }

    private Parameter findParameter(Dataset dataset, String name) {
        return dataset.getParameters().stream()
            .filter(p -> p.getParameterName().equals(name))
            .findFirst()
            .orElse(null);
    }

    private Column findColumn(Dataset dataset, String displayName) {
        return dataset.getColumns().stream()
            .filter(c -> c.getDisplayName().equals(displayName))
            .findFirst()
            .orElse(null);
    }
}
