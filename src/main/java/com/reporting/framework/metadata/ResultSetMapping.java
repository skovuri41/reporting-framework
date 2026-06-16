package com.reporting.framework.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * Represents the mapping strategy for converting ResultSet to POJOs.
 * Uses explicit column-to-field mappings for clarity and self-documentation.
 */
public class ResultSetMapping {

    private final String strategy;
    private final List<ColumnMapping> columnMappings;
    private final String unmappedColumnsStrategy;

    @JsonCreator
    public ResultSetMapping(
            @JsonProperty("strategy") String strategy,
            @JsonProperty("columnMappings") List<ColumnMapping> columnMappings,
            @JsonProperty("unmappedColumnsStrategy") String unmappedColumnsStrategy) {
        this.strategy = strategy != null ? strategy : "EXPLICIT";
        this.columnMappings = columnMappings != null ? columnMappings : Collections.emptyList();
        this.unmappedColumnsStrategy = unmappedColumnsStrategy != null ? unmappedColumnsStrategy : "IGNORE";
    }

    /**
     * The mapping strategy (currently only "EXPLICIT" is supported).
     */
    public String getStrategy() {
        return strategy;
    }

    /**
     * Explicit column-to-field mappings.
     */
    public List<ColumnMapping> getColumnMappings() {
        return columnMappings;
    }

    /**
     * Strategy for handling columns in ResultSet that are not in columnMappings.
     * Options: "IGNORE" (default), "ERROR", "WARN"
     */
    public String getUnmappedColumnsStrategy() {
        return unmappedColumnsStrategy;
    }

    @Override
    public String toString() {
        return "ResultSetMapping{" +
                "strategy='" + strategy + '\'' +
                ", columnMappings=" + columnMappings.size() +
                ", unmappedColumnsStrategy='" + unmappedColumnsStrategy + '\'' +
                '}';
    }
}
