package com.reporting.framework.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents metadata for a stored procedure parameter (input or output).
 */
public class ParameterMetadata {

    private final String name;
    private final String fieldName;
    private final String type;
    private final boolean nullable;

    @JsonCreator
    public ParameterMetadata(
            @JsonProperty("name") String name,
            @JsonProperty("fieldName") String fieldName,
            @JsonProperty("type") String type,
            @JsonProperty("nullable") Boolean nullable) {
        this.name = name;
        this.fieldName = fieldName;
        this.type = type;
        this.nullable = nullable != null ? nullable : false;
    }

    public String getName() {
        return name;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getType() {
        return type;
    }

    public boolean isNullable() {
        return nullable;
    }

    // Legacy compatibility
    public boolean isRequired() {
        return !nullable;
    }

    public String getSqlType() {
        return type;
    }

    @Override
    public String toString() {
        return "ParameterMetadata{" +
                "name='" + name + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", type='" + type + '\'' +
                ", nullable=" + nullable +
                '}';
    }
}
