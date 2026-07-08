package com.reporting.framework.metadata;

import com.reporting.framework.exception.MetadataValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for JSON Schema validation of metadata.
 * Validates that the JsonSchemaValidator correctly enforces the schema rules.
 */
class JsonSchemaValidationTest {

    private JsonSchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new JsonSchemaValidator();
    }

    @Test
    void testValidSingleReportMetadata() throws Exception {
        // Given - valid single report JSON
        String json = Files.readString(
            Paths.get("src/test/resources/sample-metadata/single-report.json")
        );

        // When/Then - should not throw exception
        validator.validate("employee-analysis-001", json);
    }

    @Test
    void testValidCatalog() throws Exception {
        // Given - valid catalog JSON
        String json = Files.readString(
            Paths.get("src/test/resources/sample-metadata/catalog.json")
        );

        // When/Then - should not throw exception
        validator.validate("catalog", json);
    }

    @Test
    void testMissingRequiredField_reportId() {
        // Given - JSON missing reportId
        String json = """
            {
                "reportName": "Test Report",
                "reportDescription": "Test",
                "datasets": []
            }
            """;

        // When/Then - should throw validation exception
        assertThatThrownBy(() -> validator.validate("test", json))
            .isInstanceOf(MetadataValidationException.class)
            .hasMessageContaining("validation");
    }

    @Test
    void testMissingRequiredField_reportName() {
        // Given - JSON missing reportName
        String json = """
            {
                "reportId": "test-001",
                "reportDescription": "Test",
                "datasets": []
            }
            """;

        // When/Then - should throw validation exception
        assertThatThrownBy(() -> validator.validate("test-001", json))
            .isInstanceOf(MetadataValidationException.class);
    }

    @Test
    void testInvalidParameterDirection() {
        // Given - invalid parameter direction
        String json = """
            {
                "reportId": "test-001",
                "reportName": "Test Report",
                "reportDescription": "Test",
                "datasets": [{
                    "datasource": "usp_Test",
                    "datasourceId": "ds-001",
                    "datasourceType": "StoredProc",
                    "datasourceDescription": "Test",
                    "parameters": [{
                        "parameterName": "testParam",
                        "dataType": "INTEGER",
                        "parameterDirection": "InvalidDirection",
                        "nullable": true
                    }],
                    "columns": []
                }]
            }
            """;

        // When/Then - should throw validation exception
        assertThatThrownBy(() -> validator.validate("test-001", json))
            .isInstanceOf(MetadataValidationException.class);
    }

    @Test
    void testInvalidDatasourceType() {
        // Given - invalid datasourceType
        String json = """
            {
                "reportId": "test-001",
                "reportName": "Test Report",
                "reportDescription": "Test",
                "datasets": [{
                    "datasource": "usp_Test",
                    "datasourceId": "ds-001",
                    "datasourceType": "InvalidType",
                    "datasourceDescription": "Test",
                    "parameters": [],
                    "columns": []
                }]
            }
            """;

        // When/Then - should throw validation exception
        assertThatThrownBy(() -> validator.validate("test-001", json))
            .isInstanceOf(MetadataValidationException.class);
    }

    @Test
    void testInvalidDataType() {
        // Given - invalid SQL data type
        String json = """
            {
                "reportId": "test-001",
                "reportName": "Test Report",
                "reportDescription": "Test",
                "datasets": [{
                    "datasource": "usp_Test",
                    "datasourceId": "ds-001",
                    "datasourceType": "StoredProc",
                    "datasourceDescription": "Test",
                    "parameters": [{
                        "parameterName": "testParam",
                        "dataType": "UNSUPPORTED_TYPE",
                        "parameterDirection": "Input",
                        "nullable": true
                    }],
                    "columns": []
                }]
            }
            """;

        // When/Then - should throw validation exception
        assertThatThrownBy(() -> validator.validate("test-001", json))
            .isInstanceOf(MetadataValidationException.class);
    }

    @Test
    void testMissingColumnRequiredFields() {
        // Given - column missing required fields
        String json = """
            {
                "reportId": "test-001",
                "reportName": "Test Report",
                "reportDescription": "Test",
                "datasets": [{
                    "datasource": "usp_Test",
                    "datasourceId": "ds-001",
                    "datasourceType": "StoredProc",
                    "datasourceDescription": "Test",
                    "parameters": [],
                    "columns": [{
                        "sourceColumn": "test_column",
                        "displayName": "testColumn"
                    }]
                }]
            }
            """;

        // When/Then - should throw validation exception (missing dataType, sortable, etc.)
        assertThatThrownBy(() -> validator.validate("test-001", json))
            .isInstanceOf(MetadataValidationException.class);
    }

    @Test
    void testInvalidJson() {
        // Given - malformed JSON
        String json = "{invalid json}";

        // When/Then - should throw validation exception
        assertThatThrownBy(() -> validator.validate("test-001", json))
            .isInstanceOf(MetadataValidationException.class);
    }

    @Test
    void testEmptyDatasets() {
        // Given - valid JSON with empty datasets (allowed by schema)
        String json = """
            {
                "reportId": "test-001",
                "reportName": "Test Report",
                "reportDescription": "Test",
                "datasets": []
            }
            """;

        // When/Then - should throw because schema requires minItems: 1 for datasets
        assertThatThrownBy(() -> validator.validate("test-001", json))
            .isInstanceOf(MetadataValidationException.class);
    }

    @Test
    void testValidationErrorContainsDetails() {
        // Given - invalid JSON
        String json = """
            {
                "reportId": "test-001",
                "reportName": "Test Report"
            }
            """;

        // When/Then - exception should contain validation details
        assertThatThrownBy(() -> validator.validate("test-001", json))
            .isInstanceOf(MetadataValidationException.class)
            .satisfies(e -> {
                MetadataValidationException ex = (MetadataValidationException) e;
                assertThat(ex.getValidationErrors()).isNotEmpty();
            });
    }
}
