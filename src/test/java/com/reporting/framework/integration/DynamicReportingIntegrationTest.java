package com.reporting.framework.integration;

import com.reporting.framework.api.ReportService;
import com.reporting.framework.connection.SimpleConnectionProvider;
import com.reporting.framework.data.DataOperations;
import com.reporting.framework.data.DataQuery;
import com.reporting.framework.data.DataRow;
import com.reporting.framework.data.DataSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
 *
 * NOTE: This test is disabled due to H2's limited support for callable stored procedures.
 * H2's CREATE ALIAS creates functions that cannot be called via CallableStatement.
 * See FullWorkflowIntegrationTest for a working integration test that demonstrates
 * the DataSet transformation layer.
 */
@Disabled("H2 does not support callable stored procedures - see FullWorkflowIntegrationTest instead")
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
        DataSet result = reportService.executeDynamic("employees");

        assertThat(result.count()).isEqualTo(5);
        assertThat(result.isEmpty()).isFalse();

        DataRow firstRow = result.first();
        assertThat(firstRow.keys()).contains("EMPLOYEE_ID", "NAME", "DEPARTMENT_ID", "SALARY");
    }

    @Test
    void testDynamicExecutionWithParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put("minSalary", 85000);

        DataSet result = reportService.executeDynamic("high_earners", params);

        assertThat(result.count()).isEqualTo(3); // Bob (90k), Charlie (85k), David (95k)

        // Verify all returned employees have salary >= 85000
        for (DataRow row : result.getRows()) {
            BigDecimal salary = row.getBigDecimal("SALARY");
            assertThat(salary.compareTo(BigDecimal.valueOf(85000))).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void testFilterTransformation() {
        DataSet allEmployees = reportService.executeDynamic("employees");

        // Filter for Engineering department (dept_id = 10)
        DataSet engineers = DataQuery.from(allEmployees)
                .filter(row -> row.getInt("DEPARTMENT_ID") == 10)
                .execute();

        assertThat(engineers.count()).isEqualTo(3); // Alice, Charlie, Eve
    }

    @Test
    void testSelectTransformation() {
        DataSet allEmployees = reportService.executeDynamic("employees");

        // Select only name and salary columns
        DataSet namesSalaries = DataQuery.from(allEmployees)
                .select("NAME", "SALARY")
                .execute();

        assertThat(namesSalaries.count()).isEqualTo(5);

        DataRow firstRow = namesSalaries.first();
        assertThat(firstRow.keys()).containsOnly("NAME", "SALARY");
    }

    @Test
    void testGroupByWithAggregations() {
        DataSet employees = reportService.executeDynamic("employees");

        // Group by department and calculate average salary and count
        DataSet summary = DataQuery.from(employees)
                .groupBy("DEPARTMENT_ID")
                .avg("SALARY")
                .count("EMPLOYEE_ID")
                .execute();

        assertThat(summary.count()).isEqualTo(2); // Engineering and Sales

        // Find the engineering and sales rows
        DataRow engineering = summary.getRows().stream()
                .filter(row -> row.getInt("DEPARTMENT_ID") == 10)
                .findFirst().orElseThrow();

        DataRow sales = summary.getRows().stream()
                .filter(row -> row.getInt("DEPARTMENT_ID") == 20)
                .findFirst().orElseThrow();

        // Engineering: Alice (80k), Charlie (85k), Eve (82k) = avg 82.33k
        assertThat(engineering.getLong("EMPLOYEE_ID_count")).isEqualTo(3L);
        assertThat(engineering.getDouble("SALARY_avg")).isCloseTo(82333.33, within(10.0));

        // Sales: Bob (90k), David (95k) = avg 92.5k
        assertThat(sales.getLong("EMPLOYEE_ID_count")).isEqualTo(2L);
        assertThat(sales.getDouble("SALARY_avg")).isCloseTo(92500.0, within(10.0));
    }

    @Test
    void testInnerJoin() {
        DataSet employees = reportService.executeDynamic("employees");
        DataSet departments = reportService.executeDynamic("departments");

        // Join employees with departments
        DataSet joined = DataOperations.innerJoin(
                employees, departments, "DEPARTMENT_ID", "DEPARTMENT_ID");

        assertThat(joined.count()).isEqualTo(5);

        // Verify joined data contains both employee and department info
        DataRow firstRow = joined.first();
        assertThat(firstRow.keys()).contains("NAME", "SALARY", "DEPARTMENT_NAME");
    }

    @Test
    void testComplexWorkflow() {
        // Complex workflow: Filter -> Join -> Select -> Sort (demonstrating fluent DSL)
        DataSet employees = reportService.executeDynamic("employees");
        DataSet departments = reportService.executeDynamic("departments");

        // Step 1: Filter high earners (>= 85k)
        DataSet highEarners = DataQuery.from(employees)
                .filter(row -> row.getBigDecimal("SALARY").compareTo(BigDecimal.valueOf(85000)) >= 0)
                .execute();

        assertThat(highEarners.count()).isEqualTo(3); // Bob, Charlie, David

        // Step 2: Join with departments
        DataSet joined = DataOperations.innerJoin(
                highEarners, departments, "DEPARTMENT_ID", "DEPARTMENT_ID");

        assertThat(joined.count()).isEqualTo(3);

        // Step 3 & 4: Select specific columns and sort by salary descending (chained)
        DataSet sorted = DataQuery.from(joined)
                .select("NAME", "SALARY", "DEPARTMENT_NAME")
                .orderBy("SALARY").desc()
                .execute();

        // Verify results
        List<DataRow> rows = sorted.getRows();
        assertThat(rows.get(0).getString("NAME")).isEqualTo("David");  // 95k
        assertThat(rows.get(1).getString("NAME")).isEqualTo("Bob");    // 90k
        assertThat(rows.get(2).getString("NAME")).isEqualTo("Charlie"); // 85k
    }

    @Test
    void testJSONOutput() {
        DataSet result = reportService.executeDynamic("departments");

        String json = result.toJSON();

        assertThat(json)
                .contains("Engineering")
                .contains("Sales")
                .contains("Marketing");
    }

    @Test
    void testWithComputedColumn() {
        DataSet employees = reportService.executeDynamic("employees");

        // Add a bonus column (10% of salary)
        DataSet withBonus = DataQuery.from(employees)
                .withColumn("BONUS", row -> row.getBigDecimal("SALARY").multiply(BigDecimal.valueOf(0.1)))
                .execute();

        assertThat(withBonus.count()).isEqualTo(5);

        DataRow firstRow = withBonus.first();
        assertThat(firstRow.has("BONUS")).isTrue();

        BigDecimal salary = firstRow.getBigDecimal("SALARY");
        BigDecimal bonus = firstRow.getBigDecimal("BONUS");
        assertThat(bonus).isEqualTo(salary.multiply(BigDecimal.valueOf(0.1)));
    }
}
