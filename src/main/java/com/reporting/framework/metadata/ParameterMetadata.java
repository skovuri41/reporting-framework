package com.reporting.framework.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents metadata for a stored procedure parameter (input or output).
 */
public class ParameterMetadata {

    private final String name;
    private final String sqlType;
    private final String javaType;
    private final boolean required;

    @JsonCreator
    public ParameterMetadata(
            @JsonProperty("name") String name,
            @JsonProperty("sqlType") String sqlType,
            @JsonProperty("javaType") String javaType,
            @JsonProperty("required") Boolean required) {
        this.name = name;
        this.sqlType = sqlType;
        this.javaType = javaType;
        this.required = required != null ? required : false;
    }

    public String getName() {
        return name;
    }

    public String getSqlType() {
        return sqlType;
    }

    public String getJavaType() {
        return javaType;
    }

    public boolean isRequired() {
        return required;
    }

    /**
     * Get the Java Class type for this parameter.
     */
    public Class<?> getJavaClass() throws ClassNotFoundException {
        return Class.forName(javaType);
    }

    @Override
    public String toString() {
        return "ParameterMetadata{" +
                "name='" + name + '\'' +
                ", sqlType='" + sqlType + '\'' +
                ", javaType='" + javaType + '\'' +
                ", required=" + required +
                '}';
    }
}
