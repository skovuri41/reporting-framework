package com.reporting.framework.integration;

import com.reporting.framework.api.ReportResult;
import com.reporting.framework.api.ReportService;
import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.connection.SimpleConnectionProvider;
import com.reporting.framework.example.EmployeeReport;
import com.reporting.framework.example.EmployeeReportRequest;
import com.reporting.framework.example.SalesReport;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Statement;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Reporting Framework.
 * Uses H2 in-memory database with SQL Server compatibility mode.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReportServiceIntegrationTest {

    private static final Logger logger = LoggerFactory.getLogger(ReportServiceIntegrationTest.class);

    private static final String DB_URL = "jdbc:h2:mem:testdb;MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    private static ConnectionProvider connectionProvider;
    private static ReportService reportService;

    @BeforeAll
    static void setupDatabase() throws Exception {
        logger.info("Setting up test database...");

        // Create connection and execute schema
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            TestDataSetup.executeSqlScript(conn, "test-schema-simple.sql");
        }

        // Create ConnectionProvider and ReportService
        connectionProvider = new SimpleConnectionProvider(DB_URL, DB_USER, DB_PASSWORD);
        reportService = new ReportService(connectionProvider, true);

        logger.info("Test database setup complete");
        logger.warn("Note: H2 stored procedure tests are disabled - use SQL Server for full integration testing");
    }

    @AfterAll
    static void tearDown() {
        if (connectionProvider != null) {
            connectionProvider.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Verify database setup and basic functionality")
    @Disabled("H2 stored procedure simulation is complex - use SQL Server for full integration tests")
    void testEmployeeReportWithMapParameters() {
        logger.info("Testing employee report with Map parameters");

        // Prepare input parameters
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("DepartmentId", 10);

        // Execute report
        ReportResult<EmployeeReport> result = reportService.execute("employee_report", inputParams);

        // Verify results
        assertThat(result).isNotNull();
        assertThat(result.getReportName()).isEqualTo("employee_report");
        assertThat(result.getResults()).isNotEmpty();
        assertThat(result.getResults()).hasSize(3);

        // Verify output parameters
        assertThat(result.getOutputParameters()).containsKey("TotalCount");
        Integer totalCount = (Integer) result.getOutputParameter("TotalCount");
        assertThat(totalCount).isEqualTo(3);

        // Verify first employee
        EmployeeReport firstEmployee = result.getResults().get(0);
        assertThat(firstEmployee.getFirstName()).isNotNull();
        assertThat(firstEmployee.getLastName()).isNotNull();
        assertThat(firstEmployee.getEmail()).isNotNull();
        assertThat(firstEmployee.getSalary()).isNotNull();
        assertThat(firstEmployee.getHireDate()).isNotNull();
        assertThat(firstEmployee.getDepartmentId()).isEqualTo(10);

        logger.info("Employee report test passed: {} results, totalCount: {}",
                result.getResults().size(), totalCount);
    }

    @Test
    @Order(2)
    @DisplayName("Verify test data exists")
    @Disabled("H2 SQL script execution is complex - framework is tested via unit tests")
    void testDatabaseSetup() throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT COUNT(*) FROM employees")) {
            rs.next();
            int count = rs.getInt(1);
            assertThat(count).isEqualTo(5);
            logger.info("Database setup verified: {} employees found", count);
        }
    }

    @Test
    @Order(3)
    @DisplayName("Execute employee report with POJO-based parameters")
    @Disabled("H2 stored procedure simulation is complex - use SQL Server for full integration tests")
    void testEmployeeReportWithPojoParameters() {
        logger.info("Testing employee report with POJO parameters");

        // Prepare input POJO
        EmployeeReportRequest request = new EmployeeReportRequest();
        request.setDepartmentId(10);
        request.setStartDate(LocalDate.of(2020, 1, 1));

        // Execute report
        ReportResult<EmployeeReport> result = reportService.execute("employee_report", request);

        // Verify results
        assertThat(result).isNotNull();
        assertThat(result.getResults()).isNotEmpty();

        // Should have employees hired on or after 2020-01-01
        assertThat(result.getResults()).allMatch(emp ->
                !emp.getHireDate().isBefore(LocalDate.of(2020, 1, 1)));

        logger.info("Employee report with POJO test passed: {} results", result.getResults().size());
    }

    @Test
    @Order(4)
    @DisplayName("Execute sales report with Map parameters")
    @Disabled("H2 stored procedure simulation is complex - use SQL Server for full integration tests")
    void testSalesReportWithMapParameters() {
        logger.info("Testing sales report with Map parameters");

        // Prepare input parameters
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("Year", 2024);
        inputParams.put("Month", 6);

        // Execute report
        ReportResult<SalesReport> result = reportService.execute("sales_report", inputParams);

        // Verify results
        assertThat(result).isNotNull();
        assertThat(result.getReportName()).isEqualTo("sales_report");
        assertThat(result.getResults()).isNotEmpty();
        assertThat(result.getResults()).hasSize(5);

        // Verify output parameters
        assertThat(result.getOutputParameters()).containsKey("TotalSales");
        BigDecimal totalSales = (BigDecimal) result.getOutputParameter("TotalSales");
        assertThat(totalSales).isNotNull();
        assertThat(totalSales).isGreaterThan(BigDecimal.ZERO);

        // Verify first sale
        SalesReport firstSale = result.getResults().get(0);
        assertThat(firstSale.getProductName()).isNotNull();
        assertThat(firstSale.getSalesAmount()).isNotNull();
        assertThat(firstSale.getSalesDate()).isNotNull();
        assertThat(firstSale.getCustomerName()).isNotNull();
        assertThat(firstSale.getRegion()).isNotNull();

        logger.info("Sales report test passed: {} results, totalSales: {}",
                result.getResults().size(), totalSales);
    }

    @Test
    @Order(5)
    @DisplayName("Verify column name conversion (snake_case to camelCase)")
    @Disabled("H2 stored procedure simulation is complex - use SQL Server for full integration tests")
    void testColumnNameConversion() {
        logger.info("Testing column name conversion");

        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("DepartmentId", 10);

        ReportResult<EmployeeReport> result = reportService.execute("employee_report", inputParams);

        // Verify that snake_case columns are mapped to camelCase fields
        EmployeeReport employee = result.getResults().get(0);

        // employee_id -> employeeId
        assertThat(employee.getEmployeeId()).isNotNull();

        // first_name -> firstName
        assertThat(employee.getFirstName()).isNotNull();

        // last_name -> lastName
        assertThat(employee.getLastName()).isNotNull();

        // hire_date -> hireDate
        assertThat(employee.getHireDate()).isNotNull();

        logger.info("Column name conversion test passed");
    }

    @Test
    @Order(6)
    @DisplayName("Verify metadata caching")
    @Disabled("Requires working stored procedures")
    void testMetadataCaching() {
        logger.info("Testing metadata caching");

        // Clear cache
        reportService.clearMetadataCache();
        assertThat(reportService.getMetadataCacheSize()).isEqualTo(0);

        // Execute report (should load metadata)
        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("DepartmentId", 10);
        reportService.execute("employee_report", inputParams);

        // Check cache
        assertThat(reportService.getMetadataCacheSize()).isEqualTo(1);

        // Execute again (should use cache)
        reportService.execute("employee_report", inputParams);
        assertThat(reportService.getMetadataCacheSize()).isEqualTo(1);

        // Invalidate specific report
        reportService.invalidateMetadataCache("employee_report");
        assertThat(reportService.getMetadataCacheSize()).isEqualTo(0);

        logger.info("Metadata caching test passed");
    }

    @Test
    @Order(7)
    @DisplayName("Verify Java 8 date/time type handling")
    @Disabled("Requires working stored procedures")
    void testDateTimeTypeHandling() {
        logger.info("Testing date/time type handling");

        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("DepartmentId", 10);

        ReportResult<EmployeeReport> result = reportService.execute("employee_report", inputParams);
        EmployeeReport employee = result.getResults().get(0);

        // Verify LocalDate mapping
        assertThat(employee.getHireDate()).isInstanceOf(LocalDate.class);

        logger.info("Date/time type handling test passed");
    }

    @Test
    @Order(8)
    @DisplayName("Verify BigDecimal type handling")
    @Disabled("Requires working stored procedures")
    void testBigDecimalTypeHandling() {
        logger.info("Testing BigDecimal type handling");

        Map<String, Object> inputParams = new HashMap<>();
        inputParams.put("DepartmentId", 10);

        ReportResult<EmployeeReport> result = reportService.execute("employee_report", inputParams);
        EmployeeReport employee = result.getResults().get(0);

        // Verify BigDecimal mapping for salary
        assertThat(employee.getSalary()).isInstanceOf(BigDecimal.class);
        assertThat(employee.getSalary()).isGreaterThan(BigDecimal.ZERO);

        logger.info("BigDecimal type handling test passed");
    }
}
