package com.reporting.framework.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SparkOperations utility methods.
 */
class SparkOperationsTest extends SparkTestBase {

    @Test
    void testFilter() {
        // Arrange
        Dataset<Row> dataset = createTestDataset();

        // Act
        Dataset<Row> filtered = SparkOperations.filter(dataset, "salary > 70000");

        // Assert
        assertThat(filtered.count()).isEqualTo(2);
        List<Row> rows = filtered.collectAsList();
        String name = rows.get(0).getAs("name");
        assertThat(name).isIn("Alice", "Charlie");
    }

    @Test
    void testSelect() {
        // Arrange
        Dataset<Row> dataset = createTestDataset();

        // Act
        Dataset<Row> selected = SparkOperations.select(dataset, "name", "salary");

        // Assert
        String[] columns = selected.columns();
        assertThat(columns).hasSize(2);
        assertThat(columns).containsExactly("name", "salary");
    }

    @Test
    void testOrderBy() {
        // Arrange
        Dataset<Row> dataset = createTestDataset();

        // Act
        Dataset<Row> ordered = SparkOperations.orderBy(dataset, "salary");

        // Assert
        List<Row> rows = ordered.collectAsList();
        assertThat((String) rows.get(0).getAs("name")).isEqualTo("Bob");
        assertThat((String) rows.get(2).getAs("name")).isEqualTo("Charlie");
    }

    @Test
    void testOrderByDesc() {
        // Arrange
        Dataset<Row> dataset = createTestDataset();

        // Act
        Dataset<Row> ordered = SparkOperations.orderByDesc(dataset, "salary");

        // Assert
        List<Row> rows = ordered.collectAsList();
        assertThat((String) rows.get(0).getAs("name")).isEqualTo("Charlie");
        assertThat((String) rows.get(2).getAs("name")).isEqualTo("Bob");
    }

    @Test
    void testLimit() {
        // Arrange
        Dataset<Row> dataset = createTestDataset();

        // Act
        Dataset<Row> limited = SparkOperations.limit(dataset, 2);

        // Assert
        assertThat(limited.count()).isEqualTo(2);
    }

    @Test
    void testGroupBy() {
        // Arrange - Create dataset with department column
        StructType schema = DataTypes.createStructType(new StructField[] {
            DataTypes.createStructField("department", DataTypes.StringType, false),
            DataTypes.createStructField("salary", DataTypes.IntegerType, false)
        });

        List<Row> rows = new ArrayList<>();
        rows.add(RowFactory.create("IT", 80000));
        rows.add(RowFactory.create("IT", 90000));
        rows.add(RowFactory.create("HR", 70000));

        Dataset<Row> dataset = spark.createDataFrame(rows, schema);

        // Act
        Dataset<Row> grouped = SparkOperations.groupBy(
            dataset,
            new String[]{"department"},
            Map.of("salary", "avg")
        );

        // Assert
        assertThat(grouped.count()).isEqualTo(2);
        List<Row> result = grouped.collectAsList();
        assertThat(result).hasSize(2);
    }

    @Test
    void testWithColumn() {
        // Arrange
        Dataset<Row> dataset = createTestDataset();

        // Act
        Dataset<Row> withNewColumn = SparkOperations.withColumn(
            dataset,
            "double_salary",
            "salary * 2"
        );

        // Assert
        String[] columns = withNewColumn.columns();
        assertThat(columns).contains("double_salary");
        List<Row> rows = withNewColumn.collectAsList();
        assertThat(rows.get(0).<Integer>getAs("double_salary")).isEqualTo(160000);
    }

    @Test
    void testDrop() {
        // Arrange
        Dataset<Row> dataset = createTestDataset();

        // Act
        Dataset<Row> dropped = SparkOperations.drop(dataset, "salary");

        // Assert
        String[] columns = dropped.columns();
        assertThat(columns).hasSize(2);
        assertThat(columns).containsExactly("id", "name");
    }

    @Test
    void testDistinct() {
        // Arrange - Create dataset with duplicates
        StructType schema = DataTypes.createStructType(new StructField[] {
            DataTypes.createStructField("value", DataTypes.IntegerType, false)
        });

        List<Row> rows = new ArrayList<>();
        rows.add(RowFactory.create(1));
        rows.add(RowFactory.create(2));
        rows.add(RowFactory.create(1));

        Dataset<Row> dataset = spark.createDataFrame(rows, schema);

        // Act
        Dataset<Row> distinct = SparkOperations.distinct(dataset);

        // Assert
        assertThat(distinct.count()).isEqualTo(2);
    }

    @Test
    void testUnion() {
        // Arrange
        Dataset<Row> dataset1 = createTestDataset();
        Dataset<Row> dataset2 = createTestDataset();

        // Act
        Dataset<Row> union = SparkOperations.union(dataset1, dataset2);

        // Assert
        assertThat(union.count()).isEqualTo(6);
    }
}
