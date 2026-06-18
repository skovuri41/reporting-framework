package com.reporting.framework.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Spark-specific configuration metadata.
 * Optional configuration that can be included in report metadata JSON.
 */
public class SparkMetadata {

    private final boolean enabled;
    private final int partitions;
    private final boolean cacheResult;

    @JsonCreator
    public SparkMetadata(
            @JsonProperty("enabled") Boolean enabled,
            @JsonProperty("partitions") Integer partitions,
            @JsonProperty("cacheResult") Boolean cacheResult) {
        this.enabled = enabled != null ? enabled : true;
        this.partitions = partitions != null ? partitions : 0; // 0 = use default
        this.cacheResult = cacheResult != null ? cacheResult : false;
    }

    /**
     * Default constructor with sensible defaults.
     */
    public SparkMetadata() {
        this(true, 0, false);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getPartitions() {
        return partitions;
    }

    public boolean isCacheResult() {
        return cacheResult;
    }

    @Override
    public String toString() {
        return "SparkMetadata{" +
                "enabled=" + enabled +
                ", partitions=" + partitions +
                ", cacheResult=" + cacheResult +
                '}';
    }
}
