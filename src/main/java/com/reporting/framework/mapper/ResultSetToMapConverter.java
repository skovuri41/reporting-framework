package com.reporting.framework.mapper;

import com.reporting.framework.exception.ReportExecutionException;
import com.reporting.framework.metadata.ColumnMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Converts JDBC ResultSet to List of Maps using explicit column mappings.
 * Provides clear, self-documenting mapping without relying on naming conventions.
 */
public class ResultSetToMapConverter {

    private static final Logger logger = LoggerFactory.getLogger(ResultSetToMapConverter.class);

    /**
     * Convert a ResultSet to a List of Maps using explicit column mappings.
     *
     * @param resultSet The ResultSet to convert
     * @param columnMappings Explicit column-to-field mappings
     * @return List of Maps representing the rows
     */
    public List<Map<String, Object>> convert(ResultSet resultSet, List<ColumnMapping> columnMappings) {
        if (columnMappings == null || columnMappings.isEmpty()) {
            throw new ReportExecutionException("Column mappings cannot be null or empty");
        }

        List<Map<String, Object>> rows = new ArrayList<>();

        try {
            ResultSetMetaData metaData = resultSet.getMetaData();

            // Build a map of available columns in ResultSet (case-insensitive)
            Map<String, Integer> availableColumns = new HashMap<>();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnLabel = metaData.getColumnLabel(i);
                availableColumns.put(columnLabel.toLowerCase(), i);
            }

            // Validate required columns exist
            validateRequiredColumns(columnMappings, availableColumns);

            // Create mapping: column name -> ColumnMapping
            Map<String, ColumnMapping> mappingsByColumn = columnMappings.stream()
                    .collect(Collectors.toMap(
                            cm -> cm.getColumn().toLowerCase(),
                            cm -> cm
                    ));

            // Process each row
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();

                // Map each configured column
                for (ColumnMapping mapping : columnMappings) {
                    String columnName = mapping.getColumn();
                    Integer columnIndex = availableColumns.get(columnName.toLowerCase());

                    if (columnIndex != null) {
                        int sqlType = metaData.getColumnType(columnIndex);
                        Object value = getColumnValue(resultSet, columnIndex, sqlType);
                        row.put(mapping.getField(), value);
                    } else if (mapping.isRequired()) {
                        // This should have been caught in validation, but double-check
                        throw new ReportExecutionException(
                                "Required column not found in ResultSet: " + columnName);
                    }
                }

                rows.add(row);
            }

            logger.debug("Converted {} rows from ResultSet to Maps using {} column mappings",
                    rows.size(), columnMappings.size());
            return rows;

        } catch (SQLException e) {
            logger.error("Failed to convert ResultSet to Maps", e);
            throw new ReportExecutionException("Failed to convert ResultSet to Maps", e);
        }
    }

    /**
     * Validate that all required columns exist in the ResultSet.
     */
    private void validateRequiredColumns(List<ColumnMapping> columnMappings,
                                         Map<String, Integer> availableColumns) {
        List<String> missingColumns = new ArrayList<>();

        for (ColumnMapping mapping : columnMappings) {
            if (mapping.isRequired()) {
                String columnName = mapping.getColumn().toLowerCase();
                if (!availableColumns.containsKey(columnName)) {
                    missingColumns.add(mapping.getColumn());
                }
            }
        }

        if (!missingColumns.isEmpty()) {
            String message = String.format(
                    "Required columns not found in ResultSet: %s. Available columns: %s",
                    missingColumns,
                    availableColumns.keySet()
            );
            logger.error(message);
            throw new ReportExecutionException(message);
        }
    }

    /**
     * Convert a ResultSet to a List of Maps by auto-inferring all columns.
     * This method does NOT require explicit column mappings - it automatically
     * discovers and maps all columns from the ResultSet.
     *
     * This is ideal for dynamic reporting where you don't want to maintain
     * explicit column mappings in metadata.
     *
     * @param resultSet The ResultSet to convert
     * @return List of Maps representing the rows (all columns included)
     */
    public List<Map<String, Object>> convertToMaps(ResultSet resultSet) {
        List<Map<String, Object>> rows = new ArrayList<>();

        try {
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // Auto-infer column names and types
            String[] columnNames = new String[columnCount];
            int[] sqlTypes = new int[columnCount];

            for (int i = 1; i <= columnCount; i++) {
                columnNames[i - 1] = metaData.getColumnLabel(i);
                sqlTypes[i - 1] = metaData.getColumnType(i);
            }

            logger.debug("Auto-inferred {} columns from ResultSet: {}",
                    columnCount, Arrays.toString(columnNames));

            // Process each row
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = columnNames[i - 1];
                    int sqlType = sqlTypes[i - 1];
                    Object value = getColumnValue(resultSet, i, sqlType);
                    row.put(columnName, value);
                }

                rows.add(row);
            }

            logger.debug("Converted {} rows from ResultSet to Maps (auto-inferred columns)",
                    rows.size());
            return rows;

        } catch (SQLException e) {
            logger.error("Failed to convert ResultSet to Maps (auto-inferred)", e);
            throw new ReportExecutionException("Failed to convert ResultSet to Maps", e);
        }
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
