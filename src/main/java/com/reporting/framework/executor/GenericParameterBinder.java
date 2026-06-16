package com.reporting.framework.executor;

import com.reporting.framework.exception.ReportExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Generic parameter binder for CallableStatement.
 * Maps Java types to SQL types and binds parameters dynamically.
 */
public class GenericParameterBinder {

    private static final Logger logger = LoggerFactory.getLogger(GenericParameterBinder.class);

    /**
     * Bind an input parameter to a CallableStatement.
     *
     * @param stmt The CallableStatement
     * @param parameterName The parameter name
     * @param value The parameter value
     * @param sqlType The SQL type (e.g., "INTEGER", "VARCHAR", "DATE")
     * @param parameterIndex The parameter index (1-based)
     */
    public void bindInputParameter(CallableStatement stmt, String parameterName, Object value,
                                    String sqlType, int parameterIndex) throws SQLException {

        if (value == null) {
            int sqlTypeCode = getSqlTypeCode(sqlType);
            stmt.setNull(parameterIndex, sqlTypeCode);
            logger.debug("Bound NULL parameter: {} at index {}", parameterName, parameterIndex);
            return;
        }

        try {
            switch (sqlType.toUpperCase()) {
                case "INTEGER":
                case "INT":
                    stmt.setInt(parameterIndex, convertToInteger(value));
                    break;

                case "BIGINT":
                case "LONG":
                    stmt.setLong(parameterIndex, convertToLong(value));
                    break;

                case "SMALLINT":
                case "SHORT":
                    stmt.setShort(parameterIndex, convertToShort(value));
                    break;

                case "TINYINT":
                case "BYTE":
                    stmt.setByte(parameterIndex, convertToByte(value));
                    break;

                case "DECIMAL":
                case "NUMERIC":
                    stmt.setBigDecimal(parameterIndex, convertToBigDecimal(value));
                    break;

                case "FLOAT":
                case "REAL":
                    stmt.setFloat(parameterIndex, convertToFloat(value));
                    break;

                case "DOUBLE":
                    stmt.setDouble(parameterIndex, convertToDouble(value));
                    break;

                case "VARCHAR":
                case "CHAR":
                case "NVARCHAR":
                case "NCHAR":
                case "TEXT":
                case "STRING":
                    stmt.setString(parameterIndex, value.toString());
                    break;

                case "DATE":
                    stmt.setDate(parameterIndex, convertToSqlDate(value));
                    break;

                case "TIME":
                    stmt.setTime(parameterIndex, convertToSqlTime(value));
                    break;

                case "TIMESTAMP":
                case "DATETIME":
                case "DATETIME2":
                    stmt.setTimestamp(parameterIndex, convertToSqlTimestamp(value));
                    break;

                case "BIT":
                case "BOOLEAN":
                    stmt.setBoolean(parameterIndex, convertToBoolean(value));
                    break;

                default:
                    // Default: use setObject
                    stmt.setObject(parameterIndex, value);
                    logger.warn("Unknown SQL type '{}', using setObject for parameter: {}",
                            sqlType, parameterName);
            }

            logger.debug("Bound parameter: {} = {} (type: {}) at index {}",
                    parameterName, value, sqlType, parameterIndex);

        } catch (Exception e) {
            throw new ReportExecutionException(
                    String.format("Failed to bind parameter '%s' with value '%s' and type '%s'",
                            parameterName, value, sqlType), e);
        }
    }

    /**
     * Register an output parameter in a CallableStatement.
     *
     * @param stmt The CallableStatement
     * @param parameterName The parameter name
     * @param sqlType The SQL type
     * @param parameterIndex The parameter index (1-based)
     */
    public void registerOutputParameter(CallableStatement stmt, String parameterName,
                                        String sqlType, int parameterIndex) throws SQLException {
        int sqlTypeCode = getSqlTypeCode(sqlType);
        stmt.registerOutParameter(parameterIndex, sqlTypeCode);

        logger.debug("Registered output parameter: {} (type: {}) at index {}",
                parameterName, sqlType, parameterIndex);
    }

    /**
     * Get the output parameter value from a CallableStatement.
     *
     * @param stmt The CallableStatement
     * @param parameterName The parameter name
     * @param sqlType The SQL type
     * @param parameterIndex The parameter index (1-based)
     * @return The output parameter value
     */
    public Object getOutputParameter(CallableStatement stmt, String parameterName,
                                     String sqlType, int parameterIndex) throws SQLException {
        Object value;

        switch (sqlType.toUpperCase()) {
            case "INTEGER":
            case "INT":
                value = stmt.getInt(parameterIndex);
                break;

            case "BIGINT":
            case "LONG":
                value = stmt.getLong(parameterIndex);
                break;

            case "DECIMAL":
            case "NUMERIC":
                value = stmt.getBigDecimal(parameterIndex);
                break;

            case "VARCHAR":
            case "CHAR":
            case "NVARCHAR":
            case "NCHAR":
            case "TEXT":
            case "STRING":
                value = stmt.getString(parameterIndex);
                break;

            case "DATE":
                Date date = stmt.getDate(parameterIndex);
                value = date != null ? date.toLocalDate() : null;
                break;

            case "TIMESTAMP":
            case "DATETIME":
            case "DATETIME2":
                Timestamp ts = stmt.getTimestamp(parameterIndex);
                value = ts != null ? ts.toLocalDateTime() : null;
                break;

            case "BIT":
            case "BOOLEAN":
                value = stmt.getBoolean(parameterIndex);
                break;

            default:
                value = stmt.getObject(parameterIndex);
        }

        logger.debug("Retrieved output parameter: {} = {} (type: {})",
                parameterName, value, sqlType);

        return value;
    }

    /**
     * Get SQL type code from string type name.
     */
    private int getSqlTypeCode(String sqlType) {
        switch (sqlType.toUpperCase()) {
            case "INTEGER":
            case "INT":
                return Types.INTEGER;
            case "BIGINT":
            case "LONG":
                return Types.BIGINT;
            case "SMALLINT":
            case "SHORT":
                return Types.SMALLINT;
            case "TINYINT":
            case "BYTE":
                return Types.TINYINT;
            case "DECIMAL":
            case "NUMERIC":
                return Types.DECIMAL;
            case "FLOAT":
            case "REAL":
                return Types.FLOAT;
            case "DOUBLE":
                return Types.DOUBLE;
            case "VARCHAR":
            case "STRING":
                return Types.VARCHAR;
            case "CHAR":
                return Types.CHAR;
            case "NVARCHAR":
                return Types.NVARCHAR;
            case "NCHAR":
                return Types.NCHAR;
            case "TEXT":
                return Types.LONGVARCHAR;
            case "DATE":
                return Types.DATE;
            case "TIME":
                return Types.TIME;
            case "TIMESTAMP":
            case "DATETIME":
            case "DATETIME2":
                return Types.TIMESTAMP;
            case "BIT":
            case "BOOLEAN":
                return Types.BIT;
            default:
                return Types.OTHER;
        }
    }

    // Type conversion methods

    private Integer convertToInteger(Object value) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private Long convertToLong(Object value) {
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }

    private Short convertToShort(Object value) {
        if (value instanceof Short) return (Short) value;
        if (value instanceof Number) return ((Number) value).shortValue();
        return Short.parseShort(value.toString());
    }

    private Byte convertToByte(Object value) {
        if (value instanceof Byte) return (Byte) value;
        if (value instanceof Number) return ((Number) value).byteValue();
        return Byte.parseByte(value.toString());
    }

    private BigDecimal convertToBigDecimal(Object value) {
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return new BigDecimal(value.toString());
    }

    private Float convertToFloat(Object value) {
        if (value instanceof Float) return (Float) value;
        if (value instanceof Number) return ((Number) value).floatValue();
        return Float.parseFloat(value.toString());
    }

    private Double convertToDouble(Object value) {
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    private Boolean convertToBoolean(Object value) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        return Boolean.parseBoolean(value.toString());
    }

    private Date convertToSqlDate(Object value) {
        if (value instanceof Date) return (Date) value;
        if (value instanceof LocalDate) return Date.valueOf((LocalDate) value);
        if (value instanceof java.util.Date) return new Date(((java.util.Date) value).getTime());
        return Date.valueOf(LocalDate.parse(value.toString()));
    }

    private Time convertToSqlTime(Object value) {
        if (value instanceof Time) return (Time) value;
        if (value instanceof LocalTime) return Time.valueOf((LocalTime) value);
        return Time.valueOf(LocalTime.parse(value.toString()));
    }

    private Timestamp convertToSqlTimestamp(Object value) {
        if (value instanceof Timestamp) return (Timestamp) value;
        if (value instanceof LocalDateTime) return Timestamp.valueOf((LocalDateTime) value);
        if (value instanceof java.util.Date) return new Timestamp(((java.util.Date) value).getTime());
        return Timestamp.valueOf(LocalDateTime.parse(value.toString()));
    }
}
