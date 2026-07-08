package com.reporting.framework.metadata.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Utility class for mapping SQL data types to Java classes.
 * <p>
 * Provides a centralized mapping from SQL Server type names (as strings)
 * to their corresponding Java class types. This is used for parameter
 * validation and type conversion during stored procedure execution.
 * </p>
 */
public class SqlTypeMapper {

    /**
     * Mapping from SQL type names to Java classes.
     */
    private static final Map<String, Class<?>> SQL_TO_JAVA = Map.ofEntries(
        Map.entry("INTEGER", Integer.class),
        Map.entry("BIGINT", Long.class),
        Map.entry("VARCHAR", String.class),
        Map.entry("NVARCHAR", String.class),
        Map.entry("DECIMAL", BigDecimal.class),
        Map.entry("NUMERIC", BigDecimal.class),
        Map.entry("DATE", LocalDate.class),
        Map.entry("DATETIME", LocalDateTime.class),
        Map.entry("DATETIME2", LocalDateTime.class),
        Map.entry("BIT", Boolean.class)
    );

    /**
     * Converts a SQL type string to its corresponding Java class.
     * <p>
     * The conversion is case-insensitive (e.g., "integer", "INTEGER", "Integer"
     * all map to Integer.class).
     * </p>
     *
     * @param sqlType the SQL type name (e.g., "INTEGER", "VARCHAR")
     * @return the corresponding Java class
     * @throws IllegalArgumentException if the SQL type is not supported
     */
    public static Class<?> toJavaClass(String sqlType) {
        if (sqlType == null || sqlType.isEmpty()) {
            throw new IllegalArgumentException("SQL type cannot be null or empty");
        }

        Class<?> javaClass = SQL_TO_JAVA.get(sqlType.toUpperCase());
        if (javaClass == null) {
            throw new IllegalArgumentException(
                "Unsupported SQL type: '" + sqlType + "'. Supported types: " +
                String.join(", ", SQL_TO_JAVA.keySet())
            );
        }
        return javaClass;
    }

    /**
     * Checks if a SQL type is supported.
     *
     * @param sqlType the SQL type name to check
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(String sqlType) {
        return sqlType != null && SQL_TO_JAVA.containsKey(sqlType.toUpperCase());
    }

    /**
     * Utility class - private constructor to prevent instantiation.
     */
    private SqlTypeMapper() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
