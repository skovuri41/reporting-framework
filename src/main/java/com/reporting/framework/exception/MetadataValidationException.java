package com.reporting.framework.exception;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when report metadata fails JSON Schema validation or business rules.
 * <p>
 * This exception captures validation errors that occur when loading metadata from
 * the database, including JSON Schema validation failures and business rule violations.
 * </p>
 */
public class MetadataValidationException extends MetadataException {

    private final List<String> validationErrors;

    /**
     * Constructs a new MetadataValidationException with a single error message.
     *
     * @param reportId the report ID with invalid metadata
     * @param errorMessage the validation error message
     */
    public MetadataValidationException(String reportId, String errorMessage) {
        super(reportId, "Metadata validation failed: " + errorMessage);
        this.validationErrors = Collections.singletonList(errorMessage);
    }

    /**
     * Constructs a new MetadataValidationException with multiple validation errors.
     *
     * @param reportId the report ID with invalid metadata
     * @param validationErrors list of validation error messages
     */
    public MetadataValidationException(String reportId, List<String> validationErrors) {
        super(reportId, "Metadata validation failed with " + validationErrors.size() + " error(s)");
        this.validationErrors = Collections.unmodifiableList(validationErrors);
    }

    /**
     * Constructs a new MetadataValidationException with a cause.
     *
     * @param reportId the report ID with invalid metadata
     * @param errorMessage the validation error message
     * @param cause the underlying cause
     */
    public MetadataValidationException(String reportId, String errorMessage, Throwable cause) {
        super(reportId, "Metadata validation failed: " + errorMessage, cause);
        this.validationErrors = Collections.singletonList(errorMessage);
    }

    /**
     * Returns the list of validation error messages.
     *
     * @return unmodifiable list of validation errors
     */
    public List<String> getValidationErrors() {
        return validationErrors;
    }
}
