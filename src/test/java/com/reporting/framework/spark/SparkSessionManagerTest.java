package com.reporting.framework.spark;

import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SparkSessionManager singleton.
 */
class SparkSessionManagerTest {

    @Test
    void testSingletonInstance() {
        // Arrange
        SparkConfig config = SparkConfig.builder()
            .appName("Test App")
            .master("local[1]")
            .build();

        // Act
        SparkSession instance1 = SparkSessionManager.getInstance(config);
        SparkSession instance2 = SparkSessionManager.getInstance(config);

        // Assert
        assertThat(instance1).isNotNull();
        assertThat(instance2).isNotNull();
        assertThat(instance1).isSameAs(instance2);

        // Cleanup
        SparkSessionManager.close();
    }

    @Test
    void testConfiguration() {
        // Arrange
        SparkConfig config = SparkConfig.builder()
            .appName("Custom App Name")
            .master("local[2]")
            .executorMemory("512m")
            .driverMemory("512m")
            .sparkProperty("spark.ui.enabled", "false")
            .build();

        // Act
        SparkSession spark = SparkSessionManager.getInstance(config);

        // Assert
        assertThat(spark.sparkContext().appName()).isEqualTo("Custom App Name");
        assertThat(spark.conf().get("spark.master")).isEqualTo("local[2]");
        assertThat(spark.conf().get("spark.ui.enabled")).isEqualTo("false");

        // Cleanup
        SparkSessionManager.close();
    }

    @Test
    void testCloseAndRecreate() {
        // Arrange
        SparkConfig config = SparkConfig.builder().master("local[1]").build();

        // Act
        SparkSession instance1 = SparkSessionManager.getInstance(config);
        SparkSessionManager.close();
        SparkSession instance2 = SparkSessionManager.getInstance(config);

        // Assert
        assertThat(instance1).isNotNull();
        assertThat(instance2).isNotNull();
        assertThat(instance1).isNotSameAs(instance2);

        // Cleanup
        SparkSessionManager.close();
    }

    @Test
    void testIsInitialized() {
        // Arrange
        SparkConfig config = SparkConfig.builder().master("local[1]").build();

        // Act & Assert
        SparkSessionManager.close();
        assertThat(SparkSessionManager.isInitialized()).isFalse();

        SparkSessionManager.getInstance(config);
        assertThat(SparkSessionManager.isInitialized()).isTrue();

        // Cleanup
        SparkSessionManager.close();
    }
}
