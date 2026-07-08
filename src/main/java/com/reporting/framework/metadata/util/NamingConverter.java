package com.reporting.framework.metadata.util;

import com.google.common.base.CaseFormat;

/**
 * Utility class for converting database naming conventions to camelCase.
 * <p>
 * Automatically detects the input format (snake_case, UPPER_SNAKE_CASE,
 * PascalCase, or already camelCase) and converts to camelCase for consistent
 * JSON API naming.
 * </p>
 * <p>
 * This converter is used when loading metadata from the database to ensure
 * parameter names and column display names follow Java/JavaScript camelCase
 * conventions, regardless of database naming style.
 * </p>
 */
public class NamingConverter {

    /**
     * Converts a database name to camelCase format.
     * <p>
     * Conversion rules:
     * <ul>
     *   <li>snake_case or UPPER_SNAKE_CASE → camelCase (e.g., "employee_id" → "employeeId")</li>
     *   <li>PascalCase → camelCase (e.g., "EmployeeId" → "employeeId")</li>
     *   <li>Already camelCase → unchanged (e.g., "employeeId" → "employeeId")</li>
     *   <li>Numbers preserved (e.g., "PARAM_1_VALUE" → "param1Value")</li>
     * </ul>
     * </p>
     *
     * @param name the database name to convert (may be null or empty)
     * @return the converted camelCase name, or the original if null/empty
     */
    public static String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Detect and convert snake_case or UPPER_SNAKE_CASE
        if (name.contains("_")) {
            // Convert to upper snake case first to normalize, then to camelCase
            return CaseFormat.UPPER_UNDERSCORE.to(
                CaseFormat.LOWER_CAMEL,
                name.toUpperCase()
            );
        }

        // Detect and convert PascalCase (including single uppercase letters)
        if (Character.isUpperCase(name.charAt(0))) {
            // First character is uppercase - convert to camelCase
            if (name.length() == 1) {
                return Character.toLowerCase(name.charAt(0)) + "";
            }
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }

        // Already camelCase or lowercase - return as-is
        return name;
    }

    /**
     * Utility class - private constructor to prevent instantiation.
     */
    private NamingConverter() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
