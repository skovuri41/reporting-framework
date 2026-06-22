package com.reporting.framework.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Fluent DSL builder for querying and transforming DataSets.
 * Provides a chainable API for filtering, selecting, sorting, and aggregating data.
 *
 * Example usage:
 * <pre>
 * DataSet result = DataQuery.from(dataSet)
 *     .filter(row -> row.getInt("age") > 18)
 *     .select("name", "email", "age")
 *     .orderBy("age").desc()
 *     .limit(10)
 *     .execute();
 * </pre>
 */
public class DataQuery {

    private static final Logger logger = LoggerFactory.getLogger(DataQuery.class);

    private final DataSet source;
    private List<Predicate<DataRow>> filters = new ArrayList<>();
    private Set<String> selectedColumns = null;
    private List<SortSpec> sorts = new ArrayList<>();
    private Integer limitCount = null;
    private boolean distinctRows = false;
    private Map<String, Function<DataRow, Object>> computedColumns = new LinkedHashMap<>();
    private String groupByColumn = null;
    private Map<String, DataOperations.AggregationFunction> aggregations = null;

    private DataQuery(DataSet source) {
        this.source = source;
    }

    /**
     * Start a query from a DataSet.
     *
     * @param source The source DataSet
     * @return A new DataQuery instance
     */
    public static DataQuery from(DataSet source) {
        return new DataQuery(source);
    }

    /**
     * Add a filter predicate.
     *
     * @param predicate The filter condition
     * @return This query for chaining
     */
    public DataQuery filter(Predicate<DataRow> predicate) {
        filters.add(predicate);
        return this;
    }

    /**
     * Select specific columns.
     *
     * @param columns The columns to select
     * @return This query for chaining
     */
    public DataQuery select(String... columns) {
        this.selectedColumns = new LinkedHashSet<>(Arrays.asList(columns));
        return this;
    }

    /**
     * Start an order by clause.
     *
     * @param column The column to sort by
     * @return An OrderByBuilder for specifying sort direction
     */
    public OrderByBuilder orderBy(String column) {
        return new OrderByBuilder(this, column);
    }

    /**
     * Limit the number of rows.
     *
     * @param limit Maximum number of rows to return
     * @return This query for chaining
     */
    public DataQuery limit(int limit) {
        this.limitCount = limit;
        return this;
    }

    /**
     * Remove duplicate rows.
     *
     * @return This query for chaining
     */
    public DataQuery distinct() {
        this.distinctRows = true;
        return this;
    }

    /**
     * Add a computed column.
     *
     * @param columnName The name of the new column
     * @param function The function to compute the column value
     * @return This query for chaining
     */
    public DataQuery withColumn(String columnName, Function<DataRow, Object> function) {
        this.computedColumns.put(columnName, function);
        return this;
    }

    /**
     * Start a group by clause.
     *
     * @param column The column to group by
     * @return A GroupByBuilder for specifying aggregations
     */
    public GroupByBuilder groupBy(String column) {
        return new GroupByBuilder(this, column);
    }

    /**
     * Execute the query and return the result DataSet.
     *
     * @return The transformed DataSet
     */
    public DataSet execute() {
        List<DataRow> result = new ArrayList<>(source.getRows());
        String transformedName = source.getReportName();

        // Apply filters
        if (!filters.isEmpty()) {
            for (Predicate<DataRow> filter : filters) {
                result = result.stream()
                        .filter(filter)
                        .collect(Collectors.toList());
            }
            logger.debug("Filtered {} rows to {} rows", source.count(), result.size());
            transformedName += "_filtered";
        }

        // Apply computed columns
        if (!computedColumns.isEmpty()) {
            result = result.stream()
                    .map(row -> {
                        DataRow newRow = row;
                        for (Map.Entry<String, Function<DataRow, Object>> entry : computedColumns.entrySet()) {
                            Object value = entry.getValue().apply(row);
                            newRow = newRow.with(entry.getKey(), value);
                        }
                        return newRow;
                    })
                    .collect(Collectors.toList());
            logger.debug("Added {} computed columns", computedColumns.size());
        }

        // Apply column selection
        if (selectedColumns != null) {
            result = result.stream()
                    .map(row -> row.select(selectedColumns.toArray(new String[0])))
                    .collect(Collectors.toList());
            logger.debug("Selected {} columns", selectedColumns.size());
            transformedName += "_selected";
        }

        // Apply sorting
        if (!sorts.isEmpty()) {
            result = new ArrayList<>(result);
            result.sort((row1, row2) -> {
                for (SortSpec sort : sorts) {
                    Object val1 = row1.get(sort.column);
                    Object val2 = row2.get(sort.column);

                    if (val1 == null && val2 == null) continue;
                    if (val1 == null) return sort.ascending ? -1 : 1;
                    if (val2 == null) return sort.ascending ? 1 : -1;

                    @SuppressWarnings("unchecked")
                    int comparison = ((Comparable<Object>) val1).compareTo(val2);
                    if (comparison != 0) {
                        return sort.ascending ? comparison : -comparison;
                    }
                }
                return 0;
            });
            logger.debug("Sorted by {} columns", sorts.size());
            transformedName += "_sorted";
        }

        // Apply distinct
        if (distinctRows) {
            result = result.stream()
                    .distinct()
                    .collect(Collectors.toList());
            logger.debug("Applied distinct, reduced to {} rows", result.size());
            transformedName += "_distinct";
        }

        // Apply limit
        if (limitCount != null) {
            result = result.stream()
                    .limit(limitCount)
                    .collect(Collectors.toList());
            logger.debug("Limited to {} rows", limitCount);
            transformedName += "_limited";
        }

        // Handle groupBy aggregations
        if (groupByColumn != null && aggregations != null) {
            Map<Object, Map<String, Object>> grouped = DataOperations.groupByWithAggregations(
                    result, groupByColumn, aggregations);

            // Convert to list of DataRows
            result = grouped.values().stream()
                    .map(DataRow::of)
                    .collect(Collectors.toList());

            logger.debug("Grouped by '{}' with {} aggregations, produced {} groups",
                    groupByColumn, aggregations.size(), result.size());
            transformedName += "_grouped";
        }

        return new DataSet(result, source.getOutputParameters(), transformedName);
    }

    /**
     * Internal method to add a sort specification.
     */
    void addSort(String column, boolean ascending) {
        this.sorts.add(new SortSpec(column, ascending));
    }

    /**
     * Internal method to set group by with aggregations.
     */
    void setGroupBy(String column, Map<String, DataOperations.AggregationFunction> aggs) {
        this.groupByColumn = column;
        this.aggregations = aggs;
    }

    /**
     * Sort specification.
     */
    private static class SortSpec {
        final String column;
        final boolean ascending;

        SortSpec(String column, boolean ascending) {
            this.column = column;
            this.ascending = ascending;
        }
    }

    /**
     * Builder for order by clauses.
     */
    public static class OrderByBuilder {
        private final DataQuery query;
        private final String column;

        OrderByBuilder(DataQuery query, String column) {
            this.query = query;
            this.column = column;
        }

        /**
         * Sort in ascending order.
         *
         * @return The query for chaining
         */
        public DataQuery asc() {
            query.addSort(column, true);
            return query;
        }

        /**
         * Sort in descending order.
         *
         * @return The query for chaining
         */
        public DataQuery desc() {
            query.addSort(column, false);
            return query;
        }
    }

    /**
     * Builder for group by clauses with aggregations.
     */
    public static class GroupByBuilder {
        private final DataQuery query;
        private final String column;
        private final Map<String, DataOperations.AggregationFunction> aggregations = new HashMap<>();

        GroupByBuilder(DataQuery query, String column) {
            this.query = query;
            this.column = column;
        }

        /**
         * Add an aggregation function for a column.
         *
         * @param column The column to aggregate
         * @param function The aggregation function
         * @return This builder for chaining
         */
        public GroupByBuilder agg(String column, DataOperations.AggregationFunction function) {
            this.aggregations.put(column, function);
            return this;
        }

        /**
         * Add a SUM aggregation.
         *
         * @param column The column to sum
         * @return This builder for chaining
         */
        public GroupByBuilder sum(String column) {
            return agg(column, DataOperations.AggregationFunction.SUM);
        }

        /**
         * Add an AVG aggregation.
         *
         * @param column The column to average
         * @return This builder for chaining
         */
        public GroupByBuilder avg(String column) {
            return agg(column, DataOperations.AggregationFunction.AVG);
        }

        /**
         * Add a COUNT aggregation.
         *
         * @param column The column to count
         * @return This builder for chaining
         */
        public GroupByBuilder count(String column) {
            return agg(column, DataOperations.AggregationFunction.COUNT);
        }

        /**
         * Add a MIN aggregation.
         *
         * @param column The column to find minimum
         * @return This builder for chaining
         */
        public GroupByBuilder min(String column) {
            return agg(column, DataOperations.AggregationFunction.MIN);
        }

        /**
         * Add a MAX aggregation.
         *
         * @param column The column to find maximum
         * @return This builder for chaining
         */
        public GroupByBuilder max(String column) {
            return agg(column, DataOperations.AggregationFunction.MAX);
        }

        /**
         * Execute the query with the specified aggregations.
         *
         * @return The transformed DataSet
         */
        public DataSet execute() {
            query.setGroupBy(column, aggregations);
            return query.execute();
        }
    }
}
