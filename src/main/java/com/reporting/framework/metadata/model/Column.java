package com.reporting.framework.metadata.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Immutable DTO representing a column in a dataset result set.
 * <p>
 * Defines a column with its source name (preserved from database), display name
 * (converted to camelCase), data type, and UI metadata flags (sortable, groupable,
 * filterable).
 * </p>
 * <p>
 * The sourceColumn is preserved exactly as it appears in the database for SQL
 * compatibility, while displayName is automatically converted to camelCase for
 * consistent UI presentation.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Column {
    private final String sourceColumn;
    private final String displayName;
    private final String dataType;
    private final Boolean sortable;
    private final Boolean groupable;
    private final Boolean filterable;

    /**
     * Constructs an immutable Column instance.
     * <p>
     * This constructor is used by Jackson for JSON deserialization.
     * </p>
     *
     * @param sourceColumn the original column name from the data source (preserved as-is)
     * @param displayName the user-friendly display name (camelCase format)
     * @param dataType the SQL data type (e.g., "INTEGER", "VARCHAR")
     * @param sortable whether the column supports sorting
     * @param groupable whether the column supports grouping
     * @param filterable whether the column supports filtering
     * @throws NullPointerException if any required field is null
     */
    @JsonCreator
    public Column(
        @JsonProperty("sourceColumn") String sourceColumn,
        @JsonProperty("displayName") String displayName,
        @JsonProperty("dataType") String dataType,
        @JsonProperty("sortable") Boolean sortable,
        @JsonProperty("groupable") Boolean groupable,
        @JsonProperty("filterable") Boolean filterable
    ) {
        this.sourceColumn = Objects.requireNonNull(sourceColumn, "sourceColumn cannot be null");
        this.displayName = Objects.requireNonNull(displayName, "displayName cannot be null");
        this.dataType = Objects.requireNonNull(dataType, "dataType cannot be null");
        this.sortable = Objects.requireNonNull(sortable, "sortable cannot be null");
        this.groupable = Objects.requireNonNull(groupable, "groupable cannot be null");
        this.filterable = Objects.requireNonNull(filterable, "filterable cannot be null");

        if (sourceColumn.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceColumn cannot be empty");
        }
        if (displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("displayName cannot be empty");
        }
        if (dataType.trim().isEmpty()) {
            throw new IllegalArgumentException("dataType cannot be empty");
        }
    }

    /**
     * Returns the original column name from the data source.
     * <p>
     * This name is preserved exactly as it appears in the database and is used
     * for SQL references (e.g., in WHERE, ORDER BY clauses).
     * </p>
     *
     * @return the source column name
     */
    public String getSourceColumn() {
        return sourceColumn;
    }

    /**
     * Returns the user-friendly display name in camelCase format.
     * <p>
     * This name is automatically converted from database naming conventions
     * to camelCase when metadata is loaded, providing consistent naming for
     * UI consumers.
     * </p>
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the SQL data type of the column.
     *
     * @return the data type (e.g., "INTEGER", "VARCHAR", "DECIMAL")
     */
    public String getDataType() {
        return dataType;
    }

    /**
     * Returns whether the column supports sorting in the UI.
     *
     * @return true if sortable, false otherwise
     */
    public Boolean getSortable() {
        return sortable;
    }

    /**
     * Returns whether the column supports grouping in the UI.
     *
     * @return true if groupable, false otherwise
     */
    public Boolean getGroupable() {
        return groupable;
    }

    /**
     * Returns whether the column supports filtering in the UI.
     *
     * @return true if filterable, false otherwise
     */
    public Boolean getFilterable() {
        return filterable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Column column = (Column) o;
        return Objects.equals(sourceColumn, column.sourceColumn) &&
               Objects.equals(displayName, column.displayName) &&
               Objects.equals(dataType, column.dataType) &&
               Objects.equals(sortable, column.sortable) &&
               Objects.equals(groupable, column.groupable) &&
               Objects.equals(filterable, column.filterable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceColumn, displayName, dataType, sortable, groupable, filterable);
    }

    @Override
    public String toString() {
        return "Column{" +
               "sourceColumn='" + sourceColumn + '\'' +
               ", displayName='" + displayName + '\'' +
               ", dataType='" + dataType + '\'' +
               ", sortable=" + sortable +
               ", groupable=" + groupable +
               ", filterable=" + filterable +
               '}';
    }
}
