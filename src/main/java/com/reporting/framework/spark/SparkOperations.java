package com.reporting.framework.spark;

import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import static org.apache.spark.sql.functions.*;

import java.util.Map;

/**
 * Utility class for common Spark transformations on Datasets.
 * Provides convenient methods for filtering, selecting, joining, and aggregating data.
 */
public class SparkOperations {

    private SparkOperations() {
        // Private constructor to prevent instantiation
    }

    /**
     * Filter dataset by SQL condition.
     *
     * @param dataset Input dataset
     * @param condition SQL condition (e.g., "salary > 80000")
     * @return Filtered dataset
     */
    public static Dataset<Row> filter(Dataset<Row> dataset, String condition) {
        return dataset.filter(condition);
    }

    /**
     * Select specific columns from dataset.
     *
     * @param dataset Input dataset
     * @param columns Column names to select
     * @return Dataset with only selected columns
     */
    public static Dataset<Row> select(Dataset<Row> dataset, String... columns) {
        return dataset.select(columns[0],
            java.util.Arrays.copyOfRange(columns, 1, columns.length));
    }

    /**
     * Join two datasets on a common column.
     *
     * @param left Left dataset
     * @param right Right dataset
     * @param joinColumn Column name to join on
     * @return Joined dataset
     */
    public static Dataset<Row> join(Dataset<Row> left, Dataset<Row> right, String joinColumn) {
        return left.join(right, joinColumn);
    }

    /**
     * Join two datasets with custom join condition and type.
     *
     * @param left Left dataset
     * @param right Right dataset
     * @param joinCondition Join condition column
     * @param joinType Type of join (inner, left, right, outer)
     * @return Joined dataset
     */
    public static Dataset<Row> join(Dataset<Row> left, Dataset<Row> right,
                                     Column joinCondition, String joinType) {
        return left.join(right, joinCondition, joinType);
    }

    /**
     * Group by columns and apply aggregation functions.
     *
     * @param dataset Input dataset
     * @param groupColumns Columns to group by
     * @param aggFunctions Map of column names to aggregation function names
     *                     (e.g., {"salary": "avg", "employee_id": "count"})
     * @return Aggregated dataset
     */
    public static Dataset<Row> groupBy(Dataset<Row> dataset, String[] groupColumns,
                                        Map<String, String> aggFunctions) {
        if (groupColumns == null || groupColumns.length == 0) {
            throw new IllegalArgumentException("Group columns cannot be empty");
        }
        if (aggFunctions == null || aggFunctions.isEmpty()) {
            throw new IllegalArgumentException("Aggregation functions cannot be empty");
        }

        // Start with group by
        var grouped = dataset.groupBy(groupColumns[0],
            java.util.Arrays.copyOfRange(groupColumns, 1, groupColumns.length));

        // Build aggregation expressions
        Column[] aggExprs = new Column[aggFunctions.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : aggFunctions.entrySet()) {
            String columnName = entry.getKey();
            String function = entry.getValue().toLowerCase();

            Column aggCol = switch (function) {
                case "avg", "average" -> avg(columnName).as(function + "_" + columnName);
                case "sum" -> sum(columnName).as("sum_" + columnName);
                case "count" -> count(columnName).as("count_" + columnName);
                case "min" -> min(columnName).as("min_" + columnName);
                case "max" -> max(columnName).as("max_" + columnName);
                default -> throw new IllegalArgumentException("Unsupported aggregation function: " + function);
            };

            aggExprs[i++] = aggCol;
        }

        return grouped.agg(aggExprs[0], java.util.Arrays.copyOfRange(aggExprs, 1, aggExprs.length));
    }

    /**
     * Order dataset by columns (ascending).
     *
     * @param dataset Input dataset
     * @param columns Column names to order by
     * @return Ordered dataset
     */
    public static Dataset<Row> orderBy(Dataset<Row> dataset, String... columns) {
        return dataset.orderBy(columns[0],
            java.util.Arrays.copyOfRange(columns, 1, columns.length));
    }

    /**
     * Order dataset by columns (descending).
     *
     * @param dataset Input dataset
     * @param columns Column names to order by (descending)
     * @return Ordered dataset
     */
    public static Dataset<Row> orderByDesc(Dataset<Row> dataset, String... columns) {
        Column[] cols = new Column[columns.length];
        for (int i = 0; i < columns.length; i++) {
            cols[i] = col(columns[i]).desc();
        }
        return dataset.orderBy(cols);
    }

    /**
     * Limit the number of rows in dataset.
     *
     * @param dataset Input dataset
     * @param n Number of rows to limit to
     * @return Limited dataset
     */
    public static Dataset<Row> limit(Dataset<Row> dataset, int n) {
        return dataset.limit(n);
    }

    /**
     * Add a new column with a SQL expression.
     *
     * @param dataset Input dataset
     * @param columnName New column name
     * @param expression SQL expression
     * @return Dataset with new column
     */
    public static Dataset<Row> withColumn(Dataset<Row> dataset, String columnName, String expression) {
        return dataset.withColumn(columnName, expr(expression));
    }

    /**
     * Drop columns from dataset.
     *
     * @param dataset Input dataset
     * @param columns Column names to drop
     * @return Dataset without specified columns
     */
    public static Dataset<Row> drop(Dataset<Row> dataset, String... columns) {
        return dataset.drop(columns);
    }

    /**
     * Remove duplicate rows.
     *
     * @param dataset Input dataset
     * @return Dataset with duplicates removed
     */
    public static Dataset<Row> distinct(Dataset<Row> dataset) {
        return dataset.distinct();
    }

    /**
     * Union two datasets (must have same schema).
     *
     * @param dataset1 First dataset
     * @param dataset2 Second dataset
     * @return Combined dataset
     */
    public static Dataset<Row> union(Dataset<Row> dataset1, Dataset<Row> dataset2) {
        return dataset1.union(dataset2);
    }
}
