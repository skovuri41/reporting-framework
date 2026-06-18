package com.reporting.framework.spark;

import org.apache.spark.sql.types.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for ResultSetSchemaInferrer.
 */
class ResultSetSchemaInferrerTest {

    @Test
    void testInferSchemaWithBasicTypes() throws SQLException {
        // Arrange
        ResultSetMetaData metadata = Mockito.mock(ResultSetMetaData.class);
        when(metadata.getColumnCount()).thenReturn(4);

        when(metadata.getColumnLabel(1)).thenReturn("id");
        when(metadata.getColumnType(1)).thenReturn(Types.INTEGER);
        when(metadata.isNullable(1)).thenReturn(ResultSetMetaData.columnNoNulls);

        when(metadata.getColumnLabel(2)).thenReturn("name");
        when(metadata.getColumnType(2)).thenReturn(Types.VARCHAR);
        when(metadata.isNullable(2)).thenReturn(ResultSetMetaData.columnNullable);

        when(metadata.getColumnLabel(3)).thenReturn("salary");
        when(metadata.getColumnType(3)).thenReturn(Types.BIGINT);
        when(metadata.isNullable(3)).thenReturn(ResultSetMetaData.columnNullable);

        when(metadata.getColumnLabel(4)).thenReturn("is_active");
        when(metadata.getColumnType(4)).thenReturn(Types.BOOLEAN);
        when(metadata.isNullable(4)).thenReturn(ResultSetMetaData.columnNoNulls);

        // Act
        StructType schema = ResultSetSchemaInferrer.inferSchema(metadata);

        // Assert
        assertThat(schema.fields()).hasSize(4);
        assertThat(schema.apply("id").dataType()).isEqualTo(DataTypes.IntegerType);
        assertThat(schema.apply("id").nullable()).isFalse();
        assertThat(schema.apply("name").dataType()).isEqualTo(DataTypes.StringType);
        assertThat(schema.apply("name").nullable()).isTrue();
        assertThat(schema.apply("salary").dataType()).isEqualTo(DataTypes.LongType);
        assertThat(schema.apply("is_active").dataType()).isEqualTo(DataTypes.BooleanType);
    }

    @Test
    void testInferSchemaWithDecimalType() throws SQLException {
        // Arrange
        ResultSetMetaData metadata = Mockito.mock(ResultSetMetaData.class);
        when(metadata.getColumnCount()).thenReturn(1);
        when(metadata.getColumnLabel(1)).thenReturn("amount");
        when(metadata.getColumnType(1)).thenReturn(Types.DECIMAL);
        when(metadata.getPrecision(1)).thenReturn(10);
        when(metadata.getScale(1)).thenReturn(2);
        when(metadata.isNullable(1)).thenReturn(ResultSetMetaData.columnNullable);

        // Act
        StructType schema = ResultSetSchemaInferrer.inferSchema(metadata);

        // Assert
        assertThat(schema.fields()).hasSize(1);
        DataType dataType = schema.apply("amount").dataType();
        assertThat(dataType).isInstanceOf(DecimalType.class);
        DecimalType decimalType = (DecimalType) dataType;
        assertThat(decimalType.precision()).isEqualTo(10);
        assertThat(decimalType.scale()).isEqualTo(2);
    }

    @Test
    void testInferSchemaWithDateAndTimestamp() throws SQLException {
        // Arrange
        ResultSetMetaData metadata = Mockito.mock(ResultSetMetaData.class);
        when(metadata.getColumnCount()).thenReturn(2);

        when(metadata.getColumnLabel(1)).thenReturn("hire_date");
        when(metadata.getColumnType(1)).thenReturn(Types.DATE);
        when(metadata.isNullable(1)).thenReturn(ResultSetMetaData.columnNullable);

        when(metadata.getColumnLabel(2)).thenReturn("created_at");
        when(metadata.getColumnType(2)).thenReturn(Types.TIMESTAMP);
        when(metadata.isNullable(2)).thenReturn(ResultSetMetaData.columnNullable);

        // Act
        StructType schema = ResultSetSchemaInferrer.inferSchema(metadata);

        // Assert
        assertThat(schema.fields()).hasSize(2);
        assertThat(schema.apply("hire_date").dataType()).isEqualTo(DataTypes.DateType);
        assertThat(schema.apply("created_at").dataType()).isEqualTo(DataTypes.TimestampType);
    }

    @Test
    void testInferSchemaWithFloatAndDouble() throws SQLException {
        // Arrange
        ResultSetMetaData metadata = Mockito.mock(ResultSetMetaData.class);
        when(metadata.getColumnCount()).thenReturn(2);

        when(metadata.getColumnLabel(1)).thenReturn("score");
        when(metadata.getColumnType(1)).thenReturn(Types.FLOAT);
        when(metadata.isNullable(1)).thenReturn(ResultSetMetaData.columnNullable);

        when(metadata.getColumnLabel(2)).thenReturn("rating");
        when(metadata.getColumnType(2)).thenReturn(Types.DOUBLE);
        when(metadata.isNullable(2)).thenReturn(ResultSetMetaData.columnNullable);

        // Act
        StructType schema = ResultSetSchemaInferrer.inferSchema(metadata);

        // Assert
        assertThat(schema.fields()).hasSize(2);
        assertThat(schema.apply("score").dataType()).isEqualTo(DataTypes.FloatType);
        assertThat(schema.apply("rating").dataType()).isEqualTo(DataTypes.DoubleType);
    }
}
