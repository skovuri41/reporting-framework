package com.reporting.framework.metadata.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable DTO representing a catalog of all available report metadata.
 * <p>
 * This wrapper class contains a collection of all report metadata entries
 * along with a count field for convenience. Used when retrieving the complete
 * catalog of available reports.
 * </p>
 * <p>
 * This class follows the immutability principle - all fields are final,
 * the reports list is wrapped in an unmodifiable list, and defensive copying
 * is performed in the constructor.
 * </p>
 */
public class ReportCatalog {
    private final List<ReportMetadata> reports;
    private final Integer count;

    /**
     * Constructs an immutable ReportCatalog instance.
     * <p>
     * This constructor is used by Jackson for JSON deserialization.
     * Performs defensive copying of the reports list and automatically
     * sets the count field to match the list size.
     * </p>
     *
     * @param reports list of all available report metadata
     * @param count total number of reports (must match reports.size())
     * @throws NullPointerException if reports is null
     * @throws IllegalArgumentException if count doesn't match reports.size()
     */
    @JsonCreator
    public ReportCatalog(
        @JsonProperty("reports") List<ReportMetadata> reports,
        @JsonProperty("count") Integer count
    ) {
        Objects.requireNonNull(reports, "reports cannot be null");
        this.reports = Collections.unmodifiableList(new ArrayList<>(reports));
        this.count = count != null ? count : reports.size();

        if (!this.count.equals(this.reports.size())) {
            throw new IllegalArgumentException(
                "Count field (" + this.count + ") does not match reports list size (" +
                this.reports.size() + ")"
            );
        }
    }

    /**
     * Constructs an immutable ReportCatalog instance with automatic count.
     * <p>
     * This convenience constructor automatically sets the count field to
     * the size of the reports list.
     * </p>
     *
     * @param reports list of all available report metadata
     * @throws NullPointerException if reports is null
     */
    public ReportCatalog(List<ReportMetadata> reports) {
        this(reports, reports != null ? reports.size() : 0);
    }

    /**
     * Returns the list of all available report metadata.
     * <p>
     * The returned list is unmodifiable.
     * </p>
     *
     * @return unmodifiable list of report metadata (may be empty)
     */
    public List<ReportMetadata> getReports() {
        return reports;
    }

    /**
     * Returns the total number of reports in the catalog.
     * <p>
     * This value always matches the size of the reports list.
     * </p>
     *
     * @return the count of reports
     */
    public Integer getCount() {
        return count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportCatalog that = (ReportCatalog) o;
        return Objects.equals(reports, that.reports) &&
               Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reports, count);
    }

    @Override
    public String toString() {
        return "ReportCatalog{" +
               "count=" + count +
               ", reports=" + reports.size() +
               '}';
    }
}
