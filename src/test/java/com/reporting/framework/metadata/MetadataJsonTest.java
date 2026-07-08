package com.reporting.framework.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reporting.framework.metadata.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for JSON serialization and deserialization of metadata DTOs.
 * Validates that Jackson correctly converts between JSON and our immutable DTOs.
 */
class MetadataJsonTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void testParameterDirectionSerialization() throws IOException {
        // Given
        ParameterDirection input = ParameterDirection.INPUT;
        ParameterDirection output = ParameterDirection.OUTPUT;

        // When
        String inputJson = objectMapper.writeValueAsString(input);
        String outputJson = objectMapper.writeValueAsString(output);

        // Then
        assertThat(inputJson).isEqualTo("\"Input\"");
        assertThat(outputJson).isEqualTo("\"Output\"");
    }

    @Test
    void testParameterDirectionDeserialization() throws IOException {
        // Given
        String inputJson = "\"Input\"";
        String outputJson = "\"Output\"";

        // When
        ParameterDirection input = objectMapper.readValue(inputJson, ParameterDirection.class);
        ParameterDirection output = objectMapper.readValue(outputJson, ParameterDirection.class);

        // Then
        assertThat(input).isEqualTo(ParameterDirection.INPUT);
        assertThat(output).isEqualTo(ParameterDirection.OUTPUT);
    }

    @Test
    void testParameterSerialization() throws IOException {
        // Given
        Parameter parameter = new Parameter(
            "departmentId",
            "INTEGER",
            ParameterDirection.INPUT,
            true
        );

        // When
        String json = objectMapper.writeValueAsString(parameter);

        // Then
        assertThat(json).contains("\"parameterName\":\"departmentId\"");
        assertThat(json).contains("\"dataType\":\"INTEGER\"");
        assertThat(json).contains("\"parameterDirection\":\"Input\"");
        assertThat(json).contains("\"nullable\":true");
    }

    @Test
    void testParameterDeserialization() throws IOException {
        // Given
        String json = """
            {
                "parameterName": "departmentId",
                "dataType": "INTEGER",
                "parameterDirection": "Input",
                "nullable": true
            }
            """;

        // When
        Parameter parameter = objectMapper.readValue(json, Parameter.class);

        // Then
        assertThat(parameter.getParameterName()).isEqualTo("departmentId");
        assertThat(parameter.getDataType()).isEqualTo("INTEGER");
        assertThat(parameter.getParameterDirection()).isEqualTo(ParameterDirection.INPUT);
        assertThat(parameter.getNullable()).isTrue();
    }

    @Test
    void testColumnDeserialization() throws IOException {
        // Given
        String json = """
            {
                "sourceColumn": "employee_id",
                "displayName": "employeeId",
                "dataType": "INTEGER",
                "sortable": true,
                "groupable": false,
                "filterable": true
            }
            """;

        // When
        Column column = objectMapper.readValue(json, Column.class);

        // Then
        assertThat(column.getSourceColumn()).isEqualTo("employee_id");
        assertThat(column.getDisplayName()).isEqualTo("employeeId");
        assertThat(column.getDataType()).isEqualTo("INTEGER");
        assertThat(column.getSortable()).isTrue();
        assertThat(column.getGroupable()).isFalse();
        assertThat(column.getFilterable()).isTrue();
    }

    @Test
    void testDatasetDeserialization() throws IOException {
        // Given
        String json = """
            {
                "datasource": "usp_GetEmployees",
                "datasourceId": "ds-001",
                "datasourceType": "StoredProc",
                "datasourceDescription": "Employee data",
                "parameters": [
                    {
                        "parameterName": "departmentId",
                        "dataType": "INTEGER",
                        "parameterDirection": "Input",
                        "nullable": true
                    }
                ],
                "columns": [
                    {
                        "sourceColumn": "employee_id",
                        "displayName": "employeeId",
                        "dataType": "INTEGER",
                        "sortable": true,
                        "groupable": false,
                        "filterable": true
                    }
                ]
            }
            """;

        // When
        Dataset dataset = objectMapper.readValue(json, Dataset.class);

        // Then
        assertThat(dataset.getDatasource()).isEqualTo("usp_GetEmployees");
        assertThat(dataset.getDatasourceId()).isEqualTo("ds-001");
        assertThat(dataset.getDatasourceType()).isEqualTo("StoredProc");
        assertThat(dataset.getParameters()).hasSize(1);
        assertThat(dataset.getColumns()).hasSize(1);
    }

    @Test
    void testReportMetadataDeserialization() throws IOException {
        // Given - load from sample file
        String json = Files.readString(
            Paths.get("src/test/resources/sample-metadata/single-report.json")
        );

        // When
        ReportMetadata metadata = objectMapper.readValue(json, ReportMetadata.class);

        // Then
        assertThat(metadata.getReportId()).isEqualTo("employee-analysis-001");
        assertThat(metadata.getReportName()).isEqualTo("Employee Analysis Report");
        assertThat(metadata.getDatasets()).hasSize(2);

        // Verify first dataset
        Dataset dataset1 = metadata.getDatasets().get(0);
        assertThat(dataset1.getDatasource()).isEqualTo("usp_GetEmployees");
        assertThat(dataset1.getParameters()).hasSize(2);
        assertThat(dataset1.getColumns()).hasSize(5);
    }

    @Test
    void testReportCatalogDeserialization() throws IOException {
        // Given - load from sample file
        String json = Files.readString(
            Paths.get("src/test/resources/sample-metadata/catalog.json")
        );

        // When
        ReportCatalog catalog = objectMapper.readValue(json, ReportCatalog.class);

        // Then
        assertThat(catalog.getCount()).isEqualTo(3);
        assertThat(catalog.getReports()).hasSize(3);
        assertThat(catalog.getReports().get(0).getReportId()).isEqualTo("employee-analysis-001");
    }

    @Test
    void testImmutabilityOfReportMetadata() throws IOException {
        // Given
        String json = Files.readString(
            Paths.get("src/test/resources/sample-metadata/single-report.json")
        );
        ReportMetadata metadata = objectMapper.readValue(json, ReportMetadata.class);

        // When - try to modify the datasets list
        List<Dataset> datasets = metadata.getDatasets();

        // Then - should throw UnsupportedOperationException
        assertThat(datasets).isUnmodifiable();
    }

    @Test
    void testRoundTripSerialization() throws IOException {
        // Given - original JSON
        String originalJson = Files.readString(
            Paths.get("src/test/resources/sample-metadata/single-report.json")
        );

        // When - deserialize and serialize back
        ReportMetadata metadata = objectMapper.readValue(originalJson, ReportMetadata.class);
        String serializedJson = objectMapper.writeValueAsString(metadata);
        ReportMetadata deserializedAgain = objectMapper.readValue(serializedJson, ReportMetadata.class);

        // Then - should be equivalent
        assertThat(deserializedAgain.getReportId()).isEqualTo(metadata.getReportId());
        assertThat(deserializedAgain.getDatasets()).hasSize(metadata.getDatasets().size());
    }
}
