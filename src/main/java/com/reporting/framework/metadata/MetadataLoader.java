package com.reporting.framework.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.exception.MetadataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Loads and caches report metadata from the database.
 * Uses Jackson to parse the JSON metadata into ReportMetadata objects.
 */
public class MetadataLoader {

    private static final Logger logger = LoggerFactory.getLogger(MetadataLoader.class);

    private final MetadataRepository repository;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, ReportMetadata> cache;
    private final boolean cacheEnabled;

    public MetadataLoader(ConnectionProvider connectionProvider) {
        this(connectionProvider, true);
    }

    public MetadataLoader(ConnectionProvider connectionProvider, boolean cacheEnabled) {
        this.repository = new MetadataRepository(connectionProvider);
        this.objectMapper = new ObjectMapper();
        this.cache = new ConcurrentHashMap<>();
        this.cacheEnabled = cacheEnabled;
    }

    /**
     * Get metadata for a report. Uses cache if enabled.
     *
     * @param reportName The name of the report
     * @return The parsed and validated ReportMetadata
     * @throws MetadataException if metadata cannot be loaded or parsed
     */
    public ReportMetadata getMetadata(String reportName) {
        if (reportName == null || reportName.trim().isEmpty()) {
            throw new MetadataException("Report name cannot be null or empty");
        }

        // Check cache first if enabled
        if (cacheEnabled) {
            ReportMetadata cached = cache.get(reportName);
            if (cached != null) {
                logger.debug("Metadata cache hit for report: {}", reportName);
                return cached;
            }
        }

        // Load from database
        logger.debug("Loading metadata from database for report: {}", reportName);
        ReportMetadata metadata = loadFromDatabase(reportName);

        // Validate metadata
        metadata.validate();

        // Cache if enabled
        if (cacheEnabled) {
            cache.put(reportName, metadata);
            logger.debug("Cached metadata for report: {}", reportName);
        }

        return metadata;
    }

    /**
     * Load metadata from database and parse JSON.
     */
    private ReportMetadata loadFromDatabase(String reportName) {
        try {
            String metadataJson = repository.fetchMetadataJson(reportName);

            if (metadataJson == null || metadataJson.trim().isEmpty()) {
                throw new MetadataException(reportName, "Metadata JSON is null or empty");
            }

            // Parse JSON to ReportMetadata object
            ReportMetadata metadata = objectMapper.readValue(metadataJson, ReportMetadata.class);

            logger.info("Successfully loaded metadata for report: {}", reportName);
            return metadata;

        } catch (Exception e) {
            if (e instanceof MetadataException) {
                throw (MetadataException) e;
            }
            logger.error("Failed to parse metadata JSON for report: {}", reportName, e);
            throw new MetadataException(reportName, "Failed to parse metadata JSON", e);
        }
    }

    /**
     * Invalidate the cache for a specific report.
     */
    public void invalidateCache(String reportName) {
        cache.remove(reportName);
        logger.debug("Invalidated cache for report: {}", reportName);
    }

    /**
     * Clear the entire metadata cache.
     */
    public void clearCache() {
        cache.clear();
        logger.debug("Cleared entire metadata cache");
    }

    /**
     * Get the current cache size.
     */
    public int getCacheSize() {
        return cache.size();
    }
}
