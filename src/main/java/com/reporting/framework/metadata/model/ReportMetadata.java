package com.reporting.framework.metadata.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable DTO representing complete metadata for a single report.
 * <p>
 * This is the top-level container for a report's metadata, including its
 * identifying information and all associated datasets. Each dataset contains
 * parameters and column definitions.
 * </p>
 * <p>
 * This class follows the immutability principle - all fields are final,
 * collections are wrapped in unmodifiable lists, and defensive copying is
 * performed in the constructor.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportMetadata {
    private final String reportId;
    private final String reportName;
    private final String reportDescription;
    private final List<Dataset> datasets;

    /**
     * Constructs an immutable ReportMetadata instance.
     * <p>
     * This constructor is used by Jackson for JSON deserialization.
     * Performs defensive copying of the datasets list.
     * </p>
     *
     * @param reportId unique identifier for the report
     * @param reportName display name of the report
     * @param reportDescription human-readable description of the report
     * @param datasets list of datasets for this report
     * @throws NullPointerException if any required field is null
     */
    @JsonCreator
    public ReportMetadata(
        @JsonProperty("reportId") String reportId,
        @JsonProperty("reportName") String reportName,
        @JsonProperty("reportDescription") String reportDescription,
        @JsonProperty("datasets") List<Dataset> datasets
    ) {
        this.reportId = Objects.requireNonNull(reportId, "reportId cannot be null");
        this.reportName = Objects.requireNonNull(reportName, "reportName cannot be null");
        this.reportDescription = reportDescription; // nullable
        this.datasets = Collections.unmodifiableList(
            datasets != null ? new ArrayList<>(datasets) : Collections.emptyList()
        );

        if (reportId.trim().isEmpty()) {
            throw new IllegalArgumentException("reportId cannot be empty");
        }
        if (reportName.trim().isEmpty()) {
            throw new IllegalArgumentException("reportName cannot be empty");
        }
    }

    /**
     * Returns the unique identifier for this report.
     *
     * @return the report ID
     */
    public String getReportId() {
        return reportId;
    }

    /**
     * Returns the display name of this report.
     *
     * @return the report name
     */
    public String getReportName() {
        return reportName;
    }

    /**
     * Returns the description of this report.
     *
     * @return the report description, or null if not provided
     */
    public String getReportDescription() {
        return reportDescription;
    }

    /**
     * Returns the list of datasets for this report.
     * <p>
     * The returned list is unmodifiable. A report may have zero or more datasets,
     * though typically at least one dataset is required for the report to be
     * executable.
     * </p>
     *
     * @return unmodifiable list of datasets (may be empty)
     */
    public List<Dataset> getDatasets() {
        return datasets;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportMetadata that = (ReportMetadata) o;
        return Objects.equals(reportId, that.reportId) &&
               Objects.equals(reportName, that.reportName) &&
               Objects.equals(reportDescription, that.reportDescription) &&
               Objects.equals(datasets, that.datasets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reportId, reportName, reportDescription, datasets);
    }

    @Override
    public String toString() {
        return "ReportMetadata{" +
               "reportId='" + reportId + '\'' +
               ", reportName='" + reportName + '\'' +
               ", reportDescription='" + reportDescription + '\'' +
               ", datasets=" + datasets.size() +
               '}';
    }
}
