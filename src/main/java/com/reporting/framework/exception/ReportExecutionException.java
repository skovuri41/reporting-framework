package com.reporting.framework.exception;

/**
 * Runtime exception thrown when a report execution fails.
 * Provides detailed error context including report name, parameters, and SQL error information.
 */
public class ReportExecutionException extends RuntimeException {

    private final String reportName;
    private final String storedProcedure;

    public ReportExecutionException(String message) {
        super(message);
        this.reportName = null;
        this.storedProcedure = null;
    }

    public ReportExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.reportName = null;
        this.storedProcedure = null;
    }

    public ReportExecutionException(String reportName, String storedProcedure, String message) {
        super(formatMessage(reportName, storedProcedure, message));
        this.reportName = reportName;
        this.storedProcedure = storedProcedure;
    }

    public ReportExecutionException(String reportName, String storedProcedure, String message, Throwable cause) {
        super(formatMessage(reportName, storedProcedure, message), cause);
        this.reportName = reportName;
        this.storedProcedure = storedProcedure;
    }

    private static String formatMessage(String reportName, String storedProcedure, String message) {
        return String.format("Report execution failed [report='%s', procedure='%s']: %s",
                reportName, storedProcedure, message);
    }

    public String getReportName() {
        return reportName;
    }

    public String getStoredProcedure() {
        return storedProcedure;
    }
}
