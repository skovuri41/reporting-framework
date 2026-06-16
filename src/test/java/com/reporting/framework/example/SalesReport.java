package com.reporting.framework.example;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Example POJO for Sales Report.
 * Demonstrates mapping from database columns (snake_case) to Java fields (camelCase).
 */
public class SalesReport {

    private String productName;
    private BigDecimal salesAmount;
    private LocalDate salesDate;
    private String customerName;
    private String region;

    public SalesReport() {
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }

    public LocalDate getSalesDate() {
        return salesDate;
    }

    public void setSalesDate(LocalDate salesDate) {
        this.salesDate = salesDate;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public String toString() {
        return "SalesReport{" +
                "productName='" + productName + '\'' +
                ", salesAmount=" + salesAmount +
                ", salesDate=" + salesDate +
                ", customerName='" + customerName + '\'' +
                ", region='" + region + '\'' +
                '}';
    }
}
