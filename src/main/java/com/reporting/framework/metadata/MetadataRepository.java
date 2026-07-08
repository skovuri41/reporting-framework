package com.reporting.framework.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.exception.MetadataNotFoundException;
import com.reporting.framework.metadata.model.*;
import com.reporting.framework.metadata.util.NamingConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for loading and caching report metadata from the database.
 * <p>
 * This class provides in-memory caching of report metadata with methods to:
 * <ul>
 *   <li>Load all metadata at startup (cache warming)</li>
 *   <li>Retrieve metadata by report ID</li>
 *   <li>Retrieve the complete catalog of all reports</li>
 *   <li>Refresh the cache on demand</li>
 * </ul>
 * </p>
 * <p>
 * Metadata is validated against the JSON Schema before being cached. The cache
 * is thread-safe using ConcurrentHashMap.
 * </p>
 */
public class MetadataRepository {

    private static final Logger logger = LoggerFactory.getLogger(MetadataRepository.class);
    private static final String LOAD_ALL_SQL =
        "SELECT REPORT_ID, METADATA_JSON FROM REPORT_METADATA ORDER BY REPORT_ID";
    private static final String LOAD_BY_ID_SQL =
        "SELECT METADATA_JSON FROM REPORT_METADATA WHERE REPORT_ID = ?";

    private final ConnectionProvider connectionProvider;
    private final JsonSchemaValidator schemaValidator;
    private final ObjectMapper objectMapper;
    private final Map<String, ReportMetadata> cache;

    /**
     * Constructs a MetadataRepository.
     *
     * @param connectionProvider provides database connections
     * @param schemaValidator validates metadata JSON
     */
    public MetadataRepository(ConnectionProvider connectionProvider, JsonSchemaValidator schemaValidator) {
        this.connectionProvider = connectionProvider;
        this.schemaValidator = schemaValidator;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // Register JSR-310 for LocalDate/LocalDateTime
        this.cache = new ConcurrentHashMap<>();
    }

    /**
     * Loads all report metadata from the database into the cache.
     * <p>
     * This method should be called at application startup to warm the cache.
     * It can also be called on-demand to refresh the cache.
     * </p>
     *
     * @throws RuntimeException if metadata loading fails
     */
    public void loadCache() {
        logger.info("Loading all report metadata into cache...");
        cache.clear();

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(LOAD_ALL_SQL);
             ResultSet rs = stmt.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                String reportId = rs.getString("REPORT_ID");
                String metadataJson = rs.getString("METADATA_JSON");

                try {
                    // Validate JSON against schema
                    schemaValidator.validate(reportId, metadataJson);

                    // Deserialize to ReportMetadata
                    ReportMetadata metadata = objectMapper.readValue(metadataJson, ReportMetadata.class);

                    // Apply naming transformation (T030-T032)
                    metadata = transformNaming(metadata);

                    // Cache it
                    cache.put(reportId, metadata);
                    count++;
                    logger.debug("Loaded metadata for reportId='{}'", reportId);

                } catch (Exception e) {
                    logger.error("Failed to load metadata for reportId='{}': {}", reportId, e.getMessage());
                    // Continue loading other reports even if one fails
                }
            }

            logger.info("Successfully loaded {} report metadata entries into cache", count);

        } catch (Exception e) {
            logger.error("Failed to load metadata cache from database", e);
            throw new RuntimeException("Failed to load metadata cache", e);
        }
    }

    /**
     * Retrieves metadata for a specific report by ID.
     * <p>
     * First checks the in-memory cache. If not found and the cache is empty,
     * attempts to load directly from the database.
     * </p>
     *
     * @param reportId the report ID to retrieve
     * @return the report metadata
     * @throws MetadataNotFoundException if the report ID does not exist
     */
    public ReportMetadata getById(String reportId) {
        // Check cache first
        ReportMetadata metadata = cache.get(reportId);
        if (metadata != null) {
            logger.debug("Retrieved metadata for reportId='{}' from cache", reportId);
            return metadata;
        }

        // If cache is empty, try loading from database directly
        if (cache.isEmpty()) {
            logger.warn("Cache is empty, attempting direct database load for reportId='{}'", reportId);
            metadata = loadFromDatabase(reportId);
            if (metadata != null) {
                cache.put(reportId, metadata);
                return metadata;
            }
        }

        // Not found
        throw new MetadataNotFoundException(reportId);
    }

    /**
     * Retrieves the complete catalog of all available reports.
     * <p>
     * Returns a ReportCatalog containing all metadata entries from the cache.
     * If the cache is empty, loads from the database first.
     * </p>
     *
     * @return the report catalog
     */
    public ReportCatalog getCatalog() {
        if (cache.isEmpty()) {
            logger.info("Cache is empty, loading all metadata before returning catalog");
            loadCache();
        }

        List<ReportMetadata> allReports = new ArrayList<>(cache.values());
        logger.debug("Returning catalog with {} reports", allReports.size());

        return new ReportCatalog(allReports);
    }

    /**
     * Refreshes the metadata cache by reloading from the database.
     */
    public void refreshCache() {
        logger.info("Refreshing metadata cache...");
        loadCache();
    }

    /**
     * Returns the current cache size.
     *
     * @return number of cached metadata entries
     */
    public int getCacheSize() {
        return cache.size();
    }

    /**
     * Loads a single report's metadata directly from the database.
     *
     * @param reportId the report ID to load
     * @return the report metadata, or null if not found
     */
    private ReportMetadata loadFromDatabase(String reportId) {
        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(LOAD_BY_ID_SQL)) {

            stmt.setString(1, reportId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String metadataJson = rs.getString("METADATA_JSON");

                    // Validate and deserialize
                    schemaValidator.validate(reportId, metadataJson);
                    ReportMetadata metadata = objectMapper.readValue(metadataJson, ReportMetadata.class);

                    // Apply naming transformation
                    return transformNaming(metadata);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to load metadata for reportId='{}' from database", reportId, e);
        }

        return null;
    }

    /**
     * Transforms metadata naming from database format to camelCase.
     * <p>
     * Applies NamingConverter to:
     * - Parameter names (T030)
     * - Column display names (T031)
     * - Preserves sourceColumn exactly (T032)
     * </p>
     *
     * @param metadata the original metadata
     * @return transformed metadata with camelCase names
     */
    private ReportMetadata transformNaming(ReportMetadata metadata) {
        List<Dataset> transformedDatasets = metadata.getDatasets().stream()
            .map(this::transformDataset)
            .collect(Collectors.toList());

        return new ReportMetadata(
            metadata.getReportId(),
            metadata.getReportName(),
            metadata.getReportDescription(),
            transformedDatasets
        );
    }

    /**
     * Transforms dataset naming.
     */
    private Dataset transformDataset(Dataset dataset) {
        List<Parameter> transformedParameters = dataset.getParameters().stream()
            .map(this::transformParameter)
            .collect(Collectors.toList());

        List<Column> transformedColumns = dataset.getColumns().stream()
            .map(this::transformColumn)
            .collect(Collectors.toList());

        return new Dataset(
            dataset.getDatasource(),
            dataset.getDatasourceId(),
            dataset.getDatasourceType(),
            dataset.getDatasourceDescription(),
            transformedParameters,
            transformedColumns
        );
    }

    /**
     * Transforms parameter naming to camelCase (T030).
     */
    private Parameter transformParameter(Parameter param) {
        String transformedName = NamingConverter.toCamelCase(param.getParameterName());

        return new Parameter(
            transformedName,
            param.getDataType(),
            param.getParameterDirection(),
            param.getNullable()
        );
    }

    /**
     * Transforms column naming (T031, T032).
     * - displayName is transformed to camelCase
     * - sourceColumn is preserved exactly as-is
     */
    private Column transformColumn(Column column) {
        String transformedDisplayName = NamingConverter.toCamelCase(column.getDisplayName());

        return new Column(
            column.getSourceColumn(), // T032: Preserved exactly
            transformedDisplayName,   // T031: Transformed to camelCase
            column.getDataType(),
            column.getSortable(),
            column.getGroupable(),
            column.getFilterable()
        );
    }
}
