package com.reporting.framework.exception;

/**
 * Exception thrown when metadata JSON cannot be parsed.
 */
public class MetadataParseException extends MetadataException {

    public MetadataParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetadataParseException(String procedureId, String message, Throwable cause) {
        super(procedureId, message, cause);
    }
}
