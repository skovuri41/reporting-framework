package com.reporting.framework.spark;

import com.reporting.framework.spark.SparkConfig;
import com.reporting.framework.spark.SparkSessionManager;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for Spark tests.
 * Sets up and tears down SparkSession in local mode.
 */
public abstract class SparkTestBase {

    protected static SparkSession spark;

    @BeforeAll
    public static void setupSpark() {
        // Workaround for Hadoop UserGroupInformation issue in tests
        System.setProperty("hadoop.home.dir", "/");

        SparkConfig config = SparkConfig.builder()
            .appName("Reporting Framework Test")
            .master("local[1]")
            .sparkProperty("spark.ui.enabled", "false")
            .sparkProperty("spark.sql.shuffle.partitions", "1")
            .build();

        spark = SparkSessionManager.getInstance(config);
    }

    @AfterAll
    public static void teardownSpark() {
        SparkSessionManager.close();
    }

    /**
     * Helper method to create a simple test Dataset.
     */
    protected Dataset<Row> createTestDataset() {
        // Define schema
        StructType schema = DataTypes.createStructType(new StructField[] {
            DataTypes.createStructField("id", DataTypes.IntegerType, false),
            DataTypes.createStructField("name", DataTypes.StringType, false),
            DataTypes.createStructField("salary", DataTypes.IntegerType, true)
        });

        // Create rows
        List<Row> rows = new ArrayList<>();
        rows.add(RowFactory.create(1, "Alice", 80000));
        rows.add(RowFactory.create(2, "Bob", 70000));
        rows.add(RowFactory.create(3, "Charlie", 90000));

        return spark.createDataFrame(rows, schema);
    }
}
