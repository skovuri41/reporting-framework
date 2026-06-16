package com.reporting.framework.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.reporting.framework.exception.MetadataException;

import java.util.Collections;
import java.util.List;

/**
 * Represents the complete metadata for a report including stored procedure,
 * result class, parameters, and mapping configuration.
 */
public class ReportMetadata {

    private final String reportName;
    private final String storedProcedure;
    private final String resultClass;
    private final List<ParameterMetadata> inputParameters;
    private final List<ParameterMetadata> outputParameters;
    private final ResultSetMapping resultSetMapping;

    @JsonCreator
    public ReportMetadata(
            @JsonProperty("reportName") String reportName,
            @JsonProperty("storedProcedure") String storedProcedure,
            @JsonProperty("resultClass") String resultClass,
            @JsonProperty("inputParameters") List<ParameterMetadata> inputParameters,
            @JsonProperty("outputParameters") List<ParameterMetadata> outputParameters,
            @JsonProperty("resultSetMapping") ResultSetMapping resultSetMapping) {
        this.reportName = reportName;
        this.storedProcedure = storedProcedure;
        this.resultClass = resultClass;
        this.inputParameters = inputParameters != null ? inputParameters : Collections.emptyList();
        this.outputParameters = outputParameters != null ? outputParameters : Collections.emptyList();
        this.resultSetMapping = resultSetMapping != null ? resultSetMapping :
                new ResultSetMapping("EXPLICIT", Collections.emptyList(), "IGNORE");
    }

    public String getReportName() {
        return reportName;
    }

    public String getStoredProcedure() {
        return storedProcedure;
    }

    public String getResultClass() {
        return resultClass;
    }

    public List<ParameterMetadata> getInputParameters() {
        return inputParameters;
    }

    public List<ParameterMetadata> getOutputParameters() {
        return outputParameters;
    }

    public ResultSetMapping getResultSetMapping() {
        return resultSetMapping;
    }

    /**
     * Get the Java Class type for the result POJO.
     */
    public Class<?> getResultClassType() {
        try {
            return Class.forName(resultClass);
        } catch (ClassNotFoundException e) {
            throw new MetadataException(reportName,
                    "Result class not found: " + resultClass, e);
        }
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
        if (resultClass == null || resultClass.trim().isEmpty()) {
            throw new MetadataException(reportName, "Result class cannot be null or empty");
        }

        // Validate result class exists
        getResultClassType();

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

        // Validate result set mapping
        if (resultSetMapping.getColumnMappings().isEmpty()) {
            throw new MetadataException(reportName, "Column mappings cannot be empty");
        }

        // Validate each column mapping
        for (ColumnMapping mapping : resultSetMapping.getColumnMappings()) {
            if (mapping.getColumn() == null || mapping.getColumn().trim().isEmpty()) {
                throw new MetadataException(reportName, "Column name cannot be null or empty in mapping");
            }
            if (mapping.getField() == null || mapping.getField().trim().isEmpty()) {
                throw new MetadataException(reportName,
                        "Field name cannot be null or empty in mapping for column: " + mapping.getColumn());
            }
        }
    }

    @Override
    public String toString() {
        return "ReportMetadata{" +
                "reportName='" + reportName + '\'' +
                ", storedProcedure='" + storedProcedure + '\'' +
                ", resultClass='" + resultClass + '\'' +
                ", inputParameters=" + inputParameters.size() +
                ", outputParameters=" + outputParameters.size() +
                ", resultSetMapping=" + resultSetMapping +
                '}';
    }
}
