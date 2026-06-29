package com.reporting.framework.exception;

/**
 * Exception thrown when metadata for a procedure is not found.
 */
public class MetadataNotFoundException extends MetadataException {

    public MetadataNotFoundException(String procedureId) {
        super(procedureId, "Metadata not found for procedure");
    }

    public MetadataNotFoundException(String procedureId, String message) {
        super(procedureId, message);
    }
}
