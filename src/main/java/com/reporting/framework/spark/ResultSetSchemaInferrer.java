package com.reporting.framework.spark;

import org.apache.spark.sql.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Infers Spark schema from JDBC ResultSet metadata.
 * Maps SQL types to Spark DataTypes.
 */
public class ResultSetSchemaInferrer {

    private static final Logger logger = LoggerFactory.getLogger(ResultSetSchemaInferrer.class);

    /**
     * Infer Spark StructType schema from ResultSet metadata.
     *
     * @param metadata JDBC ResultSetMetaData
     * @return Spark StructType schema
     * @throws SQLException if metadata cannot be read
     */
    public static StructType inferSchema(ResultSetMetaData metadata) throws SQLException {
        int columnCount = metadata.getColumnCount();
        List<StructField> fields = new ArrayList<>(columnCount);

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metadata.getColumnLabel(i);
            int sqlType = metadata.getColumnType(i);
            boolean nullable = metadata.isNullable(i) != ResultSetMetaData.columnNoNulls;

            DataType dataType = mapSqlTypeToSparkType(sqlType, metadata, i);

            fields.add(DataTypes.createStructField(columnName, dataType, nullable));

            logger.debug("Mapped column {} (SQL type {}) to Spark type {}, nullable={}",
                        columnName, sqlType, dataType, nullable);
        }

        return DataTypes.createStructType(fields);
    }

    /**
     * Map SQL type to Spark DataType.
     *
     * @param sqlType SQL type code from java.sql.Types
     * @param metadata ResultSetMetaData for precision/scale information
     * @param columnIndex 1-based column index
     * @return Corresponding Spark DataType
     * @throws SQLException if metadata cannot be read
     */
    private static DataType mapSqlTypeToSparkType(int sqlType, ResultSetMetaData metadata, int columnIndex)
            throws SQLException {
        switch (sqlType) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.TINYINT:
                return DataTypes.IntegerType;

            case Types.BIGINT:
                return DataTypes.LongType;

            case Types.DECIMAL:
            case Types.NUMERIC:
                int precision = metadata.getPrecision(columnIndex);
                int scale = metadata.getScale(columnIndex);
                // Use default precision/scale if not specified
                if (precision == 0) {
                    precision = 10;
                }
                return DataTypes.createDecimalType(precision, scale);

            case Types.FLOAT:
            case Types.REAL:
                return DataTypes.FloatType;

            case Types.DOUBLE:
                return DataTypes.DoubleType;

            case Types.VARCHAR:
            case Types.CHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
            case Types.NCLOB:
                return DataTypes.StringType;

            case Types.DATE:
                return DataTypes.DateType;

            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return DataTypes.TimestampType;

            case Types.BOOLEAN:
            case Types.BIT:
                return DataTypes.BooleanType;

            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                return DataTypes.BinaryType;

            default:
                logger.warn("Unknown SQL type {}, defaulting to StringType", sqlType);
                return DataTypes.StringType;
        }
    }
}
