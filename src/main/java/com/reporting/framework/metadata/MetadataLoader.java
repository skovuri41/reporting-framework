package com.reporting.framework.metadata;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.metadata.model.ReportCatalog;
import com.reporting.framework.metadata.model.ReportMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for loading report metadata from the database.
 * <p>
 * This class provides a simplified API for accessing report metadata,
 * delegating to MetadataRepository for caching and data access. It serves
 * as the main entry point for metadata operations in the reporting framework.
 * </p>
 * <p>
 * The loader automatically validates metadata against the JSON Schema and
 * transforms parameter names and column display names to camelCase when
 * loading from the database.
 * </p>
 */
public class MetadataLoader {

    private static final Logger logger = LoggerFactory.getLogger(MetadataLoader.class);

    private final MetadataRepository repository;

    /**
     * Constructs a MetadataLoader.
     * <p>
     * Initializes the repository and validator, and optionally warms the
     * metadata cache at startup.
     * </p>
     *
     * @param connectionProvider provides database connections
     */
    public MetadataLoader(ConnectionProvider connectionProvider) {
        this(connectionProvider, true);
    }

    /**
     * Constructs a MetadataLoader with optional cache warming.
     *
     * @param connectionProvider provides database connections
     * @param warmCacheOnStartup whether to load all metadata at startup
     */
    public MetadataLoader(ConnectionProvider connectionProvider, boolean warmCacheOnStartup) {
        JsonSchemaValidator schemaValidator = new JsonSchemaValidator();
        this.repository = new MetadataRepository(connectionProvider, schemaValidator);

        if (warmCacheOnStartup) {
            logger.info("Warming metadata cache on startup...");
            try {
                repository.loadCache();
            } catch (Exception e) {
                logger.warn("Failed to warm cache on startup, will load on-demand", e);
            }
        }
    }

    /**
     * Loads metadata for a single report by ID.
     * <p>
     * Retrieves from cache if available, otherwise loads from database,
     * validates against JSON Schema, and caches the result.
     * </p>
     * <p>
     * Parameter names and column display names are automatically converted
     * to camelCase during loading.
     * </p>
     *
     * @param reportId the unique identifier for the report
     * @return the complete report metadata
     * @throws com.reporting.framework.exception.MetadataNotFoundException if report not found
     * @throws com.reporting.framework.exception.MetadataValidationException if metadata is invalid
     */
    public ReportMetadata load(String reportId) {
        logger.debug("Loading metadata for reportId='{}'", reportId);

        if (reportId == null || reportId.trim().isEmpty()) {
            throw new IllegalArgumentException("reportId cannot be null or empty");
        }

        return repository.getById(reportId);
    }

    /**
     * Loads the complete catalog of all available reports.
     * <p>
     * Returns metadata for all reports in the system. Useful for building
     * report selection UIs or admin dashboards.
     * </p>
     *
     * @return the report catalog containing all available reports
     */
    public ReportCatalog loadCatalog() {
        logger.debug("Loading report catalog");
        return repository.getCatalog();
    }

    /**
     * Refreshes the metadata cache by reloading from the database.
     * <p>
     * Call this method after metadata changes in the database to ensure
     * the cache reflects the latest state.
     * </p>
     */
    public void refreshCache() {
        logger.info("Refreshing metadata cache");
        repository.refreshCache();
    }

    /**
     * Returns the current number of cached metadata entries.
     *
     * @return the cache size
     */
    public int getCacheSize() {
        return repository.getCacheSize();
    }
}
