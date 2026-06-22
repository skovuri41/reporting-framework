package com.reporting.framework.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reporting.framework.exception.ReportExecutionException;
import com.reporting.framework.mapper.ObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Immutable collection of DataRows representing a dataset.
 * Contains the result rows along with metadata (reportName, outputParameters).
 *
 * This is the primary container for report results in the framework.
 * Use DataQuery for transformations and DataOperations for joins/unions.
 */
public class DataSet {

    private static final Logger logger = LoggerFactory.getLogger(DataSet.class);
    private static final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

    private final List<DataRow> rows;
    private final Map<String, Object> outputParameters;
    private final String reportName;

    /**
     * Create a new DataSet.
     *
     * @param rows The result rows
     * @param outputParameters Output parameters from stored procedure
     * @param reportName The name of the report
     */
    public DataSet(List<DataRow> rows,
                   Map<String, Object> outputParameters,
                   String reportName) {
        this.rows = rows != null ? Collections.unmodifiableList(new ArrayList<>(rows)) : Collections.emptyList();
        this.outputParameters = outputParameters != null ? Collections.unmodifiableMap(new HashMap<>(outputParameters)) : Collections.emptyMap();
        this.reportName = reportName;
    }

    /**
     * Get all rows.
     *
     * @return Unmodifiable list of rows
     */
    public List<DataRow> getRows() {
        return rows;
    }

    /**
     * Get output parameters.
     *
     * @return Unmodifiable map of output parameters
     */
    public Map<String, Object> getOutputParameters() {
        return outputParameters;
    }

    /**
     * Get report name.
     *
     * @return The report name
     */
    public String getReportName() {
        return reportName;
    }

    /**
     * Get the number of rows.
     *
     * @return The row count
     */
    public int count() {
        return rows.size();
    }

    /**
     * Check if the dataset is empty.
     *
     * @return true if there are no rows
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * Get a specific row by index.
     *
     * @param index The row index
     * @return The row at the specified index
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    public DataRow getRow(int index) {
        if (index < 0 || index >= rows.size()) {
            throw new IndexOutOfBoundsException(
                    "Row index " + index + " out of bounds for size " + rows.size());
        }
        return rows.get(index);
    }

    /**
     * Get the first row, or null if empty.
     *
     * @return The first row, or null if empty
     */
    public DataRow first() {
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * Get the last row, or null if empty.
     *
     * @return The last row, or null if empty
     */
    public DataRow last() {
        return rows.isEmpty() ? null : rows.get(rows.size() - 1);
    }

    /**
     * Convert all rows to a list of maps.
     *
     * @return List of maps representing the rows
     */
    public List<Map<String, Object>> toMaps() {
        return rows.stream()
                .map(DataRow::toMap)
                .collect(Collectors.toList());
    }

    /**
     * Convert all rows to JSON string.
     *
     * @return JSON representation of the rows
     */
    public String toJSON() {
        try {
            return objectMapper.writeValueAsString(toMaps());
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert rows to JSON", e);
            throw new ReportExecutionException("Failed to convert result to JSON", e);
        }
    }

    /**
     * Convert rows to pretty-printed JSON string.
     *
     * @return Pretty JSON representation of the rows
     */
    public String toPrettyJSON() {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(toMaps());
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert rows to pretty JSON", e);
            throw new ReportExecutionException("Failed to convert result to JSON", e);
        }
    }

    /**
     * Get an output parameter value.
     *
     * @param parameterName The parameter name
     * @return The parameter value, or null if not found
     */
    public Object getOutputParameter(String parameterName) {
        return outputParameters.get(parameterName);
    }

    /**
     * Check if an output parameter exists.
     *
     * @param parameterName The parameter name
     * @return true if the parameter exists
     */
    public boolean hasOutputParameter(String parameterName) {
        return outputParameters.containsKey(parameterName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSet dataSet = (DataSet) o;
        return Objects.equals(rows, dataSet.rows) &&
                Objects.equals(outputParameters, dataSet.outputParameters) &&
                Objects.equals(reportName, dataSet.reportName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rows, outputParameters, reportName);
    }

    @Override
    public String toString() {
        return "DataSet{" +
                "reportName='" + reportName + '\'' +
                ", rows=" + rows.size() +
                ", outputParameters=" + outputParameters.size() +
                '}';
    }

    /**
     * Builder for creating DataSet instances.
     */
    public static class Builder {
        private List<DataRow> rows = new ArrayList<>();
        private Map<String, Object> outputParameters = new HashMap<>();
        private String reportName = "unnamed";

        public Builder rows(List<DataRow> rows) {
            this.rows = rows;
            return this;
        }

        public Builder addRow(DataRow row) {
            this.rows.add(row);
            return this;
        }

        public Builder outputParameters(Map<String, Object> outputParameters) {
            this.outputParameters = outputParameters;
            return this;
        }

        public Builder outputParameter(String name, Object value) {
            this.outputParameters.put(name, value);
            return this;
        }

        public Builder reportName(String reportName) {
            this.reportName = reportName;
            return this;
        }

        public DataSet build() {
            return new DataSet(rows, outputParameters, reportName);
        }
    }

    /**
     * Create a new Builder.
     *
     * @return A new DataSet.Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a DataSet from a list of rows.
     *
     * @param rows The rows
     * @param reportName The report name
     * @return A new DataSet
     */
    public static DataSet of(List<DataRow> rows, String reportName) {
        return new DataSet(rows, Collections.emptyMap(), reportName);
    }

    /**
     * Create a DataSet from a list of maps.
     *
     * @param maps The data as list of maps
     * @param reportName The report name
     * @return A new DataSet
     */
    public static DataSet fromMaps(List<Map<String, Object>> maps, String reportName) {
        List<DataRow> rows = maps.stream()
                .map(DataRow::of)
                .collect(Collectors.toList());
        return new DataSet(rows, Collections.emptyMap(), reportName);
    }

    /**
     * Create an empty DataSet.
     *
     * @param reportName The report name
     * @return An empty DataSet
     */
    public static DataSet empty(String reportName) {
        return new DataSet(Collections.emptyList(), Collections.emptyMap(), reportName);
    }
}
