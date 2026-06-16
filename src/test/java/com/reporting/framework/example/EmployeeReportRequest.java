package com.reporting.framework.example;

import java.time.LocalDate;

/**
 * Example input POJO for Employee Report parameters.
 * Demonstrates type-safe POJO-based input parameters.
 */
public class EmployeeReportRequest {

    private Integer departmentId;
    private LocalDate startDate;

    public EmployeeReportRequest() {
    }

    public EmployeeReportRequest(Integer departmentId, LocalDate startDate) {
        this.departmentId = departmentId;
        this.startDate = startDate;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    @Override
    public String toString() {
        return "EmployeeReportRequest{" +
                "departmentId=" + departmentId +
                ", startDate=" + startDate +
                '}';
    }
}
