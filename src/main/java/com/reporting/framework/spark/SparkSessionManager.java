package com.reporting.framework.spark;

import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Singleton manager for Apache Spark session.
 * Uses double-checked locking for thread-safe lazy initialization.
 */
public class SparkSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SparkSessionManager.class);
    private static volatile SparkSession instance;
    private static final Object lock = new Object();

    private SparkSessionManager() {
        // Private constructor to prevent instantiation
    }

    /**
     * Get or create the singleton SparkSession instance.
     *
     * @param config Spark configuration
     * @return SparkSession instance
     */
    public static SparkSession getInstance(SparkConfig config) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    logger.info("Initializing SparkSession with appName={}, master={}",
                               config.getAppName(), config.getMaster());

                    SparkSession.Builder builder = SparkSession.builder()
                        .appName(config.getAppName())
                        .master(config.getMaster())
                        .config("spark.executor.memory", config.getExecutorMemory())
                        .config("spark.driver.memory", config.getDriverMemory());

                    // Apply additional Spark properties
                    for (Map.Entry<String, String> entry : config.getSparkProperties().entrySet()) {
                        builder.config(entry.getKey(), entry.getValue());
                    }

                    instance = builder.getOrCreate();

                    // Register shutdown hook
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        logger.info("Shutting down SparkSession");
                        close();
                    }));

                    logger.info("SparkSession initialized successfully");
                }
            }
        }
        return instance;
    }

    /**
     * Close the SparkSession.
     * After calling this, getInstance() will create a new session.
     */
    public static void close() {
        if (instance != null) {
            synchronized (lock) {
                if (instance != null) {
                    logger.info("Closing SparkSession");
                    instance.close();
                    instance = null;
                }
            }
        }
    }

    /**
     * Check if SparkSession is initialized.
     *
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return instance != null;
    }
}
