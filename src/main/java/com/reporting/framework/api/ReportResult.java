package com.reporting.framework.api;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the result of a report execution including the result set data
 * and output parameters.
 *
 * @param <T> The type of POJO representing each row in the result set
 */
public class ReportResult<T> {

    private final List<T> results;
    private final Map<String, Object> outputParameters;
    private final String reportName;
    private final Instant executionTime;

    public ReportResult(List<T> results, Map<String, Object> outputParameters, String reportName) {
        this.results = results != null ? results : Collections.emptyList();
        this.outputParameters = outputParameters != null ? outputParameters : Collections.emptyMap();
        this.reportName = reportName;
        this.executionTime = Instant.now();
    }

    /**
     * Get the list of result rows as POJOs.
     */
    public List<T> getResults() {
        return results;
    }

    /**
     * Get the output parameters from the stored procedure.
     */
    public Map<String, Object> getOutputParameters() {
        return outputParameters;
    }

    /**
     * Get a specific output parameter value.
     */
    public Object getOutputParameter(String name) {
        return outputParameters.get(name);
    }

    /**
     * Get the name of the report that was executed.
     */
    public String getReportName() {
        return reportName;
    }

    /**
     * Get the execution timestamp.
     */
    public Instant getExecutionTime() {
        return executionTime;
    }

    /**
     * Get the number of rows in the result set.
     */
    public int getResultCount() {
        return results.size();
    }

    @Override
    public String toString() {
        return "ReportResult{" +
                "reportName='" + reportName + '\'' +
                ", resultCount=" + results.size() +
                ", outputParameters=" + outputParameters.size() +
                ", executionTime=" + executionTime +
                '}';
    }
}
