package com.reporting.framework.metadata.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable DTO representing a dataset within a report.
 * <p>
 * A dataset represents a single data source (e.g., stored procedure) with its
 * parameters and column definitions. Reports can contain multiple datasets to
 * combine data from different sources.
 * </p>
 * <p>
 * This class follows the immutability principle - all fields are final,
 * collections are wrapped in unmodifiable lists, and defensive copying is
 * performed in the constructor.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dataset {
    private final String datasource;
    private final String datasourceId;
    private final String datasourceType;
    private final String datasourceDescription;
    private final List<Parameter> parameters;
    private final List<Column> columns;

    /**
     * Constructs an immutable Dataset instance.
     * <p>
     * This constructor is used by Jackson for JSON deserialization.
     * Performs defensive copying of the parameters and columns lists.
     * </p>
     *
     * @param datasource the name of the data source (e.g., stored procedure name)
     * @param datasourceId unique identifier for this dataset within the report
     * @param datasourceType the type of data source (currently only "StoredProc")
     * @param datasourceDescription description of what this dataset provides
     * @param parameters list of input/output parameters (may be empty)
     * @param columns list of column definitions (may be empty)
     * @throws NullPointerException if any required field is null
     */
    @JsonCreator
    public Dataset(
        @JsonProperty("datasource") String datasource,
        @JsonProperty("datasourceId") String datasourceId,
        @JsonProperty("datasourceType") String datasourceType,
        @JsonProperty("datasourceDescription") String datasourceDescription,
        @JsonProperty("parameters") List<Parameter> parameters,
        @JsonProperty("columns") List<Column> columns
    ) {
        this.datasource = Objects.requireNonNull(datasource, "datasource cannot be null");
        this.datasourceId = Objects.requireNonNull(datasourceId, "datasourceId cannot be null");
        this.datasourceType = Objects.requireNonNull(datasourceType, "datasourceType cannot be null");
        this.datasourceDescription = datasourceDescription; // nullable
        this.parameters = Collections.unmodifiableList(
            parameters != null ? new ArrayList<>(parameters) : Collections.emptyList()
        );
        this.columns = Collections.unmodifiableList(
            columns != null ? new ArrayList<>(columns) : Collections.emptyList()
        );

        if (datasource.trim().isEmpty()) {
            throw new IllegalArgumentException("datasource cannot be empty");
        }
        if (datasourceId.trim().isEmpty()) {
            throw new IllegalArgumentException("datasourceId cannot be empty");
        }
        if (datasourceType.trim().isEmpty()) {
            throw new IllegalArgumentException("datasourceType cannot be empty");
        }
    }

    /**
     * Returns the name of the data source.
     *
     * @return the datasource name (e.g., "usp_GetEmployees")
     */
    public String getDatasource() {
        return datasource;
    }

    /**
     * Returns the unique identifier for this dataset within the report.
     *
     * @return the datasource ID (e.g., "ds-001")
     */
    public String getDatasourceId() {
        return datasourceId;
    }

    /**
     * Returns the type of data source.
     *
     * @return the datasource type (currently only "StoredProc" is supported)
     */
    public String getDatasourceType() {
        return datasourceType;
    }

    /**
     * Returns the description of this dataset.
     *
     * @return the datasource description, or null if not provided
     */
    public String getDatasourceDescription() {
        return datasourceDescription;
    }

    /**
     * Returns the list of parameters for this dataset.
     * <p>
     * The returned list is unmodifiable.
     * </p>
     *
     * @return unmodifiable list of parameters (may be empty)
     */
    public List<Parameter> getParameters() {
        return parameters;
    }

    /**
     * Returns the list of column definitions for this dataset.
     * <p>
     * The returned list is unmodifiable.
     * </p>
     *
     * @return unmodifiable list of columns (may be empty)
     */
    public List<Column> getColumns() {
        return columns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dataset dataset = (Dataset) o;
        return Objects.equals(datasource, dataset.datasource) &&
               Objects.equals(datasourceId, dataset.datasourceId) &&
               Objects.equals(datasourceType, dataset.datasourceType) &&
               Objects.equals(datasourceDescription, dataset.datasourceDescription) &&
               Objects.equals(parameters, dataset.parameters) &&
               Objects.equals(columns, dataset.columns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(datasource, datasourceId, datasourceType,
                           datasourceDescription, parameters, columns);
    }

    @Override
    public String toString() {
        return "Dataset{" +
               "datasource='" + datasource + '\'' +
               ", datasourceId='" + datasourceId + '\'' +
               ", datasourceType='" + datasourceType + '\'' +
               ", datasourceDescription='" + datasourceDescription + '\'' +
               ", parameters=" + parameters.size() +
               ", columns=" + columns.size() +
               '}';
    }
}
