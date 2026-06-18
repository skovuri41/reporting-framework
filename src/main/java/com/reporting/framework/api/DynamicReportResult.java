package com.reporting.framework.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reporting.framework.exception.ReportExecutionException;
import com.reporting.framework.mapper.ObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Dynamic report result container that holds results as List of Maps.
 * Provides transformation methods (filter, select, groupBy) without requiring POJO classes.
 *
 * This is a lightweight alternative to Spark Dataset<Row> that uses pure Java collections.
 */
public class DynamicReportResult {

    private static final Logger logger = LoggerFactory.getLogger(DynamicReportResult.class);
    private static final ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapper();

    private final List<Map<String, Object>> rows;
    private final Map<String, Object> outputParameters;
    private final String reportName;

    /**
     * Create a new DynamicReportResult.
     *
     * @param rows The result rows as List of Maps
     * @param outputParameters Output parameters from stored procedure
     * @param reportName The name of the report
     */
    public DynamicReportResult(List<Map<String, Object>> rows,
                               Map<String, Object> outputParameters,
                               String reportName) {
        this.rows = rows != null ? new ArrayList<>(rows) : new ArrayList<>();
        this.outputParameters = outputParameters != null ? new HashMap<>(outputParameters) : new HashMap<>();
        this.reportName = reportName;
    }

    /**
     * Get all rows.
     */
    public List<Map<String, Object>> getRows() {
        return new ArrayList<>(rows);
    }

    /**
     * Get output parameters.
     */
    public Map<String, Object> getOutputParameters() {
        return new HashMap<>(outputParameters);
    }

    /**
     * Get report name.
     */
    public String getReportName() {
        return reportName;
    }

    /**
     * Get the number of rows.
     */
    public int count() {
        return rows.size();
    }

    /**
     * Check if the result is empty.
     */
    public boolean isEmpty() {
        return rows.isEmpty();
    }

    /**
     * Filter rows by a predicate.
     *
     * @param predicate The filter condition
     * @return A new DynamicReportResult with filtered rows
     */
    public DynamicReportResult filter(Predicate<Map<String, Object>> predicate) {
        List<Map<String, Object>> filtered = rows.stream()
                .filter(predicate)
                .collect(Collectors.toList());

        logger.debug("Filtered {} rows to {} rows", rows.size(), filtered.size());
        return new DynamicReportResult(filtered, outputParameters, reportName + "_filtered");
    }

    /**
     * Select specific columns from all rows.
     *
     * @param columns The columns to select
     * @return A new DynamicReportResult with only selected columns
     */
    public DynamicReportResult select(String... columns) {
        Set<String> columnSet = new HashSet<>(Arrays.asList(columns));

        List<Map<String, Object>> selected = rows.stream()
                .map(row -> {
                    Map<String, Object> newRow = new HashMap<>();
                    for (String column : columnSet) {
                        if (row.containsKey(column)) {
                            newRow.put(column, row.get(column));
                        }
                    }
                    return newRow;
                })
                .collect(Collectors.toList());

        logger.debug("Selected {} columns from {} rows", columns.length, rows.size());
        return new DynamicReportResult(selected, outputParameters, reportName + "_selected");
    }

    /**
     * Group rows by a column value.
     *
     * @param column The column to group by
     * @return Map of group key to list of rows
     */
    public Map<Object, List<Map<String, Object>>> groupBy(String column) {
        Map<Object, List<Map<String, Object>>> grouped = rows.stream()
                .collect(Collectors.groupingBy(row -> row.get(column)));

        logger.debug("Grouped {} rows by column '{}' into {} groups",
                rows.size(), column, grouped.size());
        return grouped;
    }

    /**
     * Group rows by a column and compute aggregations.
     *
     * @param column The column to group by
     * @param aggregations Map of column names to aggregation functions
     * @return Map of group key to aggregated values
     */
    public Map<Object, Map<String, Object>> groupByWithAggregations(
            String column,
            Map<String, AggregationFunction> aggregations) {

        Map<Object, List<Map<String, Object>>> grouped = groupBy(column);

        Map<Object, Map<String, Object>> result = new HashMap<>();
        for (Map.Entry<Object, List<Map<String, Object>>> entry : grouped.entrySet()) {
            Object key = entry.getKey();
            List<Map<String, Object>> groupRows = entry.getValue();

            Map<String, Object> aggregatedValues = new HashMap<>();
            aggregatedValues.put(column, key); // Include the grouping column

            for (Map.Entry<String, AggregationFunction> agg : aggregations.entrySet()) {
                String aggColumn = agg.getKey();
                AggregationFunction function = agg.getValue();
                Object aggValue = computeAggregation(groupRows, aggColumn, function);
                aggregatedValues.put(aggColumn + "_" + function.name().toLowerCase(), aggValue);
            }

            result.put(key, aggregatedValues);
        }

        logger.debug("Computed {} aggregations on {} groups", aggregations.size(), result.size());
        return result;
    }

    /**
     * Compute an aggregation for a column across multiple rows.
     */
    private Object computeAggregation(List<Map<String, Object>> rows,
                                     String column,
                                     AggregationFunction function) {
        switch (function) {
            case COUNT:
                return (long) rows.size();

            case SUM:
                return rows.stream()
                        .map(row -> row.get(column))
                        .filter(Objects::nonNull)
                        .map(this::toDouble)
                        .reduce(0.0, Double::sum);

            case AVG:
                double sum = rows.stream()
                        .map(row -> row.get(column))
                        .filter(Objects::nonNull)
                        .map(this::toDouble)
                        .reduce(0.0, Double::sum);
                long count = rows.stream()
                        .map(row -> row.get(column))
                        .filter(Objects::nonNull)
                        .count();
                return count > 0 ? sum / count : null;

            case MIN:
                return rows.stream()
                        .map(row -> row.get(column))
                        .filter(Objects::nonNull)
                        .map(this::toDouble)
                        .min(Double::compareTo)
                        .orElse(null);

            case MAX:
                return rows.stream()
                        .map(row -> row.get(column))
                        .filter(Objects::nonNull)
                        .map(this::toDouble)
                        .max(Double::compareTo)
                        .orElse(null);

            default:
                throw new IllegalArgumentException("Unknown aggregation function: " + function);
        }
    }

    /**
     * Convert a value to double for aggregation calculations.
     */
    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                logger.warn("Cannot convert '{}' to number for aggregation", value);
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * Add a computed column to all rows.
     *
     * @param columnName The name of the new column
     * @param function The function to compute the column value
     * @return A new DynamicReportResult with the computed column added
     */
    public DynamicReportResult withColumn(String columnName,
                                         Function<Map<String, Object>, Object> function) {
        List<Map<String, Object>> newRows = rows.stream()
                .map(row -> {
                    Map<String, Object> newRow = new HashMap<>(row);
                    newRow.put(columnName, function.apply(row));
                    return newRow;
                })
                .collect(Collectors.toList());

        logger.debug("Added computed column '{}' to {} rows", columnName, rows.size());
        return new DynamicReportResult(newRows, outputParameters, reportName);
    }

    /**
     * Sort rows by a column.
     *
     * @param column The column to sort by
     * @param ascending True for ascending, false for descending
     * @return A new DynamicReportResult with sorted rows
     */
    public DynamicReportResult orderBy(String column, boolean ascending) {
        List<Map<String, Object>> sorted = new ArrayList<>(rows);
        sorted.sort((row1, row2) -> {
            Object val1 = row1.get(column);
            Object val2 = row2.get(column);

            if (val1 == null && val2 == null) return 0;
            if (val1 == null) return ascending ? -1 : 1;
            if (val2 == null) return ascending ? 1 : -1;

            @SuppressWarnings("unchecked")
            int comparison = ((Comparable<Object>) val1).compareTo(val2);
            return ascending ? comparison : -comparison;
        });

        logger.debug("Sorted {} rows by column '{}' ({})",
                rows.size(), column, ascending ? "ASC" : "DESC");
        return new DynamicReportResult(sorted, outputParameters, reportName + "_sorted");
    }

    /**
     * Limit the number of rows.
     *
     * @param limit Maximum number of rows to return
     * @return A new DynamicReportResult with limited rows
     */
    public DynamicReportResult limit(int limit) {
        List<Map<String, Object>> limited = rows.stream()
                .limit(limit)
                .collect(Collectors.toList());

        logger.debug("Limited {} rows to {} rows", rows.size(), limited);
        return new DynamicReportResult(limited, outputParameters, reportName + "_limited");
    }

    /**
     * Convert all rows to JSON string.
     *
     * @return JSON representation of the rows
     */
    public String toJSON() {
        try {
            return objectMapper.writeValueAsString(rows);
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
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert rows to pretty JSON", e);
            throw new ReportExecutionException("Failed to convert result to JSON", e);
        }
    }

    /**
     * Get a specific row by index.
     *
     * @param index The row index
     * @return The row at the specified index
     */
    public Map<String, Object> getRow(int index) {
        if (index < 0 || index >= rows.size()) {
            throw new IndexOutOfBoundsException(
                    "Row index " + index + " out of bounds for size " + rows.size());
        }
        return new HashMap<>(rows.get(index));
    }

    /**
     * Get the first row, or null if empty.
     */
    public Map<String, Object> first() {
        return rows.isEmpty() ? null : new HashMap<>(rows.get(0));
    }

    /**
     * Get distinct rows (removes duplicates).
     *
     * @return A new DynamicReportResult with distinct rows
     */
    public DynamicReportResult distinct() {
        List<Map<String, Object>> distinctRows = rows.stream()
                .distinct()
                .collect(Collectors.toList());

        logger.debug("Reduced {} rows to {} distinct rows", rows.size(), distinctRows.size());
        return new DynamicReportResult(distinctRows, outputParameters, reportName + "_distinct");
    }

    /**
     * Aggregation functions for groupBy operations.
     */
    public enum AggregationFunction {
        SUM, AVG, COUNT, MIN, MAX
    }

    @Override
    public String toString() {
        return "DynamicReportResult{" +
                "reportName='" + reportName + '\'' +
                ", rows=" + rows.size() +
                ", outputParameters=" + outputParameters.size() +
                '}';
    }
}
