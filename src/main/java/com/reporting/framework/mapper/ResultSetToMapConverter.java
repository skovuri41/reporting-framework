package com.reporting.framework.mapper;

import com.reporting.framework.exception.ReportExecutionException;
import com.reporting.framework.metadata.StoredProcedureMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts JDBC ResultSet to List of Maps.
 * Supports metadata-based column mapping and naming strategies.
 */
public class ResultSetToMapConverter {

    private static final Logger logger = LoggerFactory.getLogger(ResultSetToMapConverter.class);

    /**
     * Convert a ResultSet to a List of Maps by auto-inferring all columns
     * with a specific naming strategy.
     *
     * This method applies naming conventions to transform database column names:
     * - CAMEL_CASE: "EMPLOYEE_ID" -> "employeeId"
     * - SNAKE_CASE: "EmployeeID" -> "employee_id"
     * - PASCAL_CASE: "employee_id" -> "EmployeeId"
     * - NONE: Keep original column names
     *
     * @param resultSet The ResultSet to convert
     * @param namingStrategy The naming strategy to apply to column names
     * @return List of Maps with transformed column names
     */
    public List<Map<String, Object>> convertToMaps(ResultSet resultSet, NamingStrategy namingStrategy) {
        if (namingStrategy == null) {
            namingStrategy = NamingStrategy.NONE;
        }

        List<Map<String, Object>> rows = new ArrayList<>();

        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Auto-infer column names and apply naming strategy
            String[] originalColumnNames = new String[columnCount];
            String[] transformedColumnNames = new String[columnCount];
            int[] sqlTypes = new int[columnCount];

            for (int i = 1; i <= columnCount; i++) {
                String originalName = metaData.getColumnLabel(i);
                originalColumnNames[i - 1] = originalName;
                transformedColumnNames[i - 1] = namingStrategy.apply(originalName);
                sqlTypes[i - 1] = metaData.getColumnType(i);
            }

            logger.debug("Auto-inferred {} columns with naming strategy {}: {} -> {}",
                    columnCount, namingStrategy,
                    Arrays.toString(originalColumnNames),
                    Arrays.toString(transformedColumnNames));

            // Process each row
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = transformedColumnNames[i - 1];
                    int sqlType = sqlTypes[i - 1];
                    Object value = getColumnValue(resultSet, i, sqlType);
                    row.put(columnName, value);
                }

                rows.add(row);
            }

            logger.debug("Converted {} rows from ResultSet to Maps with naming strategy {}",
                    rows.size(), namingStrategy);
            return rows;

        } catch (SQLException e) {
            logger.error("Failed to convert ResultSet to Maps with naming strategy", e);
            throw new ReportExecutionException("Failed to convert ResultSet to Maps", e);
        }
    }

    /**
     * Convert a ResultSet to a List of Maps using column metadata from stored procedure.
     * Maps SQL column names to camelCase field names based on metadata.
     *
     * @param resultSet The ResultSet to convert
     * @param metadata Stored procedure metadata containing column mappings
     * @return List of Maps with camelCase field names
     */
    public List<Map<String, Object>> convertToMaps(ResultSet resultSet, StoredProcedureMetadata metadata) {
        List<Map<String, Object>> rows = new ArrayList<>();

        try {
            ResultSetMetaData rsMetadata = resultSet.getMetaData();
            int columnCount = rsMetadata.getColumnCount();

            // Build column mapping: SQL column name -> camelCase field name
            Map<String, String> columnToFieldMap = metadata.getFieldNameMapping();

            logger.debug("Using metadata column mappings for {} columns", columnToFieldMap.size());

            // Process each row
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String sqlColumnName = rsMetadata.getColumnName(i);
                    int sqlType = rsMetadata.getColumnType(i);

                    // Map to camelCase field name
                    String fieldName = columnToFieldMap.getOrDefault(sqlColumnName, sqlColumnName);

                    Object value = getColumnValue(resultSet, i, sqlType);
                    row.put(fieldName, value);
                }

                rows.add(row);
            }

            logger.debug("Converted {} rows with camelCase field names using metadata", rows.size());
            return rows;

        } catch (SQLException e) {
            logger.error("Failed to convert ResultSet using metadata", e);
            throw new ReportExecutionException("Failed to convert ResultSet using metadata", e);
        }
    }

    /**
     * Convert a ResultSet to a List of Maps by auto-inferring all columns.
     * This method does NOT require explicit column mappings - it automatically
     * discovers and maps all columns from the ResultSet.
     *
     * Column names are kept as-is from the database (typically UPPER_SNAKE_CASE).
     *
     * This is ideal for dynamic reporting where you don't want to maintain
     * explicit column mappings in metadata.
     *
     * @param resultSet The ResultSet to convert
     * @return List of Maps representing the rows (all columns included)
     */
    public List<Map<String, Object>> convertToMaps(ResultSet resultSet) {
        return convertToMaps(resultSet, NamingStrategy.NONE);
    }

    /**
     * Get the column value with proper type conversion.
     */
    private Object getColumnValue(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        Object value = rs.getObject(columnIndex);

        if (value == null) {
            return null;
        }

        // Convert SQL types to appropriate Java types
        switch (sqlType) {
            case Types.DATE:
                java.sql.Date date = rs.getDate(columnIndex);
                return date != null ? date.toLocalDate() : null;

            case Types.TIME:
                java.sql.Time time = rs.getTime(columnIndex);
                return time != null ? time.toLocalTime() : null;

            case Types.TIMESTAMP:
                java.sql.Timestamp timestamp = rs.getTimestamp(columnIndex);
                return timestamp != null ? timestamp.toLocalDateTime() : null;

            case Types.DECIMAL:
            case Types.NUMERIC:
                return rs.getBigDecimal(columnIndex);

            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                return rs.getInt(columnIndex);

            case Types.BIGINT:
                return rs.getLong(columnIndex);

            case Types.REAL:
            case Types.FLOAT:
                return rs.getFloat(columnIndex);

            case Types.DOUBLE:
                return rs.getDouble(columnIndex);

            case Types.BIT:
            case Types.BOOLEAN:
                return rs.getBoolean(columnIndex);

            case Types.VARCHAR:
            case Types.CHAR:
            case Types.LONGVARCHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
            case Types.LONGNVARCHAR:
                return rs.getString(columnIndex);

            default:
                return value;
        }
    }
}
