package com.reporting.framework.spark;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for Apache Spark session.
 * Uses builder pattern for construction.
 */
public class SparkConfig {

    private final String appName;
    private final String master;
    private final String executorMemory;
    private final String driverMemory;
    private final Map<String, String> sparkProperties;

    private SparkConfig(Builder builder) {
        this.appName = builder.appName;
        this.master = builder.master;
        this.executorMemory = builder.executorMemory;
        this.driverMemory = builder.driverMemory;
        this.sparkProperties = new HashMap<>(builder.sparkProperties);
    }

    public String getAppName() {
        return appName;
    }

    public String getMaster() {
        return master;
    }

    public String getExecutorMemory() {
        return executorMemory;
    }

    public String getDriverMemory() {
        return driverMemory;
    }

    public Map<String, String> getSparkProperties() {
        return new HashMap<>(sparkProperties);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String appName = "Reporting Framework";
        private String master = "local[*]";
        private String executorMemory = "1g";
        private String driverMemory = "1g";
        private Map<String, String> sparkProperties = new HashMap<>();

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder master(String master) {
            this.master = master;
            return this;
        }

        public Builder executorMemory(String executorMemory) {
            this.executorMemory = executorMemory;
            return this;
        }

        public Builder driverMemory(String driverMemory) {
            this.driverMemory = driverMemory;
            return this;
        }

        public Builder sparkProperty(String key, String value) {
            this.sparkProperties.put(key, value);
            return this;
        }

        public Builder sparkProperties(Map<String, String> properties) {
            this.sparkProperties.putAll(properties);
            return this;
        }

        public SparkConfig build() {
            return new SparkConfig(this);
        }
    }
}
