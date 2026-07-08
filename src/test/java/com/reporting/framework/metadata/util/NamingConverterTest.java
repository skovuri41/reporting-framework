package com.reporting.framework.metadata.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for NamingConverter utility class.
 * Validates automatic conversion from various database naming conventions to camelCase.
 */
class NamingConverterTest {

    @Test
    void testSnakeCaseToCamelCase() {
        assertThat(NamingConverter.toCamelCase("employee_id")).isEqualTo("employeeId");
        assertThat(NamingConverter.toCamelCase("department_name")).isEqualTo("departmentName");
        assertThat(NamingConverter.toCamelCase("total_sales_amount")).isEqualTo("totalSalesAmount");
    }

    @Test
    void testUpperSnakeCaseToCamelCase() {
        assertThat(NamingConverter.toCamelCase("EMPLOYEE_ID")).isEqualTo("employeeId");
        assertThat(NamingConverter.toCamelCase("DEPARTMENT_NAME")).isEqualTo("departmentName");
        assertThat(NamingConverter.toCamelCase("TOTAL_SALES_AMOUNT")).isEqualTo("totalSalesAmount");
    }

    @Test
    void testPascalCaseToCamelCase() {
        assertThat(NamingConverter.toCamelCase("EmployeeId")).isEqualTo("employeeId");
        assertThat(NamingConverter.toCamelCase("DepartmentName")).isEqualTo("departmentName");
        assertThat(NamingConverter.toCamelCase("TotalSalesAmount")).isEqualTo("totalSalesAmount");
    }

    @Test
    void testAlreadyCamelCase() {
        assertThat(NamingConverter.toCamelCase("employeeId")).isEqualTo("employeeId");
        assertThat(NamingConverter.toCamelCase("departmentName")).isEqualTo("departmentName");
        assertThat(NamingConverter.toCamelCase("totalSalesAmount")).isEqualTo("totalSalesAmount");
    }

    @Test
    void testSingleCharacter() {
        assertThat(NamingConverter.toCamelCase("X")).isEqualTo("x");
        assertThat(NamingConverter.toCamelCase("x")).isEqualTo("x");
        assertThat(NamingConverter.toCamelCase("a")).isEqualTo("a");
    }

    @Test
    void testWithNumbers() {
        assertThat(NamingConverter.toCamelCase("PARAM_1_VALUE")).isEqualTo("param1Value");
        assertThat(NamingConverter.toCamelCase("employee_id_2")).isEqualTo("employeeId2");
        assertThat(NamingConverter.toCamelCase("value123")).isEqualTo("value123");
    }

    @Test
    void testNullAndEmpty() {
        assertThat(NamingConverter.toCamelCase(null)).isNull();
        assertThat(NamingConverter.toCamelCase("")).isEmpty();
    }

    @Test
    void testLowercaseNoUnderscore() {
        assertThat(NamingConverter.toCamelCase("name")).isEqualTo("name");
        assertThat(NamingConverter.toCamelCase("id")).isEqualTo("id");
    }

    @ParameterizedTest
    @CsvSource({
        "Department_Id, departmentId",
        "DEPARTMENT_ID, departmentId",
        "DepartmentId, departmentId",
        "departmentId, departmentId",
        "start_date, startDate",
        "START_DATE, startDate",
        "StartDate, startDate",
        "employee_count, employeeCount",
        "EMPLOYEE_COUNT, employeeCount",
        "EmployeeCount, employeeCount",
        "sale_amount, saleAmount",
        "hire_date, hireDate",
        "total_records, totalRecords",
        "TOTAL_RECORDS, totalRecords"
    })
    void testVariousFormats(String input, String expected) {
        assertThat(NamingConverter.toCamelCase(input)).isEqualTo(expected);
    }

    @Test
    void testMixedCaseUnderscore() {
        // Mixed case with underscores should be normalized via UPPER_UNDERSCORE
        assertThat(NamingConverter.toCamelCase("Employee_Name")).isEqualTo("employeeName");
        assertThat(NamingConverter.toCamelCase("Department_ID")).isEqualTo("departmentId");
    }

    @Test
    void testRealWorldExamples() {
        // Examples from the specification
        assertThat(NamingConverter.toCamelCase("employee_id")).isEqualTo("employeeId");
        assertThat(NamingConverter.toCamelCase("full_name")).isEqualTo("fullName");
        assertThat(NamingConverter.toCamelCase("department_name")).isEqualTo("departmentName");
        assertThat(NamingConverter.toCamelCase("hire_date")).isEqualTo("hireDate");
        assertThat(NamingConverter.toCamelCase("sale_id")).isEqualTo("saleId");
        assertThat(NamingConverter.toCamelCase("employee_name")).isEqualTo("employeeName");
        assertThat(NamingConverter.toCamelCase("sale_amount")).isEqualTo("saleAmount");
        assertThat(NamingConverter.toCamelCase("sale_month")).isEqualTo("saleMonth");
    }
}
