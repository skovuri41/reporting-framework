package com.reporting.framework.api;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.exception.ReportExecutionException;
import com.reporting.framework.executor.ExecutionResult;
import com.reporting.framework.executor.StoredProcedureExecutor;
import com.reporting.framework.metadata.MetadataLoader;
import com.reporting.framework.metadata.ReportMetadata;
import com.reporting.framework.spark.SparkConfig;
import com.reporting.framework.spark.SparkDatasetConverter;
import com.reporting.framework.spark.SparkReportResult;
import com.reporting.framework.spark.SparkSessionManager;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;

/**
 * Main entry point for executing Spark-based reports.
 * Orchestrates metadata loading, stored procedure execution, and Dataset conversion.
 */
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final MetadataLoader metadataLoader;
    private final StoredProcedureExecutor executor;
    private final SparkSession spark;

    /**
     * Create a ReportService with the given SparkConfig and ConnectionProvider.
     *
     * @param sparkConfig Spark session configuration
     * @param connectionProvider The connection provider for database access
     */
    public ReportService(SparkConfig sparkConfig, ConnectionProvider connectionProvider) {
        this(sparkConfig, connectionProvider, true);
    }

    /**
     * Create a ReportService with optional metadata caching.
     *
     * @param sparkConfig Spark session configuration
     * @param connectionProvider The connection provider for database access
     * @param enableMetadataCache Whether to enable metadata caching
     */
    public ReportService(SparkConfig sparkConfig, ConnectionProvider connectionProvider, boolean enableMetadataCache) {
        if (sparkConfig == null) {
            throw new IllegalArgumentException("SparkConfig cannot be null");
        }
        if (connectionProvider == null) {
            throw new IllegalArgumentException("ConnectionProvider cannot be null");
        }

        this.metadataLoader = new MetadataLoader(connectionProvider, enableMetadataCache);
        this.executor = new StoredProcedureExecutor(connectionProvider);
        this.spark = SparkSessionManager.getInstance(sparkConfig);

        logger.info("ReportService initialized with Spark (appName={}, master={}) and metadata cache: {}",
                   sparkConfig.getAppName(), sparkConfig.getMaster(), enableMetadataCache);
    }

    /**
     * Execute a report with Map-based input parameters.
     *
     * @param reportName The name of the report to execute
     * @param inputParameters Map of input parameter names to values
     * @return SparkReportResult containing the Dataset and output parameters
     */
    public SparkReportResult execute(String reportName, Map<String, Object> inputParameters) {
        logger.info("Executing report: {} with {} input parameters",
                reportName, inputParameters != null ? inputParameters.size() : 0);

        // Load metadata
        ReportMetadata metadata = metadataLoader.getMetadata(reportName);
        logger.debug("Loaded metadata for report: {}", reportName);

        // Validate and execute
        return executeReport(metadata, inputParameters);
    }

    /**
     * Execute the report with the given metadata and parameters.
     */
    private SparkReportResult executeReport(ReportMetadata metadata, Map<String, Object> inputParameters) {
        String reportName = metadata.getReportName();
        Connection connection = null;
        CallableStatement statement = null;
        ResultSet resultSet = null;

        try {
            // Execute stored procedure
            ExecutionResult executionResult = executor.execute(metadata, inputParameters);

            resultSet = executionResult.getResultSet();
            Map<String, Object> outputParameters = executionResult.getOutputParameters();

            Dataset<Row> dataset;

            if (resultSet != null) {
                // Convert ResultSet to Spark Dataset (auto-infer schema)
                dataset = SparkDatasetConverter.convert(resultSet, spark);
                resultSet = null; // Set to null as converter closes it

                logger.info("Report {} executed successfully with {} rows",
                        reportName, dataset.count());
            } else {
                // No result set (stored procedure may only have output parameters)
                // Create empty Dataset with no schema
                dataset = spark.emptyDataFrame();
                logger.info("Report {} executed successfully with no result set", reportName);
            }

            // Create and return SparkReportResult
            return new SparkReportResult(dataset, outputParameters, reportName);

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

    /**
     * Get the SparkSession used by this service.
     * Useful for custom Spark operations.
     *
     * @return SparkSession instance
     */
    public SparkSession getSparkSession() {
        return spark;
    }

    /**
     * Shutdown the ReportService and close SparkSession.
     */
    public void shutdown() {
        logger.info("Shutting down ReportService");
        SparkSessionManager.close();
    }
}
