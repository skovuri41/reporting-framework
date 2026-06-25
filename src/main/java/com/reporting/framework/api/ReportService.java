package com.reporting.framework.api;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.data.DataRow;
import com.reporting.framework.data.DataSet;
import com.reporting.framework.exception.ReportExecutionException;
import com.reporting.framework.executor.ExecutionResult;
import com.reporting.framework.executor.StoredProcedureExecutor;
import com.reporting.framework.mapper.ResultSetToMapConverter;
import com.reporting.framework.metadata.MetadataLoader;
import com.reporting.framework.metadata.ReportMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main entry point for executing reports dynamically.
 *
 * Returns DataSet results that support fluent transformations (filter, join, groupBy, etc.)
 * without requiring POJO classes. Columns are auto-inferred from ResultSet metadata.
 *
 * This fully dynamic approach provides maximum flexibility for data transformation
 * and JSON output generation.
 */
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final MetadataLoader metadataLoader;
    private final StoredProcedureExecutor executor;
    private final ResultSetToMapConverter resultSetConverter;

    /**
     * Create a ReportService with the given ConnectionProvider.
     * Metadata caching is enabled by default.
     *
     * @param connectionProvider The connection provider for database access
     */
    public ReportService(ConnectionProvider connectionProvider) {
        this(connectionProvider, true);
    }

    /**
     * Create a ReportService with optional metadata caching.
     *
     * @param connectionProvider The connection provider for database access
     * @param enableMetadataCache Whether to enable metadata caching
     */
    public ReportService(ConnectionProvider connectionProvider, boolean enableMetadataCache) {
        if (connectionProvider == null) {
            throw new IllegalArgumentException("ConnectionProvider cannot be null");
        }

        this.metadataLoader = new MetadataLoader(connectionProvider, enableMetadataCache);
        this.executor = new StoredProcedureExecutor(connectionProvider);
        this.resultSetConverter = new ResultSetToMapConverter();

        logger.info("ReportService initialized (fully dynamic) with metadata cache: {}", enableMetadataCache);
    }

    /**
     * Execute a report and return a DataSet with dynamic schema.
     *
     * Columns are auto-inferred from ResultSet metadata - no POJO classes needed.
     * The returned DataSet supports fluent transformations: filter, join, groupBy,
     * select, orderBy, and JSON output generation.
     *
     * @param reportName The name of the report to execute
     * @param inputParameters Map of input parameter names to values (can be null)
     * @return DataSet containing rows as DataRows and output parameters
     */
    public DataSet execute(String reportName, Map<String, Object> inputParameters) {
        logger.info("Executing report: {} with {} input parameters",
                reportName, inputParameters != null ? inputParameters.size() : 0);

        // Load metadata
        ReportMetadata metadata = metadataLoader.getMetadata(reportName);
        logger.debug("Loaded metadata for report: {}", reportName);

        // Execute report
        return executeReport(metadata, inputParameters);
    }

    /**
     * Execute a report with no input parameters.
     *
     * @param reportName The name of the report to execute
     * @return DataSet containing rows as DataRows and output parameters
     */
    public DataSet execute(String reportName) {
        return execute(reportName, Collections.emptyMap());
    }

    /**
     * Execute the report with the given metadata and parameters.
     */
    private DataSet executeReport(ReportMetadata metadata,
                                  Map<String, Object> inputParameters) {
        String reportName = metadata.getReportName();
        Connection connection = null;
        CallableStatement statement = null;
        ResultSet resultSet = null;

        try {
            // Execute stored procedure
            ExecutionResult executionResult = executor.execute(metadata, inputParameters);

            resultSet = executionResult.getResultSet();
            Map<String, Object> outputParameters = executionResult.getOutputParameters();

            List<DataRow> rows;

            if (resultSet != null) {
                // Convert ResultSet to Maps using auto-inferred columns
                List<Map<String, Object>> maps = resultSetConverter.convertToMaps(resultSet);

                // Convert Maps to DataRows
                rows = maps.stream()
                        .map(DataRow::of)
                        .collect(Collectors.toList());

                logger.info("Report {} executed successfully with {} results",
                        reportName, rows.size());
            } else {
                // No result set (stored procedure may only have output parameters)
                rows = Collections.emptyList();
                logger.info("Report {} executed successfully with no result set", reportName);
            }

            // Create and return DataSet
            return new DataSet(rows, outputParameters, reportName);

        } catch (Exception e) {
            logger.error("Failed to execute report: {}", reportName, e);
            if (e instanceof ReportExecutionException) {
                throw (ReportExecutionException) e;
            }
            throw new ReportExecutionException(reportName, metadata.getStoredProcedure(),
                    "Report execution failed: " + e.getMessage(), e);

        } finally {
            // Clean up resources
            if (resultSet != null) {
                try {
                    statement = (CallableStatement) resultSet.getStatement();
                    connection = statement.getConnection();
                } catch (Exception e) {
                    logger.warn("Error getting connection/statement from ResultSet", e);
                }
            }
            executor.closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Invalidate the metadata cache for a specific report.
     */
    public void invalidateMetadataCache(String reportName) {
        metadataLoader.invalidateCache(reportName);
        logger.info("Invalidated metadata cache for report: {}", reportName);
    }

    /**
     * Clear the entire metadata cache.
     */
    public void clearMetadataCache() {
        metadataLoader.clearCache();
        logger.info("Cleared entire metadata cache");
    }

    /**
     * Get the current metadata cache size.
     */
    public int getMetadataCacheSize() {
        return metadataLoader.getCacheSize();
    }
}
