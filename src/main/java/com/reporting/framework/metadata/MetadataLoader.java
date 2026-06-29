package com.reporting.framework.metadata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.exception.MetadataException;
import com.reporting.framework.exception.MetadataNotFoundException;
import com.reporting.framework.exception.MetadataParseException;
import com.reporting.framework.mapper.ObjectMapperFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Loads and caches stored procedure metadata from the database.
 * Uses Jackson to parse the JSON metadata into StoredProcedureMetadata objects.
 */
public class MetadataLoader {

    private static final Logger logger = LoggerFactory.getLogger(MetadataLoader.class);

    private final MetadataRepository repository;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, StoredProcedureMetadata> cache;
    private final boolean cacheEnabled;

    public MetadataLoader(ConnectionProvider connectionProvider) {
        this(connectionProvider, true);
    }

    public MetadataLoader(ConnectionProvider connectionProvider, boolean cacheEnabled) {
        this.repository = new MetadataRepository(connectionProvider);
        this.objectMapper = ObjectMapperFactory.createObjectMapper();
        this.cache = new ConcurrentHashMap<>();
        this.cacheEnabled = cacheEnabled;

        // Cache warming on startup
        if (cacheEnabled) {
            warmCache();
        }
    }

    /**
     * Load all procedure metadata on startup to warm the cache.
     * If cache warming fails, logs a warning and continues (will load on-demand).
     */
    private void warmCache() {
        logger.info("Warming metadata cache...");
        try {
            List<StoredProcedureMetadata> allMetadata = loadAllMetadata();
            for (StoredProcedureMetadata metadata : allMetadata) {
                cache.put(metadata.getProcedureId(), metadata);
            }
            logger.info("Cache warmed with {} procedures", cache.size());
        } catch (Exception e) {
            logger.warn("Failed to warm cache, will load metadata on-demand", e);
        }
    }

    /**
     * Load all metadata from the database (for cache warming).
     */
    private List<StoredProcedureMetadata> loadAllMetadata() {
        try {
            // Call metadata stored proc with NULL to get all procedures
            String json = repository.fetchMetadataJson(null);

            if (json == null || json.trim().isEmpty()) {
                throw new MetadataException("Metadata stored proc returned null/empty JSON for all procedures");
            }

            // Parse JSON array
            try {
                List<StoredProcedureMetadata> metadataList = objectMapper.readValue(json,
                        new TypeReference<List<StoredProcedureMetadata>>() {});

                // Validate each metadata
                for (StoredProcedureMetadata metadata : metadataList) {
                    metadata.validate();
                }

                return metadataList;
            } catch (JsonProcessingException e) {
                throw new MetadataParseException("Failed to parse metadata JSON array: " + json, e);
            }

        } catch (Exception e) {
            if (e instanceof MetadataException) {
                throw (MetadataException) e;
            }
            throw new MetadataException("Failed to load all metadata", e);
        }
    }

    /**
     * Get metadata for a stored procedure. Uses cache if enabled.
     *
     * @param procedureId The ID of the stored procedure
     * @return The parsed and validated StoredProcedureMetadata
     * @throws MetadataException if metadata cannot be loaded or parsed
     */
    public StoredProcedureMetadata getMetadata(String procedureId) {
        if (procedureId == null || procedureId.trim().isEmpty()) {
            throw new MetadataException("Procedure ID cannot be null or empty");
        }

        // Check cache first if enabled
        if (cacheEnabled) {
            StoredProcedureMetadata cached = cache.get(procedureId);
            if (cached != null) {
                logger.debug("Metadata cache hit for procedure: {}", procedureId);
                return cached;
            }
        }

        // Load from database
        logger.debug("Loading metadata from database for procedure: {}", procedureId);
        StoredProcedureMetadata metadata = loadFromDatabase(procedureId);

        // Validate metadata
        metadata.validate();

        // Cache if enabled
        if (cacheEnabled) {
            cache.put(procedureId, metadata);
            logger.debug("Cached metadata for procedure: {}", procedureId);
        }

        return metadata;
    }

    /**
     * Load metadata from database and parse JSON.
     */
    private StoredProcedureMetadata loadFromDatabase(String procedureId) {
        try {
            String metadataJson = repository.fetchMetadataJson(procedureId);

            if (metadataJson == null || metadataJson.trim().isEmpty()) {
                throw new MetadataNotFoundException(procedureId);
            }

            // Parse JSON to StoredProcedureMetadata object
            try {
                StoredProcedureMetadata metadata = objectMapper.readValue(metadataJson, StoredProcedureMetadata.class);
                logger.info("Successfully loaded metadata for procedure: {}", procedureId);
                return metadata;
            } catch (JsonProcessingException e) {
                throw new MetadataParseException(
                        "Failed to parse metadata JSON for procedure: " + procedureId, e);
            }

        } catch (Exception e) {
            if (e instanceof MetadataException) {
                throw (MetadataException) e;
            }
            logger.error("Failed to load metadata for procedure: {}", procedureId, e);
            throw new MetadataException(procedureId, "Failed to load metadata", e);
        }
    }

    /**
     * Invalidate the cache for a specific procedure.
     */
    public void invalidateCache(String procedureId) {
        cache.remove(procedureId);
        logger.debug("Invalidated cache for procedure: {}", procedureId);
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
