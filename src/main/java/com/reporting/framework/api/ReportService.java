package com.reporting.framework.api;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.data.DataRow;
import com.reporting.framework.data.DataSet;
import com.reporting.framework.exception.ReportExecutionException;
import com.reporting.framework.executor.ExecutionResult;
import com.reporting.framework.executor.StoredProcedureExecutor;
import com.reporting.framework.mapper.NamingStrategy;
import com.reporting.framework.mapper.ResultSetToMapConverter;
import com.reporting.framework.metadata.MetadataLoader;
import com.reporting.framework.metadata.model.Dataset;
import com.reporting.framework.metadata.model.ReportCatalog;
import com.reporting.framework.metadata.model.ReportMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Main entry point for executing reports dynamically.
 * <p>
 * Returns DataSet results that support fluent transformations (filter, join, groupBy, etc.)
 * without requiring POJO classes. Columns are auto-inferred from ResultSet metadata.
 * </p>
 * <p>
 * This fully dynamic approach provides maximum flexibility for data transformation
 * and JSON output generation.
 * </p>
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

        logger.info("ReportService initialized with metadata cache: {}", enableMetadataCache);
    }

    /**
     * Execute a report by its ID with input parameters.
     * <p>
     * For reports with multiple datasets, executes the first dataset only.
     * </p>
     *
     * @param reportId The ID of the report to execute
     * @param inputParameters The input parameter values (name → value)
     * @return DataSet containing rows as DataRows and output parameters
     */
    public DataSet execute(String reportId, Map<String, Object> inputParameters) {
        logger.info("Executing report: {} with {} input parameters",
                reportId, inputParameters != null ? inputParameters.size() : 0);

        // Load report metadata
        ReportMetadata metadata = metadataLoader.load(reportId);
        logger.debug("Loaded metadata for report: {}", reportId);

        // Get first dataset (single-dataset reports for now)
        if (metadata.getDatasets().isEmpty()) {
            throw new ReportExecutionException(
                "Report '" + reportId + "' has no datasets defined in metadata"
            );
        }

        Dataset dataset = metadata.getDatasets().get(0);
        if (metadata.getDatasets().size() > 1) {
            logger.warn("Report '{}' has {} datasets. Executing first dataset only: {}",
                reportId, metadata.getDatasets().size(), dataset.getDatasourceId());
        }

        // Execute the dataset
        return executeDataset(reportId, dataset, inputParameters);
    }

    /**
     * Execute a report with no input parameters.
     *
     * @param reportId The ID of the report to execute
     * @return DataSet containing rows as DataRows and output parameters
     */
    public DataSet execute(String reportId) {
        return execute(reportId, Collections.emptyMap());
    }

    /**
     * Execute all datasets in a report and return organized results.
     * <p>
     * This method executes all datasets defined in the report metadata sequentially
     * and returns the results as a Map keyed by datasourceId. The execution order
     * matches the order datasets appear in the metadata, and this order is preserved
     * in the returned Map.
     * </p>
     * <p>
     * All datasets share the same parameter map. Each dataset extracts only the
     * parameters it needs based on its metadata definition. Extra parameters in
     * the map are ignored. Required parameters must be present or execution will
     * fail.
     * </p>
     * <p>
     * The method uses fail-fast semantics: if any dataset execution fails, the
     * method stops immediately and throws an exception. Datasets that have not
     * yet executed will be skipped. No partial results are returned.
     * </p>
     * <p>
     * Example usage:
     * <pre>{@code
     * Map<String, Object> params = Map.of(
     *     "departmentId", 10,
     *     "startDate", LocalDate.of(2024, 1, 1),
     *     "endDate", LocalDate.of(2024, 12, 31)
     * );
     *
     * Map<String, DataSet> results = reportService.executeAllDatasets("employee-analysis", params);
     *
     * // Access individual results
     * DataSet employees = results.get("ds-employees");
     * DataSet departments = results.get("ds-departments");
     * DataSet metrics = results.get("ds-metrics");
     *
     * // Iterate in execution order
     * results.forEach((datasourceId, dataSet) -> {
     *     System.out.println("Dataset: " + datasourceId + ", Rows: " + dataSet.count());
     * });
     * }</pre>
     * </p>
     *
     * @param reportId The ID of the report to execute
     * @param parameters Shared parameter map containing all parameters for all datasets.
     *                   Each dataset extracts only the parameters it needs.
     * @return Map of datasourceId → DataSet results, preserving execution order.
     *         Returns empty Map if report has zero datasets.
     * @throws ReportExecutionException if any dataset execution fails. The exception
     *         message includes the datasourceId of the failing dataset and the reportId.
     * @throws com.reporting.framework.exception.MetadataNotFoundException if reportId
     *         is not found in the metadata repository
     * @throws NullPointerException if reportId or parameters is null
     */
    public Map<String, DataSet> executeAllDatasets(String reportId,
                                                     Map<String, Object> parameters) {
        logger.info("Executing all datasets for report: {} with {} input parameters",
                reportId, parameters != null ? parameters.size() : 0);

        // Load report metadata
        ReportMetadata metadata = metadataLoader.load(reportId);
        logger.debug("Loaded metadata for report: {} with {} datasets",
                reportId, metadata.getDatasets().size());

        // Create result map (LinkedHashMap preserves insertion order)
        Map<String, DataSet> results = new LinkedHashMap<>();

        // Execute each dataset sequentially
        for (Dataset dataset : metadata.getDatasets()) {
            String datasourceId = dataset.getDatasourceId();

            try {
                logger.debug("Executing dataset '{}' ({} of {}) for report '{}'",
                        datasourceId,
                        results.size() + 1,
                        metadata.getDatasets().size(),
                        reportId);

                DataSet result = executeDataset(reportId, dataset, parameters);
                results.put(datasourceId, result);

                logger.debug("Dataset '{}' executed successfully: {} rows",
                        datasourceId, result.count());

            } catch (Exception e) {
                logger.error("Failed to execute dataset '{}' for report '{}' (fail-fast)",
                        datasourceId, reportId, e);

                throw new ReportExecutionException(
                        "Failed to execute dataset '" + datasourceId +
                        "' in report '" + reportId + "': " + e.getMessage(),
                        e
                );
            }
        }

        logger.info("Successfully executed all {} datasets for report '{}'",
                results.size(), reportId);

        return results;
    }

    /**
     * Get metadata for a report.
     *
     * @param reportId The report ID
     * @return The report metadata
     */
    public ReportMetadata getMetadata(String reportId) {
        return metadataLoader.load(reportId);
    }

    /**
     * Get catalog of all available reports.
     *
     * @return The report catalog
     */
    public ReportCatalog getCatalog() {
        return metadataLoader.loadCatalog();
    }

    /**
     * Refresh the metadata cache.
     */
    public void refreshMetadataCache() {
        logger.info("Refreshing metadata cache");
        metadataLoader.refreshCache();
    }

    /**
     * Get current metadata cache size.
     *
     * @return number of cached metadata entries
     */
    public int getMetadataCacheSize() {
        return metadataLoader.getCacheSize();
    }

    /**
     * Execute a single dataset from a report.
     */
    private DataSet executeDataset(String reportId, Dataset dataset,
                                   Map<String, Object> inputParameters) {
        String datasource = dataset.getDatasource();
        logger.info("Executing dataset '{}' (datasource: {}) for report '{}'",
            dataset.getDatasourceId(), datasource, reportId);

        try {
            // Execute stored procedure
            ExecutionResult result = executor.execute(dataset, inputParameters);

            // Convert ResultSet to List<Map>
            List<Map<String, Object>> rows = resultSetConverter.convertToMaps(
                result.getResultSet(),
                NamingStrategy.CAMEL_CASE
            );

            // Convert to DataRows
            List<DataRow> dataRows = rows.stream()
                .map(DataRow::of)
                .collect(Collectors.toList());

            logger.info("Executed dataset '{}' successfully: {} rows, {} output parameters",
                dataset.getDatasourceId(), dataRows.size(), result.getOutputParameters().size());

            // Create DataSet
            return new DataSet(dataRows, result.getOutputParameters(), reportId);

        } catch (Exception e) {
            logger.error("Failed to execute dataset '{}' for report '{}'",
                dataset.getDatasourceId(), reportId, e);
            throw new ReportExecutionException(
                "Failed to execute dataset '" + dataset.getDatasourceId() +
                "' for report '" + reportId + "': " + e.getMessage(),
                e
            );
        }
    }
}
