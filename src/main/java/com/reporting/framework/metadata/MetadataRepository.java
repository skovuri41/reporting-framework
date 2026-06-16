package com.reporting.framework.metadata;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.exception.MetadataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Data access layer for querying the REPORT_METADATA table.
 */
public class MetadataRepository {

    private static final Logger logger = LoggerFactory.getLogger(MetadataRepository.class);
    private static final String QUERY_METADATA =
            "SELECT METADATA_JSON FROM REPORT_METADATA WHERE REPORT_NAME = ?";

    private final ConnectionProvider connectionProvider;

    public MetadataRepository(ConnectionProvider connectionProvider) {
        if (connectionProvider == null) {
            throw new IllegalArgumentException("ConnectionProvider cannot be null");
        }
        this.connectionProvider = connectionProvider;
    }

    /**
     * Fetch the metadata JSON for a given report name.
     *
     * @param reportName The name of the report
     * @return The metadata JSON string
     * @throws MetadataException if metadata not found or query fails
     */
    public String fetchMetadataJson(String reportName) {
        logger.debug("Fetching metadata for report: {}", reportName);

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement stmt = conn.prepareStatement(QUERY_METADATA)) {

            stmt.setString(1, reportName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String metadataJson = rs.getString("METADATA_JSON");
                    logger.debug("Successfully fetched metadata for report: {}", reportName);
                    return metadataJson;
                } else {
                    throw new MetadataException(reportName,
                            "No metadata found in database");
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to fetch metadata for report: {}", reportName, e);
            throw new MetadataException(reportName,
                    "Failed to query metadata from database", e);
        }
    }
}
