package com.reporting.framework.test;

import com.reporting.framework.exception.MetadataNotFoundException;
import com.reporting.framework.metadata.StoredProcedureMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test-only implementation of metadata loading for H2 testing.
 * Since H2 doesn't support callable stored procedures, this class
 * stores metadata in memory and bypasses the database.
 */
public class TestMetadataLoader {

    private static final Logger logger = LoggerFactory.getLogger(TestMetadataLoader.class);

    private final Map<String, StoredProcedureMetadata> testMetadata = new ConcurrentHashMap<>();

    /**
     * Register test metadata programmatically.
     *
     * @param procedureId The procedure ID
     * @param metadata The metadata to register
     */
    public void registerTestMetadata(String procedureId, StoredProcedureMetadata metadata) {
        testMetadata.put(procedureId, metadata);
        logger.debug("Registered test metadata for procedure: {}", procedureId);
    }

    /**
     * Get metadata for a procedure.
     *
     * @param procedureId The procedure ID
     * @return The stored procedure metadata
     * @throws MetadataNotFoundException if not found
     */
    public StoredProcedureMetadata getMetadata(String procedureId) {
        StoredProcedureMetadata metadata = testMetadata.get(procedureId);

        if (metadata == null) {
            logger.error("Test metadata not found for procedure: {}", procedureId);
            throw new MetadataNotFoundException(procedureId);
        }

        logger.debug("Retrieved test metadata for procedure: {}", procedureId);
        return metadata;
    }

    public void invalidateCache(String procedureId) {
        testMetadata.remove(procedureId);
    }

    public void clearCache() {
        testMetadata.clear();
    }

    public int getCacheSize() {
        return testMetadata.size();
    }
}
