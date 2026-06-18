package com.reporting.framework.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.reporting.framework.exception.MetadataException;

import java.util.Collections;
import java.util.List;

/**
 * Represents the complete metadata for a report including stored procedure,
 * parameters, and Spark configuration.
 */
public class ReportMetadata {

    private final String reportName;
    private final String storedProcedure;
    private final List<ParameterMetadata> inputParameters;
    private final List<ParameterMetadata> outputParameters;
    private final SparkMetadata sparkConfig;

    @JsonCreator
    public ReportMetadata(
            @JsonProperty("reportName") String reportName,
            @JsonProperty("storedProcedure") String storedProcedure,
            @JsonProperty("inputParameters") List<ParameterMetadata> inputParameters,
            @JsonProperty("outputParameters") List<ParameterMetadata> outputParameters,
            @JsonProperty("sparkConfig") SparkMetadata sparkConfig) {
        this.reportName = reportName;
        this.storedProcedure = storedProcedure;
        this.inputParameters = inputParameters != null ? inputParameters : Collections.emptyList();
        this.outputParameters = outputParameters != null ? outputParameters : Collections.emptyList();
        this.sparkConfig = sparkConfig != null ? sparkConfig : new SparkMetadata();
    }

    public String getReportName() {
        return reportName;
    }

    public String getStoredProcedure() {
        return storedProcedure;
    }

    public List<ParameterMetadata> getInputParameters() {
        return inputParameters;
    }

    public List<ParameterMetadata> getOutputParameters() {
        return outputParameters;
    }

    public SparkMetadata getSparkConfig() {
        return sparkConfig;
    }

    /**
     * Validate metadata configuration.
     */
    public void validate() {
        if (reportName == null || reportName.trim().isEmpty()) {
            throw new MetadataException("Report name cannot be null or empty");
        }
        if (storedProcedure == null || storedProcedure.trim().isEmpty()) {
            throw new MetadataException(reportName, "Stored procedure cannot be null or empty");
        }

        // Validate input parameters
        for (ParameterMetadata param : inputParameters) {
            if (param.getName() == null || param.getName().trim().isEmpty()) {
                throw new MetadataException(reportName, "Input parameter name cannot be null or empty");
            }
            if (param.getSqlType() == null || param.getSqlType().trim().isEmpty()) {
                throw new MetadataException(reportName,
                        "Input parameter SQL type cannot be null or empty: " + param.getName());
            }
        }

        // Validate output parameters
        for (ParameterMetadata param : outputParameters) {
            if (param.getName() == null || param.getName().trim().isEmpty()) {
                throw new MetadataException(reportName, "Output parameter name cannot be null or empty");
            }
            if (param.getSqlType() == null || param.getSqlType().trim().isEmpty()) {
                throw new MetadataException(reportName,
                        "Output parameter SQL type cannot be null or empty: " + param.getName());
            }
        }
    }

    @Override
    public String toString() {
        return "ReportMetadata{" +
                "reportName='" + reportName + '\'' +
                ", storedProcedure='" + storedProcedure + '\'' +
                ", inputParameters=" + inputParameters.size() +
                ", outputParameters=" + outputParameters.size() +
                ", sparkConfig=" + sparkConfig +
                '}';
    }
}
