package com.reporting.framework.exception;

/**
 * Runtime exception thrown when metadata loading or parsing fails.
 * Provides detailed error context including report name and metadata source.
 */
public class MetadataException extends RuntimeException {

    private final String reportName;

    public MetadataException(String message) {
        super(message);
        this.reportName = null;
    }

    public MetadataException(String message, Throwable cause) {
        super(message, cause);
        this.reportName = null;
    }

    public MetadataException(String reportName, String message) {
        super(formatMessage(reportName, message));
        this.reportName = reportName;
    }

    public MetadataException(String reportName, String message, Throwable cause) {
        super(formatMessage(reportName, message), cause);
        this.reportName = reportName;
    }

    private static String formatMessage(String reportName, String message) {
        return String.format("Metadata error [report='%s']: %s", reportName, message);
    }

    public String getReportName() {
        return reportName;
    }
}
