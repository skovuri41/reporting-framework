package com.reporting.framework.exception;

/**
 * Exception thrown when report metadata cannot be found for a given report ID.
 * <p>
 * This indicates that the requested report does not exist in the REPORT_METADATA
 * table, or the metadata cache does not contain an entry for the specified ID.
 * </p>
 */
public class MetadataNotFoundException extends MetadataException {

    /**
     * Constructs a new MetadataNotFoundException for the specified report ID.
     *
     * @param reportId the report ID that was not found
     */
    public MetadataNotFoundException(String reportId) {
        super(reportId, "Report metadata not found");
    }

    /**
     * Constructs a new MetadataNotFoundException with a custom message.
     *
     * @param reportId the report ID that was not found
     * @param message the custom error message
     */
    public MetadataNotFoundException(String reportId, String message) {
        super(reportId, message);
    }
}
