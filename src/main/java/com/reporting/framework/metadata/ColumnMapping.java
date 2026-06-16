package com.reporting.framework.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents an explicit mapping between a database column and a POJO field.
 * This provides clear, self-documenting mapping without relying on naming conventions.
 */
public class ColumnMapping {

    private final String column;
    private final String field;
    private final boolean required;

    @JsonCreator
    public ColumnMapping(
            @JsonProperty("column") String column,
            @JsonProperty("field") String field,
            @JsonProperty("required") Boolean required) {
        this.column = column;
        this.field = field;
        this.required = required != null ? required : true; // Default to required
    }

    /**
     * The database column name (e.g., "employee_id", "first_name").
     */
    public String getColumn() {
        return column;
    }

    /**
     * The POJO field name (e.g., "employeeId", "firstName").
     */
    public String getField() {
        return field;
    }

    /**
     * Whether this column is required to be present in the ResultSet.
     * If true and column is missing, validation will fail.
     */
    public boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        return "ColumnMapping{" +
                "column='" + column + '\'' +
                ", field='" + field + '\'' +
                ", required=" + required +
                '}';
    }
}
