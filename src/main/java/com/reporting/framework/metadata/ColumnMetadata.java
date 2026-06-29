package com.reporting.framework.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents metadata for a result set column.
 * Maps database column names to camelCase field names.
 */
public class ColumnMetadata {

    private final String columnName;
    private final String fieldName;
    private final String columnType;

    @JsonCreator
    public ColumnMetadata(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("fieldName") String fieldName,
            @JsonProperty("columnType") String columnType) {
        this.columnName = columnName;
        this.fieldName = fieldName;
        this.columnType = columnType;
    }

    /**
     * The database column name (e.g., "EMPLOYEE_ID", "FIRST_NAME").
     */
    public String getColumnName() {
        return columnName;
    }

    /**
     * The camelCase field name (e.g., "employeeId", "firstName").
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * The column data type (e.g., "INTEGER", "VARCHAR", "DECIMAL").
     */
    public String getColumnType() {
        return columnType;
    }

    @Override
    public String toString() {
        return "ColumnMetadata{" +
                "columnName='" + columnName + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", columnType='" + columnType + '\'' +
                '}';
    }
}
