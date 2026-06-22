package com.reporting.framework.api;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.data.DataRow;
import com.reporting.framework.data.DataSet;
import com.reporting.framework.exception.ReportExecutionException;
import com.reporting.framework.executor.ExecutionResult;
import com.reporting.framework.executor.StoredProcedureExecutor;
import com.reporting.framework.mapper.JacksonPojoMapper;
import com.reporting.framework.mapper.PojoToParameterConverter;
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
 * Main entry point for executing reports.
 * Orchestrates metadata loading, stored procedure execution, and result mapping.
 */
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final MetadataLoader metadataLoader;
    private final StoredProcedureExecutor executor;
    private final ResultSetToMapConverter resultSetConverter;
    private final JacksonPojoMapper pojoMapper;
    private final PojoToParameterConverter parameterConverter;

    /**
     * Create a ReportService with the given ConnectionProvider.
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
        this.pojoMapper = new JacksonPojoMapper();
        this.parameterConverter = new PojoToParameterConverter();

        logger.info("ReportService initialized with metadata cache: {}", enableMetadataCache);
    }

    /**
     * Execute a report with Map-based input parameters.
     *
     * @param reportName The name of the report to execute
     * @param inputParameters Map of input parameter names to values
     * @param <T> The type of POJO representing each result row
     * @return ReportResult containing the results and output parameters
     */
    public <T> ReportResult<T> execute(String reportName, Map<String, Object> inputParameters) {
        logger.info("Executing report: {} with {} input parameters",
                reportName, inputParameters != null ? inputParameters.size() : 0);

        // Load metadata
        ReportMetadata metadata = metadataLoader.getMetadata(reportName);
        logger.debug("Loaded metadata for report: {}", reportName);

        // Validate and execute
        return executeReport(metadata, inputParameters);
    }

    /**
     * Execute a report with POJO-based input parameters.
     * The POJO will be converted to a Map for parameter binding.
     *
     * @param reportName The name of the report to execute
     * @param inputPojo The input POJO containing parameter values
     * @param <T> The type of POJO representing each result row
     * @return ReportResult containing the results and output parameters
     */
    public <T> ReportResult<T> execute(String reportName, Object inputPojo) {
        logger.info("Executing report: {} with POJO input: {}",
                reportName, inputPojo != null ? inputPojo.getClass().getSimpleName() : "null");

        // Convert POJO to Map
        Map<String, Object> inputParameters = parameterConverter.convert(inputPojo);
        logger.debug("Converted POJO to {} parameters", inputParameters.size());

        return execute(reportName, inputParameters);
    }

    /**
     * Execute the report with the given metadata and parameters.
     */
    @SuppressWarnings("unchecked")
    private <T> ReportResult<T> executeReport(ReportMetadata metadata, Map<String, Object> inputParameters) {
        String reportName = metadata.getReportName();
        Connection connection = null;
        CallableStatement statement = null;
        ResultSet resultSet = null;

        try {
            // Execute stored procedure
            ExecutionResult executionResult = executor.execute(metadata, inputParameters);

            resultSet = executionResult.getResultSet();
            Map<String, Object> outputParameters = executionResult.getOutputParameters();

            List<T> results;

            if (resultSet != null) {
                // Convert ResultSet to Maps using explicit column mappings
                List<Map<String, Object>> maps = resultSetConverter.convert(
                        resultSet,
                        metadata.getResultSetMapping().getColumnMappings()
                );

                // Convert Maps to POJOs
                Class<T> resultClass = (Class<T>) metadata.getResultClassType();
                results = pojoMapper.mapToPojo(maps, resultClass);

                logger.info("Report {} executed successfully with {} results",
                        reportName, results.size());
            } else {
                // No result set (stored procedure may only have output parameters)
                results = List.of();
                logger.info("Report {} executed successfully with no result set", reportName);
            }

            // Create and return ReportResult
            return new ReportResult<>(results, outputParameters, reportName);

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
     * Execute a report dynamically without POJO classes.
     * Returns a DataSet that contains rows as List of DataRows.
     * Columns are auto-inferred from ResultSet - no explicit column mappings needed.
     *
     * This is a lightweight alternative to the typed execute() method when you:
     * - Don't want to create POJO classes for each report
     * - Need dynamic schema handling
     * - Want to apply transformations (filter, join, groupBy) on results
     *
     * @param reportName The name of the report to execute
     * @param inputParameters Map of input parameter names to values (can be null)
     * @return DataSet containing rows as DataRows and output parameters
     */
    public DataSet executeDynamic(String reportName, Map<String, Object> inputParameters) {
        logger.info("Executing report dynamically: {} with {} input parameters",
                reportName, inputParameters != null ? inputParameters.size() : 0);

        // Load metadata
        ReportMetadata metadata = metadataLoader.getMetadata(reportName);
        logger.debug("Loaded metadata for dynamic report: {}", reportName);

        // Execute report dynamically
        return executeDynamicReport(metadata, inputParameters);
    }

    /**
     * Execute a report dynamically with no input parameters.
     *
     * @param reportName The name of the report to execute
     * @return DataSet containing rows as DataRows and output parameters
     */
    public DataSet executeDynamic(String reportName) {
        return executeDynamic(reportName, Collections.emptyMap());
    }

    /**
     * Execute the report dynamically with the given metadata and parameters.
     */
    private DataSet executeDynamicReport(ReportMetadata metadata,
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

                logger.info("Dynamic report {} executed successfully with {} results",
                        reportName, rows.size());
            } else {
                // No result set (stored procedure may only have output parameters)
                rows = Collections.emptyList();
                logger.info("Dynamic report {} executed successfully with no result set", reportName);
            }

            // Create and return DataSet
            return new DataSet(rows, outputParameters, reportName);

        } catch (Exception e) {
            logger.error("Failed to execute dynamic report: {}", reportName, e);
            if (e instanceof ReportExecutionException) {
                throw (ReportExecutionException) e;
            }
            throw new ReportExecutionException(reportName, metadata.getStoredProcedure(),
                    "Dynamic report execution failed: " + e.getMessage(), e);

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
