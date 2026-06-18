package com.reporting.framework.integration;

import com.reporting.framework.api.DynamicReportResult;
import com.reporting.framework.api.ReportService;
import com.reporting.framework.connection.SimpleConnectionProvider;
import com.reporting.framework.transform.ReportTransformer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test demonstrating the dynamic reporting workflow.
 * Uses H2 in-memory database to test the full stack.
 */
class DynamicReportingIntegrationTest {

    private static SimpleConnectionProvider connectionProvider;
    private static ReportService reportService;

    @BeforeAll
    static void setUp() throws Exception {
        // Create in-memory H2 database
        String jdbcUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MSSQLServer";
        String username = "sa";
        String password = "";

        connectionProvider = new SimpleConnectionProvider(jdbcUrl, username, password);
        reportService = new ReportService(connectionProvider);

        // Create test schema
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create metadata table
            stmt.execute("CREATE TABLE REPORT_METADATA (" +
                    "REPORT_NAME VARCHAR(100) PRIMARY KEY," +
                    "STORED_PROCEDURE VARCHAR(200)," +
                    "RESULT_CLASS VARCHAR(500)," +
                    "METADATA_JSON VARCHAR(5000)" +
                    ")");

            // Create test tables
            stmt.execute("CREATE TABLE employees (" +
                    "employee_id INT PRIMARY KEY," +
                    "name VARCHAR(100)," +
                    "department_id INT," +
                    "salary DECIMAL(10,2)" +
                    ")");

            stmt.execute("CREATE TABLE departments (" +
                    "department_id INT PRIMARY KEY," +
                    "department_name VARCHAR(100)" +
                    ")");

            // Insert test data - departments
            stmt.execute("INSERT INTO departments VALUES (10, 'Engineering')");
            stmt.execute("INSERT INTO departments VALUES (20, 'Sales')");
            stmt.execute("INSERT INTO departments VALUES (30, 'Marketing')");

            // Insert test data - employees
            stmt.execute("INSERT INTO employees VALUES (1, 'Alice', 10, 80000)");
            stmt.execute("INSERT INTO employees VALUES (2, 'Bob', 20, 90000)");
            stmt.execute("INSERT INTO employees VALUES (3, 'Charlie', 10, 85000)");
            stmt.execute("INSERT INTO employees VALUES (4, 'David', 20, 95000)");
            stmt.execute("INSERT INTO employees VALUES (5, 'Eve', 10, 82000)");

            // Create simple stored procedures (H2 syntax)
            stmt.execute("CREATE ALIAS GET_EMPLOYEES AS $$" +
                    "java.sql.ResultSet getEmployees(java.sql.Connection conn) " +
                    "throws java.sql.SQLException { " +
                    "  return conn.createStatement().executeQuery(\"SELECT * FROM employees\"); " +
                    "} $$");

            stmt.execute("CREATE ALIAS GET_DEPARTMENTS AS $$" +
                    "java.sql.ResultSet getDepartments(java.sql.Connection conn) " +
                    "throws java.sql.SQLException { " +
                    "  return conn.createStatement().executeQuery(\"SELECT * FROM departments\"); " +
                    "} $$");

            stmt.execute("CREATE ALIAS GET_HIGH_EARNERS AS $$" +
                    "java.sql.ResultSet getHighEarners(java.sql.Connection conn, int minSalary) " +
                    "throws java.sql.SQLException { " +
                    "  return conn.createStatement().executeQuery(" +
                    "    \"SELECT * FROM employees WHERE salary >= \" + minSalary); " +
                    "} $$");

            // Insert metadata for reports
            stmt.execute("INSERT INTO REPORT_METADATA VALUES (" +
                    "'employees', " +
                    "'GET_EMPLOYEES', " +
                    "'java.util.Map', " +
                    "'{\"reportName\":\"employees\"," +
                    "\"storedProcedure\":\"GET_EMPLOYEES\"," +
                    "\"resultClass\":\"java.util.Map\"," +
                    "\"inputParameters\":[]," +
                    "\"outputParameters\":[]}'" +
                    ")");

            stmt.execute("INSERT INTO REPORT_METADATA VALUES (" +
                    "'departments', " +
                    "'GET_DEPARTMENTS', " +
                    "'java.util.Map', " +
                    "'{\"reportName\":\"departments\"," +
                    "\"storedProcedure\":\"GET_DEPARTMENTS\"," +
                    "\"resultClass\":\"java.util.Map\"," +
                    "\"inputParameters\":[]," +
                    "\"outputParameters\":[]}'" +
                    ")");

            stmt.execute("INSERT INTO REPORT_METADATA VALUES (" +
                    "'high_earners', " +
                    "'GET_HIGH_EARNERS', " +
                    "'java.util.Map', " +
                    "'{\"reportName\":\"high_earners\"," +
                    "\"storedProcedure\":\"GET_HIGH_EARNERS\"," +
                    "\"resultClass\":\"java.util.Map\"," +
                    "\"inputParameters\":[" +
                    "{\"name\":\"minSalary\",\"sqlType\":\"INTEGER\",\"javaType\":\"java.lang.Integer\",\"required\":true}" +
                    "]," +
                    "\"outputParameters\":[]}'" +
                    ")");
        }
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (connectionProvider != null) {
            try (Connection conn = connectionProvider.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("SHUTDOWN");
            }
        }
    }

    @Test
    void testBasicDynamicExecution() {
        DynamicReportResult result = reportService.executeDynamic("employees");

        assertThat(result.count()).isEqualTo(5);
        assertThat(result.isEmpty()).isFalse();

        Map<String, Object> firstRow = result.first();
        assertThat(firstRow).containsKeys("employee_id", "name", "department_id", "salary");
    }

    @Test
    void testDynamicExecutionWithParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("minSalary", 85000);

        DynamicReportResult result = reportService.executeDynamic("high_earners", params);

        assertThat(result.count()).isEqualTo(3); // Bob (90k), Charlie (85k), David (95k)

        // Verify all returned employees have salary >= 85000
        for (Map<String, Object> row : result.getRows()) {
            BigDecimal salary = (BigDecimal) row.get("SALARY");
            assertThat(salary.compareTo(BigDecimal.valueOf(85000))).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void testFilterTransformation() {
        DynamicReportResult allEmployees = reportService.executeDynamic("employees");

        // Filter for Engineering department (dept_id = 10)
        DynamicReportResult engineers = allEmployees.filter(row ->
                ((Integer) row.get("DEPARTMENT_ID")) == 10
        );

        assertThat(engineers.count()).isEqualTo(3); // Alice, Charlie, Eve
    }

    @Test
    void testSelectTransformation() {
        DynamicReportResult allEmployees = reportService.executeDynamic("employees");

        // Select only name and salary columns
        DynamicReportResult namesSalaries = allEmployees.select("NAME", "SALARY");

        assertThat(namesSalaries.count()).isEqualTo(5);

        Map<String, Object> firstRow = namesSalaries.first();
        assertThat(firstRow).containsOnlyKeys("NAME", "SALARY");
    }

    @Test
    void testGroupByWithAggregations() {
        DynamicReportResult employees = reportService.executeDynamic("employees");

        // Group by department and calculate average salary and count
        Map<String, DynamicReportResult.AggregationFunction> aggregations = new HashMap<>();
        aggregations.put("SALARY", DynamicReportResult.AggregationFunction.AVG);
        aggregations.put("EMPLOYEE_ID", DynamicReportResult.AggregationFunction.COUNT);

        Map<Object, Map<String, Object>> summary = employees.groupByWithAggregations(
                "DEPARTMENT_ID", aggregations);

        assertThat(summary).hasSize(2); // Engineering and Sales

        // Engineering: Alice (80k), Charlie (85k), Eve (82k) = avg 82.33k
        Map<String, Object> engineering = summary.get(10);
        assertThat(engineering).containsEntry("EMPLOYEE_ID_count", 3L);
        assertThat((Double) engineering.get("SALARY_avg"))
                .isCloseTo(82333.33, within(10.0));

        // Sales: Bob (90k), David (95k) = avg 92.5k
        Map<String, Object> sales = summary.get(20);
        assertThat(sales).containsEntry("EMPLOYEE_ID_count", 2L);
        assertThat((Double) sales.get("SALARY_avg"))
                .isCloseTo(92500.0, within(10.0));
    }

    @Test
    void testInnerJoin() {
        DynamicReportResult employees = reportService.executeDynamic("employees");
        DynamicReportResult departments = reportService.executeDynamic("departments");

        // Join employees with departments
        DynamicReportResult joined = ReportTransformer.innerJoin(
                employees, departments, "DEPARTMENT_ID", "DEPARTMENT_ID");

        assertThat(joined.count()).isEqualTo(5);

        // Verify joined data contains both employee and department info
        Map<String, Object> firstRow = joined.first();
        assertThat(firstRow).containsKeys("NAME", "SALARY", "DEPARTMENT_NAME");
    }

    @Test
    void testComplexWorkflow() {
        // Complex workflow: Filter -> Join -> Aggregate
        DynamicReportResult employees = reportService.executeDynamic("employees");
        DynamicReportResult departments = reportService.executeDynamic("departments");

        // Step 1: Filter high earners (>= 85k)
        DynamicReportResult highEarners = employees.filter(row -> {
            BigDecimal salary = (BigDecimal) row.get("SALARY");
            return salary.compareTo(BigDecimal.valueOf(85000)) >= 0;
        });

        assertThat(highEarners.count()).isEqualTo(3); // Bob, Charlie, David

        // Step 2: Join with departments
        DynamicReportResult joined = ReportTransformer.innerJoin(
                highEarners, departments, "DEPARTMENT_ID", "DEPARTMENT_ID");

        assertThat(joined.count()).isEqualTo(3);

        // Step 3: Select specific columns
        DynamicReportResult final_result = joined.select(
                "NAME", "SALARY", "DEPARTMENT_NAME");

        // Step 4: Sort by salary descending
        DynamicReportResult sorted = final_result.orderBy("SALARY", false);

        // Verify results
        List<Map<String, Object>> rows = sorted.getRows();
        assertThat(rows.get(0).get("NAME")).isEqualTo("David");  // 95k
        assertThat(rows.get(1).get("NAME")).isEqualTo("Bob");    // 90k
        assertThat(rows.get(2).get("NAME")).isEqualTo("Charlie"); // 85k
    }

    @Test
    void testJSONOutput() {
        DynamicReportResult result = reportService.executeDynamic("departments");

        String json = result.toJSON();

        assertThat(json)
                .contains("Engineering")
                .contains("Sales")
                .contains("Marketing");
    }

    @Test
    void testWithComputedColumn() {
        DynamicReportResult employees = reportService.executeDynamic("employees");

        // Add a bonus column (10% of salary)
        DynamicReportResult withBonus = employees.withColumn("BONUS", row -> {
            BigDecimal salary = (BigDecimal) row.get("SALARY");
            return salary.multiply(BigDecimal.valueOf(0.1));
        });

        assertThat(withBonus.count()).isEqualTo(5);

        Map<String, Object> firstRow = withBonus.first();
        assertThat(firstRow).containsKey("BONUS");

        BigDecimal salary = (BigDecimal) firstRow.get("SALARY");
        BigDecimal bonus = (BigDecimal) firstRow.get("BONUS");
        assertThat(bonus).isEqualTo(salary.multiply(BigDecimal.valueOf(0.1)));
    }
}
