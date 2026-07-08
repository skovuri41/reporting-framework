package com.reporting.framework.metadata.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enumeration for parameter direction in stored procedure execution.
 * <p>
 * Defines whether a parameter is an input to the stored procedure or
 * an output returned by the stored procedure.
 * </p>
 * <p>
 * JSON serialization uses "Input" and "Output" string values to maintain
 * consistency with the metadata JSON schema.
 * </p>
 */
public enum ParameterDirection {
    /**
     * Input parameter - value provided to the stored procedure
     */
    INPUT,

    /**
     * Output parameter - value returned by the stored procedure
     */
    OUTPUT;

    /**
     * Deserializes from JSON string value to enum constant.
     * <p>
     * Accepts "Input" or "Output" (case-sensitive) and maps to the
     * corresponding enum value.
     * </p>
     *
     * @param value the JSON string value ("Input" or "Output")
     * @return the corresponding ParameterDirection enum constant
     * @throws IllegalArgumentException if value is not "Input" or "Output"
     */
    @JsonCreator
    public static ParameterDirection fromString(String value) {
        if ("Input".equals(value)) {
            return INPUT;
        }
        if ("Output".equals(value)) {
            return OUTPUT;
        }
        throw new IllegalArgumentException(
            "Invalid parameter direction: '" + value + "'. Expected 'Input' or 'Output'."
        );
    }

    /**
     * Serializes enum constant to JSON string value.
     *
     * @return "Input" for INPUT, "Output" for OUTPUT
     */
    @JsonValue
    public String toJson() {
        return this == INPUT ? "Input" : "Output";
    }
}
