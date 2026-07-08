package com.reporting.framework.metadata.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Immutable DTO representing a stored procedure parameter.
 * <p>
 * Defines an input or output parameter with its name, data type, direction,
 * and nullability constraints. The parameter name is automatically converted
 * to camelCase when loaded from the database.
 * </p>
 * <p>
 * This class follows the immutability principle - all fields are final and
 * there are no setter methods.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Parameter {
    private final String parameterName;
    private final String dataType;
    private final ParameterDirection parameterDirection;
    private final Boolean nullable;

    /**
     * Constructs an immutable Parameter instance.
     * <p>
     * This constructor is used by Jackson for JSON deserialization.
     * </p>
     *
     * @param parameterName the name of the parameter (camelCase format)
     * @param dataType the SQL data type (e.g., "INTEGER", "VARCHAR")
     * @param parameterDirection the direction (Input or Output)
     * @param nullable whether the parameter accepts null values
     * @throws NullPointerException if any required field is null
     */
    @JsonCreator
    public Parameter(
        @JsonProperty("parameterName") String parameterName,
        @JsonProperty("dataType") String dataType,
        @JsonProperty("parameterDirection") ParameterDirection parameterDirection,
        @JsonProperty("nullable") Boolean nullable
    ) {
        this.parameterName = Objects.requireNonNull(parameterName, "parameterName cannot be null");
        this.dataType = Objects.requireNonNull(dataType, "dataType cannot be null");
        this.parameterDirection = Objects.requireNonNull(parameterDirection, "parameterDirection cannot be null");
        this.nullable = Objects.requireNonNull(nullable, "nullable cannot be null");

        if (parameterName.trim().isEmpty()) {
            throw new IllegalArgumentException("parameterName cannot be empty");
        }
        if (dataType.trim().isEmpty()) {
            throw new IllegalArgumentException("dataType cannot be empty");
        }
    }

    /**
     * Returns the parameter name in camelCase format.
     * <p>
     * This name is automatically converted from database naming conventions
     * (snake_case, UPPER_CASE, etc.) to camelCase when metadata is loaded.
     * </p>
     *
     * @return the parameter name
     */
    public String getParameterName() {
        return parameterName;
    }

    /**
     * Returns the SQL data type of the parameter.
     *
     * @return the data type (e.g., "INTEGER", "VARCHAR", "DECIMAL")
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Returns the parameter direction.
     *
     * @return INPUT or OUTPUT
     */
    public ParameterDirection getParameterDirection() {
        return parameterDirection;
    }

    /**
     * Returns whether the parameter accepts null values.
     *
     * @return true if nullable, false otherwise
     */
    public Boolean getNullable() {
        return nullable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parameter parameter = (Parameter) o;
        return Objects.equals(parameterName, parameter.parameterName) &&
               Objects.equals(dataType, parameter.dataType) &&
               parameterDirection == parameter.parameterDirection &&
               Objects.equals(nullable, parameter.nullable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterName, dataType, parameterDirection, nullable);
    }

    @Override
    public String toString() {
        return "Parameter{" +
               "parameterName='" + parameterName + '\'' +
               ", dataType='" + dataType + '\'' +
               ", parameterDirection=" + parameterDirection +
               ", nullable=" + nullable +
               '}';
    }
}
