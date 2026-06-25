package com.reporting.framework.mapper;

/**
 * Naming strategy for converting database column names to map keys.
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
                return toCamelCase(columnName);

            case SNAKE_CASE:
                return toSnakeCase(columnName);

            case PASCAL_CASE:
                return toPascalCase(columnName);

            case LOWER_CASE:
                return columnName.toLowerCase();

            case UPPER_CASE:
                return columnName.toUpperCase();

            default:
                return columnName;
        }
    }

    /**
     * Convert a string to camelCase.
     * Handles: UPPER_SNAKE_CASE, snake_case, PascalCase, camelCase
     */
    private static String toCamelCase(String input) {
        // Already camelCase or single word
        if (!input.contains("_") && !input.equals(input.toUpperCase())) {
            // If it's PascalCase, lowercase the first letter
            if (Character.isUpperCase(input.charAt(0))) {
                return Character.toLowerCase(input.charAt(0)) + input.substring(1);
            }
            return input;
        }

        // Split by underscore and process each word
        String[] parts = input.toLowerCase().split("_");
        StringBuilder result = new StringBuilder(parts[0]); // First word stays lowercase

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)));
                if (parts[i].length() > 1) {
                    result.append(parts[i].substring(1));
                }
            }
        }

        return result.toString();
    }

    /**
     * Convert a string to snake_case.
     * Handles: camelCase, PascalCase, UPPER_SNAKE_CASE
     */
    private static String toSnakeCase(String input) {
        // Already snake_case
        if (input.contains("_") && input.equals(input.toLowerCase())) {
            return input;
        }

        // Already UPPER_SNAKE_CASE
        if (input.contains("_") && input.equals(input.toUpperCase())) {
            return input.toLowerCase();
        }

        // Convert camelCase or PascalCase to snake_case
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (Character.isUpperCase(c)) {
                // Add underscore before uppercase letter, but not at the start
                // and not if previous character was already uppercase
                if (i > 0 && Character.isLowerCase(input.charAt(i - 1))) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Convert a string to PascalCase.
     * Handles: UPPER_SNAKE_CASE, snake_case, camelCase
     */
    private static String toPascalCase(String input) {
        String camelCase = toCamelCase(input);
        return Character.toUpperCase(camelCase.charAt(0)) + camelCase.substring(1);
    }
}
