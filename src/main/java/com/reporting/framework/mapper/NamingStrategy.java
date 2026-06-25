package com.reporting.framework.mapper;

import com.google.common.base.CaseFormat;

/**
 * Naming strategy for converting database column names to map keys.
 *
 * Uses Google Guava's CaseFormat for robust, well-tested conversions.
 *
 * Common use cases:
 * - NONE: Keep original column names (e.g., "EMPLOYEE_ID" stays "EMPLOYEE_ID")
 * - CAMEL_CASE: Convert to camelCase (e.g., "EMPLOYEE_ID" -> "employeeId")
 * - SNAKE_CASE: Convert to snake_case (e.g., "EmployeeID" -> "employee_id")
 * - PASCAL_CASE: Convert to PascalCase (e.g., "employee_id" -> "EmployeeId")
 * - LOWER_CASE: Convert to lowercase (e.g., "EMPLOYEE_ID" -> "employee_id")
 * - UPPER_CASE: Convert to uppercase (e.g., "employee_id" -> "EMPLOYEE_ID")
 */
public enum NamingStrategy {
    /**
     * Keep column names unchanged as they appear in the ResultSet.
     * Example: "EMPLOYEE_ID" -> "EMPLOYEE_ID"
     */
    NONE,

    /**
     * Convert to camelCase (first letter lowercase, subsequent words capitalized, no separators).
     * Example: "EMPLOYEE_ID" -> "employeeId", "first_name" -> "firstName"
     */
    CAMEL_CASE,

    /**
     * Convert to snake_case (all lowercase with underscores).
     * Example: "EmployeeID" -> "employee_id", "FirstName" -> "first_name"
     */
    SNAKE_CASE,

    /**
     * Convert to PascalCase (first letter uppercase, subsequent words capitalized, no separators).
     * Example: "employee_id" -> "EmployeeId", "first_name" -> "FirstName"
     */
    PASCAL_CASE,

    /**
     * Convert to lowercase with no transformations.
     * Example: "EMPLOYEE_ID" -> "employee_id"
     */
    LOWER_CASE,

    /**
     * Convert to UPPERCASE with no transformations.
     * Example: "employee_id" -> "EMPLOYEE_ID"
     */
    UPPER_CASE;

    /**
     * Apply this naming strategy to a column name.
     *
     * @param columnName The original column name from the database
     * @return The transformed column name according to this strategy
     */
    public String apply(String columnName) {
        if (columnName == null || columnName.isEmpty()) {
            return columnName;
        }

        switch (this) {
            case NONE:
                return columnName;

            case CAMEL_CASE:
                return convertTo(columnName, CaseFormat.LOWER_CAMEL);

            case SNAKE_CASE:
                return convertTo(columnName, CaseFormat.LOWER_UNDERSCORE);

            case PASCAL_CASE:
                return convertTo(columnName, CaseFormat.UPPER_CAMEL);

            case LOWER_CASE:
                return columnName.toLowerCase();

            case UPPER_CASE:
                return columnName.toUpperCase();

            default:
                return columnName;
        }
    }

    /**
     * Convert a column name to the target CaseFormat by detecting the source format.
     *
     * @param input The input string
     * @param targetFormat The target CaseFormat
     * @return The converted string
     */
    private static String convertTo(String input, CaseFormat targetFormat) {
        CaseFormat sourceFormat = detectFormat(input);

        try {
            return sourceFormat.to(targetFormat, input);
        } catch (Exception e) {
            // If conversion fails, return original string
            return input;
        }
    }

    /**
     * Detect the CaseFormat of a string.
     *
     * Detection rules:
     * - UPPER_UNDERSCORE: Contains _ and all letters are uppercase (e.g., EMPLOYEE_ID)
     *                     OR all uppercase with no underscores (e.g., ID, NAME)
     * - LOWER_UNDERSCORE: Contains _ and has lowercase letters (e.g., employee_id)
     * - UPPER_CAMEL: Starts with uppercase, has lowercase, no _ (e.g., EmployeeId)
     * - LOWER_CAMEL: Starts with lowercase, has uppercase letters, no _ (e.g., employeeId)
     */
    private static CaseFormat detectFormat(String input) {
        if (input.contains("_")) {
            // Has underscores - snake_case variant
            boolean hasLowercase = !input.equals(input.toUpperCase());
            return hasLowercase ? CaseFormat.LOWER_UNDERSCORE : CaseFormat.UPPER_UNDERSCORE;
        } else {
            // No underscores - check if all uppercase (like "ID", "NAME")
            if (input.equals(input.toUpperCase())) {
                // All uppercase - treat as UPPER_UNDERSCORE
                return CaseFormat.UPPER_UNDERSCORE;
            }

            // Mixed case - camel case variant
            if (input.isEmpty() || Character.isLowerCase(input.charAt(0))) {
                return CaseFormat.LOWER_CAMEL;
            } else {
                return CaseFormat.UPPER_CAMEL;
            }
        }
    }

}
