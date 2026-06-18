package com.reporting.framework.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts JDBC ResultSet to Spark Dataset.
 * Handles schema inference and type-safe value extraction.
 */
public class SparkDatasetConverter {

    private static final Logger logger = LoggerFactory.getLogger(SparkDatasetConverter.class);

    /**
     * Convert ResultSet to Spark Dataset.
     *
     * @param rs JDBC ResultSet (will be closed after conversion)
     * @param spark SparkSession
     * @return Spark Dataset with inferred schema
     * @throws SQLException if ResultSet cannot be read
     */
    public static Dataset<Row> convert(ResultSet rs, SparkSession spark) throws SQLException {
        try {
            ResultSetMetaData metadata = rs.getMetaData();

            // Infer schema from metadata
            StructType schema = ResultSetSchemaInferrer.inferSchema(metadata);
            logger.debug("Inferred schema: {}", schema);

            // Extract rows
            List<Row> rows = extractRows(rs, metadata);
            logger.info("Converted {} rows from ResultSet to Dataset", rows.size());

            // Create Dataset
            return spark.createDataFrame(rows, schema);

        } finally {
            // Always close ResultSet
            if (rs != null && !rs.isClosed()) {
                rs.close();
            }
        }
    }

    /**
     * Extract all rows from ResultSet.
     *
     * @param rs ResultSet
     * @param metadata ResultSetMetaData
     * @return List of Spark Rows
     * @throws SQLException if ResultSet cannot be read
     */
    private static List<Row> extractRows(ResultSet rs, ResultSetMetaData metadata) throws SQLException {
        List<Row> rows = new ArrayList<>();
        int columnCount = metadata.getColumnCount();

        while (rs.next()) {
            Object[] values = new Object[columnCount];

            for (int i = 1; i <= columnCount; i++) {
                values[i - 1] = extractValue(rs, i, metadata.getColumnType(i));
            }

            rows.add(RowFactory.create(values));
        }

        return rows;
    }

    /**
     * Extract a single value from ResultSet with proper type handling.
     *
     * @param rs ResultSet
     * @param columnIndex 1-based column index
     * @param sqlType SQL type code
     * @return Extracted value (null if SQL NULL)
     * @throws SQLException if value cannot be read
     */
    private static Object extractValue(ResultSet rs, int columnIndex, int sqlType) throws SQLException {
        switch (sqlType) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                int intValue = rs.getInt(columnIndex);
                return rs.wasNull() ? null : intValue;

            case Types.BIGINT:
                long longValue = rs.getLong(columnIndex);
                return rs.wasNull() ? null : longValue;

            case Types.DECIMAL:
            case Types.NUMERIC:
                return rs.getBigDecimal(columnIndex);

            case Types.FLOAT:
            case Types.REAL:
                float floatValue = rs.getFloat(columnIndex);
                return rs.wasNull() ? null : floatValue;

            case Types.DOUBLE:
                double doubleValue = rs.getDouble(columnIndex);
                return rs.wasNull() ? null : doubleValue;

            case Types.VARCHAR:
            case Types.CHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
            case Types.NCLOB:
                return rs.getString(columnIndex);

            case Types.DATE:
                return rs.getDate(columnIndex);

            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return rs.getTimestamp(columnIndex);

            case Types.BOOLEAN:
            case Types.BIT:
                boolean boolValue = rs.getBoolean(columnIndex);
                return rs.wasNull() ? null : boolValue;

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                return rs.getBytes(columnIndex);

            default:
                // Fallback to string representation
                return rs.getString(columnIndex);
        }
    }
}
