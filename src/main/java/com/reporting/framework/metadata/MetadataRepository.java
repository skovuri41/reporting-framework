package com.reporting.framework.metadata;

import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.exception.MetadataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Data access layer for calling the metadata stored procedure.
 * Replaces the old SELECT-based approach with stored procedure calls.
 */
public class MetadataRepository {

    private static final Logger logger = LoggerFactory.getLogger(MetadataRepository.class);
    private static final String CALL_METADATA_PROC = "{call usp_GetProcedureMetadata(?, ?)}";

    private final ConnectionProvider connectionProvider;

    public MetadataRepository(ConnectionProvider connectionProvider) {
        if (connectionProvider == null) {
            throw new IllegalArgumentException("ConnectionProvider cannot be null");
        }
        this.connectionProvider = connectionProvider;
    }

    /**
     * Fetch the metadata JSON for a given procedure ID by calling the metadata stored procedure.
     *
     * @param procedureId The ID of the stored procedure (or NULL for all procedures)
     * @return The metadata JSON string from the OUT parameter
     * @throws MetadataException if metadata not found or procedure call fails
     */
    public String fetchMetadataJson(String procedureId) {
        logger.debug("Fetching metadata for procedure: {}", procedureId);

        try (Connection conn = connectionProvider.getConnection();
             CallableStatement stmt = conn.prepareCall(CALL_METADATA_PROC)) {

            // Set input parameter (procedure ID)
            if (procedureId == null) {
                stmt.setNull(1, Types.VARCHAR);
                logger.debug("Fetching metadata for ALL procedures");
            } else {
                stmt.setString(1, procedureId);
            }

            // Register output parameter (metadata JSON)
            stmt.registerOutParameter(2, Types.NVARCHAR);

            // Execute stored procedure
            stmt.execute();

            // Get output parameter
            String metadataJson = stmt.getString(2);

            if (metadataJson == null || metadataJson.trim().isEmpty()) {
                if (procedureId == null) {
                    throw new MetadataException("No metadata found - stored procedure returned null/empty JSON");
                } else {
                    throw new MetadataException(procedureId,
                            "No metadata found for procedure - stored procedure returned null/empty JSON");
                }
            }

            logger.debug("Successfully fetched metadata (JSON length: {} characters)", metadataJson.length());
            return metadataJson;

        } catch (SQLException e) {
            logger.error("Failed to call metadata stored procedure for: {}", procedureId, e);
            if (procedureId == null) {
                throw new MetadataException("Failed to call metadata stored procedure", e);
            } else {
                throw new MetadataException(procedureId,
                        "Failed to call metadata stored procedure", e);
            }
        }
    }
}
