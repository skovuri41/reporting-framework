package com.reporting.framework.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Result container for Spark-based report execution.
 * Contains the Dataset and output parameters from stored procedure.
 */
public class SparkReportResult {

    private final Dataset<Row> dataset;
    private final Map<String, Object> outputParameters;
    private final String reportName;
    private final Instant executionTime;

    public SparkReportResult(Dataset<Row> dataset, Map<String, Object> outputParameters, String reportName) {
        this.dataset = dataset;
        this.outputParameters = new HashMap<>(outputParameters);
        this.reportName = reportName;
        this.executionTime = Instant.now();
    }

    /**
     * Get the Spark Dataset for custom transformations.
     *
     * @return Dataset of Rows
     */
    public Dataset<Row> getDataset() {
        return dataset;
    }

    /**
     * Get output parameters from stored procedure.
     *
     * @return Map of parameter names to values
     */
    public Map<String, Object> getOutputParameters() {
        return new HashMap<>(outputParameters);
    }

    /**
     * Get report name.
     *
     * @return Report name
     */
    public String getReportName() {
        return reportName;
    }

    /**
     * Get execution timestamp.
     *
     * @return Execution time
     */
    public Instant getExecutionTime() {
        return executionTime;
    }

    /**
     * Serialize entire dataset to JSON string.
     *
     * @return JSON representation of all rows
     */
    public String toJSON() {
        List<String> jsonRows = dataset.toJSON().collectAsList();
        return "[" + String.join(",", jsonRows) + "]";
    }

    /**
     * Serialize limited number of rows to JSON.
     *
     * @param limit Maximum number of rows to include
     * @return JSON representation of limited rows
     */
    public String toJSON(int limit) {
        List<String> jsonRows = dataset.limit(limit).toJSON().collectAsList();
        return "[" + String.join(",", jsonRows) + "]";
    }

    /**
     * Get list of JSON strings, one per row.
     *
     * @return List of JSON row representations
     */
    public List<String> toJSONList() {
        return dataset.toJSON().collectAsList();
    }

    /**
     * Get row count.
     *
     * @return Number of rows in dataset
     */
    public long count() {
        return dataset.count();
    }

    /**
     * Show dataset in console (for debugging).
     *
     * @param numRows Number of rows to show
     */
    public void show(int numRows) {
        dataset.show(numRows);
    }

    /**
     * Show first 20 rows in console (for debugging).
     */
    public void show() {
        dataset.show();
    }
}
