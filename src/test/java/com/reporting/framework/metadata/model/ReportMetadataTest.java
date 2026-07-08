package com.reporting.framework.metadata.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ReportMetadata dataset lookup functionality.
 */
class ReportMetadataTest {

    private ReportMetadata metadata;
    private Dataset dataset1;
    private Dataset dataset2;
    private Dataset dataset3;

    @BeforeEach
    void setUp() {
        // Create test datasets with different datasourceIds
        dataset1 = new Dataset(
            "usp_GetEmployees",
            "ds-employees",
            "StoredProc",
            "Employee dataset",
            Collections.emptyList(),
            Collections.emptyList()
        );

        dataset2 = new Dataset(
            "usp_GetDepartments",
            "ds-departments",
            "StoredProc",
            "Department dataset",
            Collections.emptyList(),
            Collections.emptyList()
        );

        dataset3 = new Dataset(
            "usp_GetMetrics",
            "ds-metrics",
            "StoredProc",
            "Metrics dataset",
            Collections.emptyList(),
            Collections.emptyList()
        );

        // Create report metadata with 3 datasets
        metadata = new ReportMetadata(
            "test-report",
            "Test Report",
            "Test report with multiple datasets",
            Arrays.asList(dataset1, dataset2, dataset3)
        );
    }

    @Test
    void testGetDatasetById_ValidId() {
        // Test with first dataset
        Dataset result = metadata.getDatasetById("ds-employees");

        assertThat(result).isNotNull();
        assertThat(result.getDatasourceId()).isEqualTo("ds-employees");
        assertThat(result.getDatasource()).isEqualTo("usp_GetEmployees");

        // Test with second dataset
        result = metadata.getDatasetById("ds-departments");

        assertThat(result).isNotNull();
        assertThat(result.getDatasourceId()).isEqualTo("ds-departments");
        assertThat(result.getDatasource()).isEqualTo("usp_GetDepartments");

        // Test with third dataset
        result = metadata.getDatasetById("ds-metrics");

        assertThat(result).isNotNull();
        assertThat(result.getDatasourceId()).isEqualTo("ds-metrics");
        assertThat(result.getDatasource()).isEqualTo("usp_GetMetrics");
    }

    @Test
    void testGetDatasetById_InvalidId() {
        // Test with non-existent datasourceId
        assertThatThrownBy(() -> metadata.getDatasetById("ds-invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Dataset not found: ds-invalid");
    }

    @Test
    void testGetDatasetById_ErrorMessageIncludesAvailableIds() {
        // Verify error message includes both requested ID and available IDs
        assertThatThrownBy(() -> metadata.getDatasetById("ds-nonexistent"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Dataset not found: ds-nonexistent")
            .hasMessageContaining("Available datasets:")
            .hasMessageContaining("ds-employees")
            .hasMessageContaining("ds-departments")
            .hasMessageContaining("ds-metrics");
    }

    @Test
    void testGetDatasetById_CaseSensitive() {
        // Verify lookup is case-sensitive (FR-004)
        assertThatThrownBy(() -> metadata.getDatasetById("DS-EMPLOYEES"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Dataset not found: DS-EMPLOYEES");

        assertThatThrownBy(() -> metadata.getDatasetById("ds-Employees"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Dataset not found: ds-Employees");
    }

    @Test
    void testGetDatasetById_EmptyDatasets() {
        // Create metadata with no datasets
        ReportMetadata emptyMetadata = new ReportMetadata(
            "empty-report",
            "Empty Report",
            null,
            Collections.emptyList()
        );

        // Should throw exception with empty available list
        assertThatThrownBy(() -> emptyMetadata.getDatasetById("ds-any"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Dataset not found: ds-any")
            .hasMessageContaining("Available datasets:");
    }

    @Test
    void testGetDatasetById_NullDatasourceId() {
        // Verify null datasourceId throws NullPointerException
        assertThatThrownBy(() -> metadata.getDatasetById(null))
            .isInstanceOf(NullPointerException.class);
    }
}
