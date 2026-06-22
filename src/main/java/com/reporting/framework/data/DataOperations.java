package com.reporting.framework.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class providing complex data operations like joins, unions, pivots, and aggregations.
 * All methods are static and operate on DataSets and DataRows.
 */
public class DataOperations {

    private static final Logger logger = LoggerFactory.getLogger(DataOperations.class);

    /**
     * Join types supported.
     */
    public enum JoinType {
        INNER,      // Only matching rows from both sides
        LEFT,       // All rows from left, matching from right (nulls if no match)
        RIGHT,      // All rows from right, matching from left (nulls if no match)
        FULL_OUTER  // All rows from both sides
    }

    /**
     * Aggregation functions.
     */
    public enum AggregationFunction {
        SUM, AVG, COUNT, MIN, MAX
    }

    /**
     * Perform an inner join between two DataSets.
     *
     * @param left The left DataSet
     * @param right The right DataSet
     * @param leftKey The join key column in the left DataSet
     * @param rightKey The join key column in the right DataSet
     * @return A new DataSet with joined rows
     */
    public static DataSet innerJoin(DataSet left, DataSet right, String leftKey, String rightKey) {
        return join(left, right, leftKey, rightKey, JoinType.INNER);
    }

    /**
     * Perform a left join between two DataSets.
     *
     * @param left The left DataSet
     * @param right The right DataSet
     * @param leftKey The join key column in the left DataSet
     * @param rightKey The join key column in the right DataSet
     * @return A new DataSet with joined rows
     */
    public static DataSet leftJoin(DataSet left, DataSet right, String leftKey, String rightKey) {
        return join(left, right, leftKey, rightKey, JoinType.LEFT);
    }

    /**
     * Perform a right join between two DataSets.
     *
     * @param left The left DataSet
     * @param right The right DataSet
     * @param leftKey The join key column in the left DataSet
     * @param rightKey The join key column in the right DataSet
     * @return A new DataSet with joined rows
     */
    public static DataSet rightJoin(DataSet left, DataSet right, String leftKey, String rightKey) {
        return join(left, right, leftKey, rightKey, JoinType.RIGHT);
    }

    /**
     * Perform a full outer join between two DataSets.
     *
     * @param left The left DataSet
     * @param right The right DataSet
     * @param leftKey The join key column in the left DataSet
     * @param rightKey The join key column in the right DataSet
     * @return A new DataSet with joined rows
     */
    public static DataSet fullOuterJoin(DataSet left, DataSet right, String leftKey, String rightKey) {
        return join(left, right, leftKey, rightKey, JoinType.FULL_OUTER);
    }

    /**
     * Perform a join operation between two DataSets.
     *
     * @param left The left DataSet
     * @param right The right DataSet
     * @param leftKey The join key column in the left DataSet
     * @param rightKey The join key column in the right DataSet
     * @param joinType The type of join to perform
     * @return A new DataSet with joined rows
     */
    public static DataSet join(DataSet left, DataSet right, String leftKey, String rightKey, JoinType joinType) {
        logger.debug("Performing {} join: left={} rows, right={} rows, leftKey='{}', rightKey='{}'",
                joinType, left.count(), right.count(), leftKey, rightKey);

        // Build index from right side for efficient lookup
        Map<Object, List<DataRow>> rightIndex = right.getRows().stream()
                .collect(Collectors.groupingBy(row -> row.get(rightKey)));

        List<DataRow> joinedRows = new ArrayList<>();
        Set<Object> matchedRightKeys = new HashSet<>();

        // Process left side
        for (DataRow leftRow : left.getRows()) {
            Object key = leftRow.get(leftKey);
            List<DataRow> rightMatches = rightIndex.getOrDefault(key, Collections.emptyList());

            if (rightMatches.isEmpty()) {
                // No match on right side
                if (joinType == JoinType.LEFT || joinType == JoinType.FULL_OUTER) {
                    joinedRows.add(leftRow);
                }
            } else {
                // Has matches on right side
                matchedRightKeys.add(key);
                for (DataRow rightRow : rightMatches) {
                    DataRow merged = mergeRows(leftRow, rightRow, "left_", "right_");
                    joinedRows.add(merged);
                }
            }
        }

        // For RIGHT and FULL_OUTER joins, add unmatched rows from right side
        if (joinType == JoinType.RIGHT || joinType == JoinType.FULL_OUTER) {
            for (Map.Entry<Object, List<DataRow>> entry : rightIndex.entrySet()) {
                Object key = entry.getKey();
                if (!matchedRightKeys.contains(key)) {
                    // These right rows had no match on left side
                    for (DataRow rightRow : entry.getValue()) {
                        joinedRows.add(prefixKeys(rightRow, "right_"));
                    }
                }
            }
        }

        logger.debug("Join produced {} rows", joinedRows.size());

        String joinedReportName = left.getReportName() + "_" + joinType.name().toLowerCase() +
                "_join_" + right.getReportName();

        // Combine output parameters from both sides
        Map<String, Object> combinedOutputParams = new HashMap<>(left.getOutputParameters());
        combinedOutputParams.putAll(right.getOutputParameters());

        return new DataSet(joinedRows, combinedOutputParams, joinedReportName);
    }

    /**
     * Merge two rows, prefixing conflicting keys.
     */
    private static DataRow mergeRows(DataRow leftRow, DataRow rightRow, String leftPrefix, String rightPrefix) {
        Map<String, Object> merged = new HashMap<>();

        Set<String> leftKeys = leftRow.keys();
        Set<String> rightKeys = rightRow.keys();

        // Add all left columns
        for (String key : leftKeys) {
            if (rightKeys.contains(key)) {
                // Conflict - use prefix
                merged.put(leftPrefix + key, leftRow.get(key));
            } else {
                // No conflict
                merged.put(key, leftRow.get(key));
            }
        }

        // Add all right columns
        for (String key : rightKeys) {
            if (leftKeys.contains(key)) {
                // Conflict - use prefix
                merged.put(rightPrefix + key, rightRow.get(key));
            } else {
                // No conflict
                merged.put(key, rightRow.get(key));
            }
        }

        return DataRow.of(merged);
    }

    /**
     * Add a prefix to all keys in a row.
     */
    private static DataRow prefixKeys(DataRow row, String prefix) {
        Map<String, Object> prefixed = new HashMap<>();
        for (String key : row.keys()) {
            prefixed.put(prefix + key, row.get(key));
        }
        return DataRow.of(prefixed);
    }

    /**
     * Union two DataSets (concatenate rows).
     *
     * @param first The first DataSet
     * @param second The second DataSet
     * @return A new DataSet with all rows from both DataSets
     */
    public static DataSet union(DataSet first, DataSet second) {
        logger.debug("Performing union: first={} rows, second={} rows", first.count(), second.count());

        List<DataRow> allRows = new ArrayList<>();
        allRows.addAll(first.getRows());
        allRows.addAll(second.getRows());

        Map<String, Object> combinedOutputParams = new HashMap<>(first.getOutputParameters());
        combinedOutputParams.putAll(second.getOutputParameters());

        String unionedReportName = first.getReportName() + "_union_" + second.getReportName();

        return new DataSet(allRows, combinedOutputParams, unionedReportName);
    }

    /**
     * Union two DataSets and remove duplicates.
     *
     * @param first The first DataSet
     * @param second The second DataSet
     * @return A new DataSet with distinct rows from both DataSets
     */
    public static DataSet unionDistinct(DataSet first, DataSet second) {
        DataSet unioned = union(first, second);
        return DataQuery.from(unioned).distinct().execute();
    }

    /**
     * Cross join (Cartesian product) two DataSets.
     * WARNING: This can produce very large results (left_rows * right_rows).
     *
     * @param left The left DataSet
     * @param right The right DataSet
     * @return A new DataSet with cross product of all rows
     */
    public static DataSet crossJoin(DataSet left, DataSet right) {
        logger.warn("Performing cross join: {} × {} = {} potential rows",
                left.count(), right.count(), (long) left.count() * right.count());

        List<DataRow> crossProduct = new ArrayList<>();

        for (DataRow leftRow : left.getRows()) {
            for (DataRow rightRow : right.getRows()) {
                DataRow merged = mergeRows(leftRow, rightRow, "left_", "right_");
                crossProduct.add(merged);
            }
        }

        logger.debug("Cross join produced {} rows", crossProduct.size());

        Map<String, Object> combinedOutputParams = new HashMap<>(left.getOutputParameters());
        combinedOutputParams.putAll(right.getOutputParameters());

        String crossJoinedReportName = left.getReportName() + "_cross_join_" + right.getReportName();

        return new DataSet(crossProduct, combinedOutputParams, crossJoinedReportName);
    }

    /**
     * Pivot rows into columns based on a key column and value column.
     *
     * @param dataSet The DataSet to pivot
     * @param groupByColumn The column to group by (becomes rows in output)
     * @param pivotColumn The column whose values become column names
     * @param valueColumn The column whose values fill the pivoted cells
     * @return List of pivoted rows as DataRows
     */
    public static List<DataRow> pivot(DataSet dataSet, String groupByColumn, String pivotColumn, String valueColumn) {
        logger.debug("Pivoting DataSet on groupBy='{}', pivot='{}', value='{}'",
                groupByColumn, pivotColumn, valueColumn);

        // Group by the groupByColumn
        Map<Object, List<DataRow>> grouped = dataSet.getRows().stream()
                .collect(Collectors.groupingBy(row -> row.get(groupByColumn)));

        List<DataRow> pivoted = new ArrayList<>();

        for (Map.Entry<Object, List<DataRow>> entry : grouped.entrySet()) {
            Object groupKey = entry.getKey();
            List<DataRow> groupRows = entry.getValue();

            Map<String, Object> pivotedRow = new HashMap<>();
            pivotedRow.put(groupByColumn, groupKey);

            // For each row in the group, add the pivoted value
            for (DataRow row : groupRows) {
                Object pivotKey = row.get(pivotColumn);
                Object value = row.get(valueColumn);

                if (pivotKey != null) {
                    pivotedRow.put(String.valueOf(pivotKey), value);
                }
            }

            pivoted.add(DataRow.of(pivotedRow));
        }

        logger.debug("Pivot produced {} rows", pivoted.size());
        return pivoted;
    }

    /**
     * Calculate running totals for a numeric column.
     *
     * @param dataSet The DataSet to process
     * @param valueColumn The column to calculate running total for
     * @param runningTotalColumn The name of the new column to add
     * @return A new DataSet with running total column added
     */
    public static DataSet withRunningTotal(DataSet dataSet, String valueColumn, String runningTotalColumn) {
        logger.debug("Calculating running total for column '{}'", valueColumn);

        List<DataRow> rows = dataSet.getRows();
        List<DataRow> newRows = new ArrayList<>();
        double runningTotal = 0.0;

        for (DataRow row : rows) {
            Object value = row.get(valueColumn);

            if (value instanceof Number) {
                runningTotal += ((Number) value).doubleValue();
            }

            DataRow newRow = row.with(runningTotalColumn, runningTotal);
            newRows.add(newRow);
        }

        return new DataSet(newRows, dataSet.getOutputParameters(),
                dataSet.getReportName() + "_with_running_total");
    }

    /**
     * Group rows by a column and compute aggregations.
     *
     * @param rows The rows to group
     * @param column The column to group by
     * @param aggregations Map of column names to aggregation functions
     * @return Map of group key to aggregated values
     */
    public static Map<Object, Map<String, Object>> groupByWithAggregations(
            List<DataRow> rows,
            String column,
            Map<String, AggregationFunction> aggregations) {

        Map<Object, List<DataRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(row -> row.get(column)));

        Map<Object, Map<String, Object>> result = new HashMap<>();

        for (Map.Entry<Object, List<DataRow>> entry : grouped.entrySet()) {
            Object key = entry.getKey();
            List<DataRow> groupRows = entry.getValue();

            Map<String, Object> aggregatedValues = new HashMap<>();
            aggregatedValues.put(column, key); // Include the grouping column

            for (Map.Entry<String, AggregationFunction> agg : aggregations.entrySet()) {
                String aggColumn = agg.getKey();
                AggregationFunction function = agg.getValue();
                Object aggValue = aggregate(groupRows, aggColumn, function);
                aggregatedValues.put(aggColumn + "_" + function.name().toLowerCase(), aggValue);
            }

            result.put(key, aggregatedValues);
        }

        logger.debug("Computed {} aggregations on {} groups", aggregations.size(), result.size());
        return result;
    }

    /**
     * Compute an aggregation for a column across multiple rows.
     *
     * @param rows The rows to aggregate
     * @param column The column to aggregate
     * @param function The aggregation function
     * @return The aggregated value
     */
    public static Object aggregate(List<DataRow> rows, String column, AggregationFunction function) {
        switch (function) {
            case COUNT:
                return (long) rows.size();

            case SUM:
                return rows.stream()
                        .map(row -> row.get(column))
                        .filter(Objects::nonNull)
                        .map(DataOperations::toDouble)
                        .reduce(0.0, Double::sum);

            case AVG:
                double sum = rows.stream()
                        .map(row -> row.get(column))
                        .filter(Objects::nonNull)
                        .map(DataOperations::toDouble)
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
                        .map(DataOperations::toDouble)
                        .min(Double::compareTo)
                        .orElse(null);

            case MAX:
                return rows.stream()
                        .map(row -> row.get(column))
                        .filter(Objects::nonNull)
                        .map(DataOperations::toDouble)
                        .max(Double::compareTo)
                        .orElse(null);

            default:
                throw new IllegalArgumentException("Unknown aggregation function: " + function);
        }
    }

    /**
     * Convert a value to double for aggregation calculations.
     */
    private static Double toDouble(Object value) {
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
}
