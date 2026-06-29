package com.reporting.framework.metadata;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents complete metadata for a stored procedure including parameters and columns.
 * Replaces the old ReportMetadata class with a clearer naming convention.
 */
public class StoredProcedureMetadata {

    private final String procedureId;
    private final String procedureName;
    private final String description;
    private final List<ParameterMetadata> parameters;
    private final List<ColumnMetadata> columns;
    private final List<ParameterMetadata> outputParameters;

    @JsonCreator
    public StoredProcedureMetadata(
            @JsonProperty("procedureId") String procedureId,
            @JsonProperty("procedureName") String procedureName,
            @JsonProperty("description") String description,
            @JsonProperty("parameters") List<ParameterMetadata> parameters,
            @JsonProperty("columns") List<ColumnMetadata> columns,
            @JsonProperty("outputParameters") List<ParameterMetadata> outputParameters) {
        this.procedureId = procedureId;
        this.procedureName = procedureName;
        this.description = description;
        this.parameters = parameters != null ? parameters : Collections.emptyList();
        this.columns = columns != null ? columns : Collections.emptyList();
        this.outputParameters = outputParameters != null ? outputParameters : Collections.emptyList();
    }

    public String getProcedureId() {
        return procedureId;
    }

    public String getProcedureName() {
        return procedureName;
    }

    public String getDescription() {
        return description;
    }

    public List<ParameterMetadata> getParameters() {
        return parameters;
    }

    public List<ColumnMetadata> getColumns() {
        return columns;
    }

    public List<ParameterMetadata> getOutputParameters() {
        return outputParameters;
    }

    // Compatibility methods for legacy code
    public String getReportName() {
        return procedureId;
    }

    public String getStoredProcedure() {
        return procedureName;
    }

    public List<ParameterMetadata> getInputParameters() {
        return parameters;
    }

    /**
     * Get all parameter names (for filtering).
     */
    public Set<String> getAllParameterNames() {
        return parameters.stream()
                .map(ParameterMetadata::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Get required parameter names (for validation).
     */
    public Set<String> getRequiredParameterNames() {
        return parameters.stream()
                .filter(p -> !p.isNullable())
                .map(ParameterMetadata::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Get column name to field name mapping.
     * Example: EMPLOYEE_ID → employeeId
     */
    public Map<String, String> getFieldNameMapping() {
        return columns.stream()
                .collect(Collectors.toMap(
                        ColumnMetadata::getColumnName,
                        ColumnMetadata::getFieldName,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    /**
     * Validate metadata configuration.
     */
    public void validate() {
        if (procedureId == null || procedureId.trim().isEmpty()) {
            throw new IllegalArgumentException("Procedure ID cannot be null or empty");
        }
        if (procedureName == null || procedureName.trim().isEmpty()) {
            throw new IllegalArgumentException("Procedure name cannot be null or empty for: " + procedureId);
        }

        // Validate parameters
        for (ParameterMetadata param : parameters) {
            if (param.getName() == null || param.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Parameter name cannot be null or empty for procedure: " + procedureId);
            }
            if (param.getType() == null || param.getType().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Parameter type cannot be null or empty: " + param.getName() + " in procedure: " + procedureId);
            }
        }

        // Validate output parameters
        for (ParameterMetadata param : outputParameters) {
            if (param.getName() == null || param.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Output parameter name cannot be null or empty for procedure: " + procedureId);
            }
            if (param.getType() == null || param.getType().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Output parameter type cannot be null or empty: " + param.getName() + " in procedure: " + procedureId);
            }
        }

        // Validate columns
        for (ColumnMetadata col : columns) {
            if (col.getColumnName() == null || col.getColumnName().trim().isEmpty()) {
                throw new IllegalArgumentException("Column name cannot be null or empty for procedure: " + procedureId);
            }
            if (col.getFieldName() == null || col.getFieldName().trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Field name cannot be null or empty for column: " + col.getColumnName() + " in procedure: " + procedureId);
            }
        }
    }

    @Override
    public String toString() {
        return "StoredProcedureMetadata{" +
                "procedureId='" + procedureId + '\'' +
                ", procedureName='" + procedureName + '\'' +
                ", parameters=" + parameters.size() +
                ", columns=" + columns.size() +
                ", outputParameters=" + outputParameters.size() +
                '}';
    }

    /**
     * Builder for creating StoredProcedureMetadata instances in tests.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String procedureId;
        private String procedureName;
        private String description;
        private List<ParameterMetadata> parameters = new ArrayList<>();
        private List<ColumnMetadata> columns = new ArrayList<>();
        private List<ParameterMetadata> outputParameters = new ArrayList<>();

        public Builder procedureId(String procedureId) {
            this.procedureId = procedureId;
            return this;
        }

        public Builder procedureName(String procedureName) {
            this.procedureName = procedureName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder addParameter(String name, String fieldName, String type, boolean nullable) {
            this.parameters.add(new ParameterMetadata(name, fieldName, type, nullable));
            return this;
        }

        public Builder addColumn(String columnName, String fieldName, String columnType) {
            this.columns.add(new ColumnMetadata(columnName, fieldName, columnType));
            return this;
        }

        public Builder addOutputParameter(String name, String fieldName, String type) {
            this.outputParameters.add(new ParameterMetadata(name, fieldName, type, false));
            return this;
        }

        public StoredProcedureMetadata build() {
            return new StoredProcedureMetadata(
                    procedureId,
                    procedureName,
                    description,
                    parameters,
                    columns,
                    outputParameters
            );
        }
    }
}
