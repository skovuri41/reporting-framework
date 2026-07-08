package com.reporting.framework.metadata;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.exception.MetadataNotFoundException;
import com.reporting.framework.metadata.model.ReportCatalog;
import com.reporting.framework.metadata.model.ReportMetadata;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MetadataLoader.
 * Tests single report loading, catalog loading, and error handling.
 */
class MetadataLoaderTest {

    private static final String VALID_REPORT_JSON = """
        {
            "reportId": "test-001",
            "reportName": "Test Report",
            "reportDescription": "Test Description",
            "datasets": [{
                "datasource": "usp_Test",
                "datasourceId": "ds-001",
                "datasourceType": "StoredProc",
                "datasourceDescription": "Test dataset",
                "parameters": [{
                    "parameterName": "departmentId",
                    "dataType": "INTEGER",
                    "parameterDirection": "Input",
                    "nullable": true
                }],
                "columns": [{
                    "sourceColumn": "employee_id",
                    "displayName": "employeeId",
                    "dataType": "INTEGER",
                    "sortable": true,
                    "groupable": false,
                    "filterable": true
                }]
            }]
        }
        """;

    @Test
    void testLoadSingleReport() throws Exception {
        // Given
        ConnectionProvider connectionProvider = createMockConnectionProvider(VALID_REPORT_JSON);
        MetadataLoader loader = new MetadataLoader(connectionProvider, false);

        // When
        ReportMetadata metadata = loader.load("test-001");

        // Then
        assertThat(metadata).isNotNull();
        assertThat(metadata.getReportId()).isEqualTo("test-001");
        assertThat(metadata.getReportName()).isEqualTo("Test Report");
        assertThat(metadata.getDatasets()).hasSize(1);
        assertThat(metadata.getDatasets().get(0).getDatasource()).isEqualTo("usp_Test");
    }

    @Test
    void testLoadNonExistentReport() throws Exception {
        // Given - connection returns empty result
        ConnectionProvider connectionProvider = createMockConnectionProviderNoResults();
        MetadataLoader loader = new MetadataLoader(connectionProvider, false);

        // When/Then
        assertThatThrownBy(() -> loader.load("non-existent"))
            .isInstanceOf(MetadataNotFoundException.class)
            .hasMessageContaining("non-existent");
    }

    @Test
    void testLoadWithNullReportId() throws Exception {
        // Given
        ConnectionProvider connectionProvider = mock(ConnectionProvider.class);
        MetadataLoader loader = new MetadataLoader(connectionProvider, false);

        // When/Then
        assertThatThrownBy(() -> loader.load(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reportId cannot be null");
    }

    @Test
    void testLoadWithEmptyReportId() throws Exception {
        // Given
        ConnectionProvider connectionProvider = mock(ConnectionProvider.class);
        MetadataLoader loader = new MetadataLoader(connectionProvider, false);

        // When/Then
        assertThatThrownBy(() -> loader.load(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("reportId cannot be null or empty");
    }

    @Test
    void testLoadCatalog() throws Exception {
        // Given - multiple reports
        String catalogJson = """
            {
                "reportId": "report-001",
                "reportName": "Report 1",
                "reportDescription": "First report",
                "datasets": [{
                    "datasource": "usp_Test1",
                    "datasourceId": "ds-001",
                    "datasourceType": "StoredProc",
                    "datasourceDescription": "Test",
                    "parameters": [],
                    "columns": []
                }]
            }
            """;

        ConnectionProvider connectionProvider = createMockConnectionProviderForCatalog(catalogJson);
        MetadataLoader loader = new MetadataLoader(connectionProvider, false);

        // When
        ReportCatalog catalog = loader.loadCatalog();

        // Then
        assertThat(catalog).isNotNull();
        assertThat(catalog.getReports()).isNotEmpty();
        assertThat(catalog.getCount()).isEqualTo(catalog.getReports().size());
    }

    @Test
    void testCacheSize() throws Exception {
        // Given
        ConnectionProvider connectionProvider = createMockConnectionProviderForCatalog(VALID_REPORT_JSON);
        MetadataLoader loader = new MetadataLoader(connectionProvider, true); // warm cache

        // When
        int cacheSize = loader.getCacheSize();

        // Then
        assertThat(cacheSize).isGreaterThanOrEqualTo(0);
    }

    // Helper methods

    private ConnectionProvider createMockConnectionProvider(String jsonResponse) throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("METADATA_JSON")).thenReturn(jsonResponse);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        ConnectionProvider provider = mock(ConnectionProvider.class);
        when(provider.getConnection()).thenReturn(connection);

        return provider;
    }

    private ConnectionProvider createMockConnectionProviderNoResults() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.next()).thenReturn(false);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        ConnectionProvider provider = mock(ConnectionProvider.class);
        when(provider.getConnection()).thenReturn(connection);

        return provider;
    }

    private ConnectionProvider createMockConnectionProviderForCatalog(String jsonResponse) throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("REPORT_ID")).thenReturn("report-001");
        when(resultSet.getString("METADATA_JSON")).thenReturn(jsonResponse);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        ConnectionProvider provider = mock(ConnectionProvider.class);
        when(provider.getConnection()).thenReturn(connection);

        return provider;
    }
}
