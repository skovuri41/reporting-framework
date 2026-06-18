package com.reporting.framework.transform;

import com.reporting.framework.api.DynamicReportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for transforming and joining DynamicReportResults.
 * Provides operations similar to SQL joins and transformations without requiring Spark.
 */
public class ReportTransformer {

    private static final Logger logger = LoggerFactory.getLogger(ReportTransformer.class);

    /**
     * Join types supported by the transformer.
     */
    public enum JoinType {
        INNER,      // Only matching rows from both sides
        LEFT,       // All rows from left, matching from right (nulls if no match)
        RIGHT,      // All rows from right, matching from left (nulls if no match)
        FULL_OUTER  // All rows from both sides
    }

    /**
     * Perform an inner join between two results.
     *
     * @param left The left result
     * @param right The right result
     * @param leftKey The join key column in the left result
     * @param rightKey The join key column in the right result
     * @return A new DynamicReportResult with joined rows
     */
    public static DynamicReportResult innerJoin(DynamicReportResult left,
                                               DynamicReportResult right,
                                               String leftKey,
                                               String rightKey) {
        return join(left, right, leftKey, rightKey, JoinType.INNER);
    }

    /**
     * Perform a left join between two results.
     *
     * @param left The left result
     * @param right The right result
     * @param leftKey The join key column in the left result
     * @param rightKey The join key column in the right result
     * @return A new DynamicReportResult with joined rows
     */
    public static DynamicReportResult leftJoin(DynamicReportResult left,
                                              DynamicReportResult right,
                                              String leftKey,
                                              String rightKey) {
        return join(left, right, leftKey, rightKey, JoinType.LEFT);
    }

    /**
     * Perform a right join between two results.
     *
     * @param left The left result
     * @param right The right result
     * @param leftKey The join key column in the left result
     * @param rightKey The join key column in the right result
     * @return A new DynamicReportResult with joined rows
     */
    public static DynamicReportResult rightJoin(DynamicReportResult left,
                                               DynamicReportResult right,
                                               String leftKey,
                                               String rightKey) {
        return join(left, right, leftKey, rightKey, JoinType.RIGHT);
    }

    /**
     * Perform a full outer join between two results.
     *
     * @param left The left result
     * @param right The right result
     * @param leftKey The join key column in the left result
     * @param rightKey The join key column in the right result
     * @return A new DynamicReportResult with joined rows
     */
    public static DynamicReportResult fullOuterJoin(DynamicReportResult left,
                                                   DynamicReportResult right,
                                                   String leftKey,
                                                   String rightKey) {
        return join(left, right, leftKey, rightKey, JoinType.FULL_OUTER);
    }

    /**
     * Perform a join operation between two results.
     *
     * @param left The left result
     * @param right The right result
     * @param leftKey The join key column in the left result
     * @param rightKey The join key column in the right result
     * @param joinType The type of join to perform
     * @return A new DynamicReportResult with joined rows
     */
    public static DynamicReportResult join(DynamicReportResult left,
                                          DynamicReportResult right,
                                          String leftKey,
                                          String rightKey,
                                          JoinType joinType) {
        logger.debug("Performing {} join: left={} rows, right={} rows, leftKey='{}', rightKey='{}'",
                joinType, left.count(), right.count(), leftKey, rightKey);

        // Build index from right side for efficient lookup
        Map<Object, List<Map<String, Object>>> rightIndex = right.getRows().stream()
                .collect(Collectors.groupingBy(row -> row.get(rightKey)));

        List<Map<String, Object>> joinedRows = new ArrayList<>();
        Set<Object> matchedRightKeys = new HashSet<>();

        // Process left side
        for (Map<String, Object> leftRow : left.getRows()) {
            Object key = leftRow.get(leftKey);
            List<Map<String, Object>> rightMatches = rightIndex.getOrDefault(key, Collections.emptyList());

            if (rightMatches.isEmpty()) {
                // No match on right side
                if (joinType == JoinType.LEFT || joinType == JoinType.FULL_OUTER) {
                    joinedRows.add(new HashMap<>(leftRow));
                }
            } else {
                // Has matches on right side
                matchedRightKeys.add(key);
                for (Map<String, Object> rightRow : rightMatches) {
                    Map<String, Object> merged = mergeRows(leftRow, rightRow, "left_", "right_");
                    joinedRows.add(merged);
                }
            }
        }

        // For RIGHT and FULL_OUTER joins, add unmatched rows from right side
        if (joinType == JoinType.RIGHT || joinType == JoinType.FULL_OUTER) {
            for (Map.Entry<Object, List<Map<String, Object>>> entry : rightIndex.entrySet()) {
                Object key = entry.getKey();
                if (!matchedRightKeys.contains(key)) {
                    // These right rows had no match on left side
                    for (Map<String, Object> rightRow : entry.getValue()) {
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

        return new DynamicReportResult(joinedRows, combinedOutputParams, joinedReportName);
    }

    /**
     * Merge two rows, prefixing conflicting keys.
     */
    private static Map<String, Object> mergeRows(Map<String, Object> leftRow,
                                                Map<String, Object> rightRow,
                                                String leftPrefix,
                                                String rightPrefix) {
        Map<String, Object> merged = new HashMap<>();

        // Add all left columns
        for (Map.Entry<String, Object> entry : leftRow.entrySet()) {
            String key = entry.getKey();
            if (rightRow.containsKey(key)) {
                // Conflict - use prefix
                merged.put(leftPrefix + key, entry.getValue());
            } else {
                // No conflict
                merged.put(key, entry.getValue());
            }
        }

        // Add all right columns
        for (Map.Entry<String, Object> entry : rightRow.entrySet()) {
            String key = entry.getKey();
            if (leftRow.containsKey(key)) {
                // Conflict - use prefix
                merged.put(rightPrefix + key, entry.getValue());
            } else {
                // No conflict
                merged.put(key, entry.getValue());
            }
        }

        return merged;
    }

    /**
     * Add a prefix to all keys in a row.
     */
    private static Map<String, Object> prefixKeys(Map<String, Object> row, String prefix) {
        Map<String, Object> prefixed = new HashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            prefixed.put(prefix + entry.getKey(), entry.getValue());
        }
        return prefixed;
    }

    /**
     * Union two results (concatenate rows).
     *
     * @param first The first result
     * @param second The second result
     * @return A new DynamicReportResult with all rows from both results
     */
    public static DynamicReportResult union(DynamicReportResult first,
                                           DynamicReportResult second) {
        logger.debug("Performing union: first={} rows, second={} rows",
                first.count(), second.count());

        List<Map<String, Object>> allRows = new ArrayList<>();
        allRows.addAll(first.getRows());
        allRows.addAll(second.getRows());

        Map<String, Object> combinedOutputParams = new HashMap<>(first.getOutputParameters());
        combinedOutputParams.putAll(second.getOutputParameters());

        String unionedReportName = first.getReportName() + "_union_" + second.getReportName();

        return new DynamicReportResult(allRows, combinedOutputParams, unionedReportName);
    }

    /**
     * Union two results and remove duplicates.
     *
     * @param first The first result
     * @param second The second result
     * @return A new DynamicReportResult with distinct rows from both results
     */
    public static DynamicReportResult unionDistinct(DynamicReportResult first,
                                                   DynamicReportResult second) {
        DynamicReportResult unioned = union(first, second);
        return unioned.distinct();
    }

    /**
     * Cross join (Cartesian product) two results.
     * WARNING: This can produce very large results (left_rows * right_rows).
     *
     * @param left The left result
     * @param right The right result
     * @return A new DynamicReportResult with cross product of all rows
     */
    public static DynamicReportResult crossJoin(DynamicReportResult left,
                                               DynamicReportResult right) {
        logger.warn("Performing cross join: {} × {} = {} potential rows",
                left.count(), right.count(), (long) left.count() * right.count());

        List<Map<String, Object>> crossProduct = new ArrayList<>();

        for (Map<String, Object> leftRow : left.getRows()) {
            for (Map<String, Object> rightRow : right.getRows()) {
                Map<String, Object> merged = mergeRows(leftRow, rightRow, "left_", "right_");
                crossProduct.add(merged);
            }
        }

        logger.debug("Cross join produced {} rows", crossProduct.size());

        Map<String, Object> combinedOutputParams = new HashMap<>(left.getOutputParameters());
        combinedOutputParams.putAll(right.getOutputParameters());

        String crossJoinedReportName = left.getReportName() + "_cross_join_" + right.getReportName();

        return new DynamicReportResult(crossProduct, combinedOutputParams, crossJoinedReportName);
    }

    /**
     * Pivot rows into columns based on a key column and value column.
     *
     * @param result The result to pivot
     * @param groupByColumn The column to group by (becomes rows in output)
     * @param pivotColumn The column whose values become column names
     * @param valueColumn The column whose values fill the pivoted cells
     * @return List of pivoted rows
     */
    public static List<Map<String, Object>> pivot(DynamicReportResult result,
                                                  String groupByColumn,
                                                  String pivotColumn,
                                                  String valueColumn) {
        logger.debug("Pivoting result on groupBy='{}', pivot='{}', value='{}'",
                groupByColumn, pivotColumn, valueColumn);

        // Group by the groupByColumn
        Map<Object, List<Map<String, Object>>> grouped = result.groupBy(groupByColumn);

        List<Map<String, Object>> pivoted = new ArrayList<>();

        for (Map.Entry<Object, List<Map<String, Object>>> entry : grouped.entrySet()) {
            Object groupKey = entry.getKey();
            List<Map<String, Object>> groupRows = entry.getValue();

            Map<String, Object> pivotedRow = new HashMap<>();
            pivotedRow.put(groupByColumn, groupKey);

            // For each row in the group, add the pivoted value
            for (Map<String, Object> row : groupRows) {
                Object pivotKey = row.get(pivotColumn);
                Object value = row.get(valueColumn);

                if (pivotKey != null) {
                    pivotedRow.put(String.valueOf(pivotKey), value);
                }
            }

            pivoted.add(pivotedRow);
        }

        logger.debug("Pivot produced {} rows", pivoted.size());
        return pivoted;
    }

    /**
     * Calculate running totals for a numeric column.
     *
     * @param result The result to process
     * @param valueColumn The column to calculate running total for
     * @param runningTotalColumn The name of the new column to add
     * @return A new DynamicReportResult with running total column added
     */
    public static DynamicReportResult withRunningTotal(DynamicReportResult result,
                                                      String valueColumn,
                                                      String runningTotalColumn) {
        logger.debug("Calculating running total for column '{}'", valueColumn);

        List<Map<String, Object>> rows = result.getRows();
        List<Map<String, Object>> newRows = new ArrayList<>();
        double runningTotal = 0.0;

        for (Map<String, Object> row : rows) {
            Map<String, Object> newRow = new HashMap<>(row);
            Object value = row.get(valueColumn);

            if (value instanceof Number) {
                runningTotal += ((Number) value).doubleValue();
            }

            newRow.put(runningTotalColumn, runningTotal);
            newRows.add(newRow);
        }

        return new DynamicReportResult(newRows, result.getOutputParameters(),
                result.getReportName() + "_with_running_total");
    }
}
