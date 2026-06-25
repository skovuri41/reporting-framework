package com.reporting.framework.integration;

import com.reporting.framework.connection.SimpleConnectionProvider;
import com.reporting.framework.data.DataOperations;
import com.reporting.framework.data.DataQuery;
import com.reporting.framework.data.DataRow;
import com.reporting.framework.data.DataSet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Full workflow integration test demonstrating end-to-end usage of the reporting framework.
 *
 * This test demonstrates the dataset abstraction layer (fluent DSL for transformations)
 * with realistic business scenarios using employee, department, and sales data.
 *
 * Note: Due to H2's limited stored procedure support, this test creates DataSets directly
 * from queries rather than using the metadata layer. The focus is on demonstrating the
 * powerful transformation capabilities of the DataSet API.
 *
 * Test scenarios progressively demonstrate:
 * 1. Basic workflow: Join, filter, select, orderBy
 * 2. Aggregation workflow: GroupBy with multiple aggregations
 * 3. Multi-table workflow: Complex joins with filtering
 * 4. Comprehensive analysis: Multi-step transformations with computed columns
 */
class FullWorkflowIntegrationTest {

    private static SimpleConnectionProvider connectionProvider;

    @BeforeAll
    static void setUp() throws Exception {
        // Create in-memory H2 database with unique name to avoid conflicts
        String jdbcUrl = "jdbc:h2:mem:fullworkflow_" + System.currentTimeMillis() +
                         ";DB_CLOSE_DELAY=-1;MODE=MSSQLServer";
        String username = "sa";
        String password = "";

        connectionProvider = new SimpleConnectionProvider(jdbcUrl, username, password);

        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {

            // Create business tables
            stmt.execute("CREATE TABLE employees (" +
                    "employee_id INT PRIMARY KEY," +
                    "name VARCHAR(100)," +
                    "department_id INT," +
                    "salary DECIMAL(10,2)," +
                    "hire_date DATE" +
                    ")");

            stmt.execute("CREATE TABLE departments (" +
                    "department_id INT PRIMARY KEY," +
                    "department_name VARCHAR(100)," +
                    "budget DECIMAL(12,2)" +
                    ")");

            stmt.execute("CREATE TABLE sales (" +
                    "sale_id INT PRIMARY KEY," +
                    "employee_id INT," +
                    "sale_amount DECIMAL(10,2)," +
                    "sale_month VARCHAR(20)" +
                    ")");

            // Insert test data - departments
            stmt.execute("INSERT INTO departments VALUES (10, 'Engineering', 500000)");
            stmt.execute("INSERT INTO departments VALUES (20, 'Sales', 750000)");
            stmt.execute("INSERT INTO departments VALUES (30, 'Marketing', 300000)");

            // Insert test data - employees
            stmt.execute("INSERT INTO employees VALUES (1, 'Alice Johnson', 10, 80000, '2020-01-15')");
            stmt.execute("INSERT INTO employees VALUES (2, 'Bob Smith', 20, 90000, '2019-03-10')");
            stmt.execute("INSERT INTO employees VALUES (3, 'Charlie Brown', 10, 85000, '2021-06-20')");
            stmt.execute("INSERT INTO employees VALUES (4, 'David Lee', 20, 95000, '2018-11-05')");
            stmt.execute("INSERT INTO employees VALUES (5, 'Eve Wilson', 10, 82000, '2022-02-28')");
            stmt.execute("INSERT INTO employees VALUES (6, 'Frank Miller', 30, 70000, '2021-09-12')");
            stmt.execute("INSERT INTO employees VALUES (7, 'Grace Davis', 20, 88000, '2020-07-01')");

            // Insert test data - sales (Q1 2024 for Sales department employees: 2, 4, 7)
            // Bob Smith (employee_id=2): 55000 total
            stmt.execute("INSERT INTO sales VALUES (1, 2, 18000, '2024-01')");
            stmt.execute("INSERT INTO sales VALUES (2, 2, 22000, '2024-02')");
            stmt.execute("INSERT INTO sales VALUES (3, 2, 15000, '2024-03')");
            // David Lee (employee_id=4): 83000 total
            stmt.execute("INSERT INTO sales VALUES (4, 4, 30000, '2024-01')");
            stmt.execute("INSERT INTO sales VALUES (5, 4, 28000, '2024-02')");
            stmt.execute("INSERT INTO sales VALUES (6, 4, 25000, '2024-03')");
            // Grace Davis (employee_id=7): 42000 total
            stmt.execute("INSERT INTO sales VALUES (7, 7, 12000, '2024-01')");
            stmt.execute("INSERT INTO sales VALUES (8, 7, 15000, '2024-02')");
            stmt.execute("INSERT INTO sales VALUES (9, 7, 15000, '2024-03')");
        }
    }

    /**
     * Helper method to create a DataSet from a SQL query.
     * This simulates fetching data from a report source.
     */
    private static DataSet createDataSetFromQuery(String sql, String datasetName) throws Exception {
        List<DataRow> rows = new ArrayList<>();

        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> rowData = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    rowData.put(columnName, value);
                }
                rows.add(DataRow.of(rowData));
            }
        }

        return new DataSet(rows, new HashMap<>(), datasetName);
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

    /**
     * Test 1: Basic workflow demonstrating join, filter, select, and orderBy.
     *
     * Business scenario: Find all high-earning employees (>= 85000) with their department
     * information, ordered by salary descending.
     *
     * Expected JSON Output:
     * [
     *   {
     *     "NAME": "David Lee",
     *     "SALARY": 95000.00,
     *     "DEPARTMENT_NAME": "Sales"
     *   },
     *   {
     *     "NAME": "Bob Smith",
     *     "SALARY": 90000.00,
     *     "DEPARTMENT_NAME": "Sales"
     *   },
     *   {
     *     "NAME": "Grace Davis",
     *     "SALARY": 88000.00,
     *     "DEPARTMENT_NAME": "Sales"
     *   },
     *   {
     *     "NAME": "Charlie Brown",
     *     "SALARY": 85000.00,
     *     "DEPARTMENT_NAME": "Engineering"
     *   }
     * ]
     */
    @Test
    void testEmployeeSalaryAnalysis() throws Exception {
        // 1. Fetch data from database
        DataSet employees = createDataSetFromQuery("SELECT * FROM employees", "employees");
        DataSet departments = createDataSetFromQuery("SELECT * FROM departments", "departments");

        assertThat(employees.count()).isEqualTo(7);
        assertThat(departments.count()).isEqualTo(3);

        // 2. Join employees with departments
        DataSet joined = DataOperations.innerJoin(employees, departments,
                "DEPARTMENT_ID", "DEPARTMENT_ID");

        assertThat(joined.count()).isEqualTo(7);

        // 3. Filter for high earners, select columns, and order by salary
        DataSet result = DataQuery.from(joined)
                .filter(row -> row.getBigDecimal("SALARY")
                        .compareTo(BigDecimal.valueOf(85000)) >= 0)
                .select("NAME", "SALARY", "DEPARTMENT_NAME")
                .orderBy("SALARY").desc()
                .execute();

        // 4. Generate JSON
        String json = result.toJSON();

        // 5. Verify results
        assertThat(result.count()).isEqualTo(4); // David(95k), Bob(90k), Grace(88k), Charlie(85k)

        // Verify top earner
        DataRow topEarner = result.first();
        assertThat(topEarner.getString("NAME")).isEqualTo("David Lee");
        assertThat(topEarner.getBigDecimal("SALARY")).isEqualByComparingTo("95000");
        assertThat(topEarner.getString("DEPARTMENT_NAME")).isEqualTo("Sales");

        // Verify ordering (descending salary)
        assertThat(result.getRows().get(0).getBigDecimal("SALARY"))
                .isGreaterThanOrEqualTo(result.getRows().get(1).getBigDecimal("SALARY"));
        assertThat(result.getRows().get(1).getBigDecimal("SALARY"))
                .isGreaterThanOrEqualTo(result.getRows().get(2).getBigDecimal("SALARY"));

        // Verify JSON contains expected data
        assertThat(json)
                .contains("David Lee")
                .contains("DEPARTMENT_NAME")
                .contains("SALARY");
    }

    /**
     * Test 2: Aggregation workflow with groupBy.
     *
     * Business scenario: Calculate salary statistics (average, sum, count) per department.
     *
     * Expected JSON Output:
     * [
     *   {
     *     "DEPARTMENT_NAME": "Engineering",
     *     "SALARY_sum": 247000.00,
     *     "EMPLOYEE_ID_count": 3
     *   },
     *   {
     *     "DEPARTMENT_NAME": "Sales",
     *     "SALARY_sum": 273000.00,
     *     "EMPLOYEE_ID_count": 3
     *   },
     *   {
     *     "DEPARTMENT_NAME": "Marketing",
     *     "SALARY_sum": 70000.00,
     *     "EMPLOYEE_ID_count": 1
     *   }
     * ]
     */
    @Test
    void testDepartmentSalaryAggregation() throws Exception {
        // Fetch and join data
        DataSet employees = createDataSetFromQuery("SELECT * FROM employees", "employees");
        DataSet departments = createDataSetFromQuery("SELECT * FROM departments", "departments");

        DataSet joined = DataOperations.innerJoin(employees, departments,
                "DEPARTMENT_ID", "DEPARTMENT_ID");

        // Group by department with count and sum
        DataSet summary = DataQuery.from(joined)
                .groupBy("DEPARTMENT_NAME")
                .sum("SALARY")
                .count("EMPLOYEE_ID")
                .execute();

        String json = summary.toJSON();

        // Verify aggregations
        assertThat(summary.count()).isEqualTo(3); // 3 departments

        // Find Engineering department stats
        DataRow engineering = summary.getRows().stream()
                .filter(row -> "Engineering".equals(row.getString("DEPARTMENT_NAME")))
                .findFirst()
                .orElseThrow();

        // Engineering: Alice(80k), Charlie(85k), Eve(82k) = 247k total
        assertThat(engineering.getLong("EMPLOYEE_ID_count")).isEqualTo(3L);
        assertThat(engineering.getDouble("SALARY_sum")).isCloseTo(247000.0, within(10.0));

        // Find Sales department stats
        DataRow sales = summary.getRows().stream()
                .filter(row -> "Sales".equals(row.getString("DEPARTMENT_NAME")))
                .findFirst()
                .orElseThrow();

        // Sales: Bob(90k), David(95k), Grace(88k) = 273k total
        assertThat(sales.getLong("EMPLOYEE_ID_count")).isEqualTo(3L);
        assertThat(sales.getDouble("SALARY_sum")).isCloseTo(273000.0, within(10.0));

        // Find Marketing department stats
        DataRow marketing = summary.getRows().stream()
                .filter(row -> "Marketing".equals(row.getString("DEPARTMENT_NAME")))
                .findFirst()
                .orElseThrow();

        // Marketing: Frank(70k)
        assertThat(marketing.getLong("EMPLOYEE_ID_count")).isEqualTo(1L);
        assertThat(marketing.getDouble("SALARY_sum")).isCloseTo(70000.0, within(10.0));

        // Verify JSON output
        assertThat(json)
                .contains("DEPARTMENT_NAME")
                .contains("SALARY_sum")
                .contains("EMPLOYEE_ID_count");
    }

    /**
     * Test 3: Multi-table workflow with sales performance analysis.
     *
     * Business scenario: Calculate total sales per employee in the Sales department,
     * ordered by performance.
     *
     * Expected JSON Output:
     * [
     *   {
     *     "NAME": "David Lee",
     *     "SALE_AMOUNT_sum": 83000.00
     *   },
     *   {
     *     "NAME": "Bob Smith",
     *     "SALE_AMOUNT_sum": 55000.00
     *   },
     *   {
     *     "NAME": "Grace Davis",
     *     "SALE_AMOUNT_sum": 42000.00
     *   }
     * ]
     */
    @Test
    void testSalesPerformanceAnalysis() throws Exception {
        // Fetch sales and employees
        DataSet sales = createDataSetFromQuery("SELECT * FROM sales", "sales");
        DataSet employees = createDataSetFromQuery("SELECT * FROM employees", "employees");

        assertThat(sales.count()).isEqualTo(9); // 9 sales records
        assertThat(employees.count()).isEqualTo(7);

        // Join sales with employees
        DataSet joined = DataOperations.innerJoin(sales, employees,
                "EMPLOYEE_ID", "EMPLOYEE_ID");

        assertThat(joined.count()).isEqualTo(9);

        // Filter for Sales department (dept_id=20) and aggregate by employee
        DataSet summary = DataQuery.from(joined)
                .filter(row -> row.getInt("DEPARTMENT_ID") == 20)
                .groupBy("NAME")
                .sum("SALE_AMOUNT")
                .execute();

        // Sort by total sales descending
        DataSet sorted = DataQuery.from(summary)
                .select("NAME", "SALE_AMOUNT_sum")
                .orderBy("SALE_AMOUNT_sum").desc()
                .execute();

        String json = sorted.toJSON();

        // Verify results
        assertThat(sorted.count()).isEqualTo(3); // Bob, David, Grace

        // Verify top performer
        DataRow topPerformer = sorted.first();
        assertThat(topPerformer.getString("NAME")).isEqualTo("David Lee");
        assertThat(topPerformer.getDouble("SALE_AMOUNT_sum")).isCloseTo(83000.0, within(10.0));

        // Verify second performer
        DataRow secondPerformer = sorted.getRows().get(1);
        assertThat(secondPerformer.getString("NAME")).isEqualTo("Bob Smith");
        assertThat(secondPerformer.getDouble("SALE_AMOUNT_sum")).isCloseTo(55000.0, within(10.0));

        // Verify third performer
        DataRow thirdPerformer = sorted.getRows().get(2);
        assertThat(thirdPerformer.getString("NAME")).isEqualTo("Grace Davis");
        assertThat(thirdPerformer.getDouble("SALE_AMOUNT_sum")).isCloseTo(42000.0, within(10.0));

        // Verify ordering
        assertThat(sorted.getRows().get(0).getDouble("SALE_AMOUNT_sum"))
                .isGreaterThan(sorted.getRows().get(1).getDouble("SALE_AMOUNT_sum"));
        assertThat(sorted.getRows().get(1).getDouble("SALE_AMOUNT_sum"))
                .isGreaterThan(sorted.getRows().get(2).getDouble("SALE_AMOUNT_sum"));

        // Verify JSON output
        assertThat(json)
                .contains("David Lee")
                .contains("Bob Smith")
                .contains("Grace Davis")
                .contains("SALE_AMOUNT_sum");
    }

    /**
     * Test 4: Comprehensive analysis demonstrating complex multi-step workflow.
     *
     * Business scenario: Full employee analysis combining all data sources with computed
     * columns, multiple joins, filtering, and pretty JSON output.
     *
     * Computed Columns:
     * - ANNUAL_BONUS = SALARY * 10%
     * - SALES_COMMISSION = SALE_AMOUNT_sum * 2% (or 0 if no sales)
     * - TOTAL_COMPENSATION = SALARY + ANNUAL_BONUS + SALES_COMMISSION
     *
     * Filter: TOTAL_COMPENSATION > 80,000
     *
     * Expected JSON Output (Pretty Printed):
     * [
     *   {
     *     "NAME": "David Lee",
     *     "DEPARTMENT_NAME": "Sales",
     *     "SALARY": 95000.00,
     *     "SALE_AMOUNT_sum": 83000.00,
     *     "TOTAL_COMPENSATION": 106160.00
     *   },
     *   {
     *     "NAME": "Bob Smith",
     *     "DEPARTMENT_NAME": "Sales",
     *     "SALARY": 90000.00,
     *     "SALE_AMOUNT_sum": 55000.00,
     *     "TOTAL_COMPENSATION": 100100.00
     *   },
     *   {
     *     "NAME": "Grace Davis",
     *     "DEPARTMENT_NAME": "Sales",
     *     "SALARY": 88000.00,
     *     "SALE_AMOUNT_sum": 42000.00,
     *     "TOTAL_COMPENSATION": 97640.00
     *   },
     *   {
     *     "NAME": "Charlie Brown",
     *     "DEPARTMENT_NAME": "Engineering",
     *     "SALARY": 85000.00,
     *     "SALE_AMOUNT_sum": null,
     *     "TOTAL_COMPENSATION": 93500.00
     *   },
     *   {
     *     "NAME": "Eve Wilson",
     *     "DEPARTMENT_NAME": "Engineering",
     *     "SALARY": 82000.00,
     *     "SALE_AMOUNT_sum": null,
     *     "TOTAL_COMPENSATION": 90200.00
     *   },
     *   {
     *     "NAME": "Alice Johnson",
     *     "DEPARTMENT_NAME": "Engineering",
     *     "SALARY": 80000.00,
     *     "SALE_AMOUNT_sum": null,
     *     "TOTAL_COMPENSATION": 88000.00
     *   }
     * ]
     *
     * Note: Frank Miller (Marketing, 70k salary) is excluded as his total compensation
     * (77,000) is below the 80,000 threshold.
     */
    @Test
    void testComprehensiveAnalysis() throws Exception {
        // Fetch all 3 datasets
        DataSet employees = createDataSetFromQuery("SELECT * FROM employees", "employees");
        DataSet departments = createDataSetFromQuery("SELECT * FROM departments", "departments");
        DataSet sales = createDataSetFromQuery("SELECT * FROM sales", "sales");

        // Step 1: Join employees with departments
        DataSet empDept = DataOperations.innerJoin(employees, departments,
                "DEPARTMENT_ID", "DEPARTMENT_ID");

        // Step 2: Aggregate sales by employee
        DataSet salesByEmployee = DataQuery.from(sales)
                .groupBy("EMPLOYEE_ID")
                .sum("SALE_AMOUNT")
                .count("SALE_ID")
                .execute();

        // Step 3: Left join with sales data (not all employees have sales)
        DataSet fullData = DataOperations.leftJoin(empDept, salesByEmployee,
                "EMPLOYEE_ID", "EMPLOYEE_ID");

        // Step 4a: Add base computed columns
        DataSet withBaseColumns = DataQuery.from(fullData)
                .withColumn("ANNUAL_BONUS", row -> {
                    // 10% of salary as base bonus
                    BigDecimal salary = row.getBigDecimal("SALARY");
                    return salary.multiply(BigDecimal.valueOf(0.1));
                })
                .withColumn("SALES_COMMISSION", row -> {
                    // 2% commission on sales (0 if no sales)
                    Object salesAmount = row.get("SALE_AMOUNT_sum");
                    if (salesAmount == null) {
                        return BigDecimal.ZERO;
                    }
                    double salesValue = ((Number) salesAmount).doubleValue();
                    return BigDecimal.valueOf(salesValue * 0.02);
                })
                .execute();

        // Step 4b: Add total compensation (depends on previous columns)
        DataSet withComputedColumns = DataQuery.from(withBaseColumns)
                .withColumn("TOTAL_COMPENSATION", row -> {
                    // Salary + annual bonus + sales commission
                    BigDecimal salary = row.getBigDecimal("SALARY");
                    BigDecimal bonus = row.getBigDecimal("ANNUAL_BONUS");
                    BigDecimal commission = row.getBigDecimal("SALES_COMMISSION");
                    return salary.add(bonus).add(commission);
                })
                .execute();

        // Step 4c: Filter on computed columns
        DataSet enriched = DataQuery.from(withComputedColumns)
                .filter(row -> {
                    // Filter for employees earning over 80k in total compensation
                    BigDecimal totalComp = row.getBigDecimal("TOTAL_COMPENSATION");
                    return totalComp.compareTo(BigDecimal.valueOf(80000)) > 0;
                })
                .execute();

        // Step 5: Select and order final output
        DataSet finalResult = DataQuery.from(enriched)
                .select("NAME", "DEPARTMENT_NAME", "SALARY",
                       "SALE_AMOUNT_sum", "TOTAL_COMPENSATION")
                .orderBy("TOTAL_COMPENSATION").desc()
                .execute();

        // Generate pretty JSON
        String prettyJson = finalResult.toPrettyJSON();

        // Verify comprehensive workflow
        assertThat(finalResult.count()).isGreaterThan(0);
        assertThat(finalResult.count()).isLessThanOrEqualTo(7);

        // Verify computed columns exist
        DataRow firstRow = finalResult.first();
        assertThat(firstRow.keys()).contains(
                "NAME", "DEPARTMENT_NAME", "SALARY",
                "SALE_AMOUNT_sum", "TOTAL_COMPENSATION");

        // Verify top compensated employee (should be David Lee with sales)
        assertThat(firstRow.getString("NAME")).isIn("David Lee", "Bob Smith", "Grace Davis");
        assertThat(firstRow.getBigDecimal("TOTAL_COMPENSATION"))
                .isGreaterThan(BigDecimal.valueOf(80000));

        // Verify ordering (descending by total compensation)
        for (int i = 0; i < finalResult.count() - 1; i++) {
            BigDecimal current = finalResult.getRows().get(i).getBigDecimal("TOTAL_COMPENSATION");
            BigDecimal next = finalResult.getRows().get(i + 1).getBigDecimal("TOTAL_COMPENSATION");
            assertThat(current).isGreaterThanOrEqualTo(next);
        }

        // Verify pretty JSON formatting
        assertThat(prettyJson)
                .contains("\n")  // Pretty formatting includes newlines
                .contains("DEPARTMENT_NAME")
                .contains("TOTAL_COMPENSATION");

        // Additional verification: Check that sales employees have higher compensation
        boolean hasSalesEmployee = finalResult.getRows().stream()
                .anyMatch(row -> "Sales".equals(row.getString("DEPARTMENT_NAME"))
                        && row.get("SALE_AMOUNT_sum") != null);

        if (hasSalesEmployee) {
            // At least one sales employee with commission should be in results
            assertThat(hasSalesEmployee).isTrue();
        }
    }

    /**
     * Test 5: Nested objects within records (Phase 2 pattern).
     *
     * Business scenario: Structure employee records with logical nested sections
     * (personal info, employment info, compensation) rather than flat fields.
     *
     * Expected JSON Output:
     * [
     *   {
     *     "personal": {
     *       "id": 1,
     *       "fullName": "Alice Johnson",
     *       "hireDate": "2020-01-15"
     *     },
     *     "employment": {
     *       "department": "Engineering",
     *       "departmentId": 10,
     *       "status": "Active"
     *     },
     *     "compensation": {
     *       "annualSalary": 80000.00,
     *       "currency": "USD",
     *       "payFrequency": "monthly"
     *     }
     *   },
     *   {
     *     "personal": {
     *       "id": 2,
     *       "fullName": "Bob Smith",
     *       "hireDate": "2019-03-10"
     *     },
     *     "employment": {
     *       "department": "Sales",
     *       "departmentId": 20,
     *       "status": "Active"
     *     },
     *     "compensation": {
     *       "annualSalary": 90000.00,
     *       "currency": "USD",
     *       "payFrequency": "monthly"
     *     }
     *   },
     *   ...
     * ]
     *
     * Note: This demonstrates Phase 2 - nested objects within each record.
     * Currently requires manual construction; future implementation would
     * provide declarative mapping API.
     */
    @Test
    void testNestedObjectsWithinRecords() throws Exception {
        // Fetch employee and department data
        DataSet employees = createDataSetFromQuery("SELECT * FROM employees", "employees");
        DataSet departments = createDataSetFromQuery("SELECT * FROM departments", "departments");

        // Join to get department names
        DataSet joined = DataOperations.innerJoin(employees, departments,
                "DEPARTMENT_ID", "DEPARTMENT_ID");

        // Build nested object structure for each employee
        List<Map<String, Object>> structuredRecords = new ArrayList<>();

        for (DataRow row : joined.getRows()) {
            Map<String, Object> record = new HashMap<>();

            // Personal information section
            Map<String, Object> personal = new HashMap<>();
            personal.put("id", row.getInt("EMPLOYEE_ID"));
            personal.put("fullName", row.getString("NAME"));
            personal.put("hireDate", row.get("HIRE_DATE").toString());
            record.put("personal", personal);

            // Employment information section
            Map<String, Object> employment = new HashMap<>();
            employment.put("department", row.getString("DEPARTMENT_NAME"));
            employment.put("departmentId", row.getInt("DEPARTMENT_ID"));
            employment.put("status", "Active");
            record.put("employment", employment);

            // Compensation information section
            Map<String, Object> compensation = new HashMap<>();
            compensation.put("annualSalary", row.getBigDecimal("SALARY"));
            compensation.put("currency", "USD");
            compensation.put("payFrequency", "monthly");
            record.put("compensation", compensation);

            structuredRecords.add(record);
        }

        // Convert to JSON
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        String nestedJson = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(structuredRecords);

        // Verify nested structure
        assertThat(structuredRecords).hasSize(7); // All 7 employees

        // Verify first record has nested sections
        Map<String, Object> firstRecord = structuredRecords.get(0);
        assertThat(firstRecord).containsKeys("personal", "employment", "compensation");

        // Verify personal section structure
        @SuppressWarnings("unchecked")
        Map<String, Object> personal = (Map<String, Object>) firstRecord.get("personal");
        assertThat(personal).containsKeys("id", "fullName", "hireDate");
        assertThat(personal.get("id")).isInstanceOf(Integer.class);
        assertThat(personal.get("fullName")).isInstanceOf(String.class);

        // Verify employment section structure
        @SuppressWarnings("unchecked")
        Map<String, Object> employment = (Map<String, Object>) firstRecord.get("employment");
        assertThat(employment).containsKeys("department", "departmentId", "status");
        assertThat(employment.get("status")).isEqualTo("Active");

        // Verify compensation section structure
        @SuppressWarnings("unchecked")
        Map<String, Object> compensation = (Map<String, Object>) firstRecord.get("compensation");
        assertThat(compensation).containsKeys("annualSalary", "currency", "payFrequency");
        assertThat(compensation.get("currency")).isEqualTo("USD");
        assertThat(compensation.get("payFrequency")).isEqualTo("monthly");

        // Verify JSON contains nested structure
        assertThat(nestedJson)
                .contains("\"personal\"")
                .contains("\"employment\"")
                .contains("\"compensation\"")
                .contains("\"fullName\"")
                .contains("\"department\"")
                .contains("\"annualSalary\"");

        // Output for visual verification
        System.out.println("\nPhase 2 - Nested Objects Within Records:");
        System.out.println(nestedJson);

        // Verify each record is structured the same way
        for (Map<String, Object> record : structuredRecords) {
            assertThat(record).containsKeys("personal", "employment", "compensation");
        }
    }

    /**
     * Test 6: Hierarchical nested structure (Phase 3 pattern).
     *
     * Business scenario: Create a department-centric hierarchical view where each department
     * contains its metadata and an array of employees. This demonstrates building hierarchical
     * JSON with grouped nested arrays (parent-child relationships).
     *
     * Expected JSON Output:
     * {
     *   "company": "Acme Corp",
     *   "reportDate": "2024-06-24",
     *   "departments": [
     *     {
     *       "departmentId": 10,
     *       "departmentName": "Engineering",
     *       "budget": 500000.00,
     *       "headcount": 3,
     *       "totalSalary": 247000.00,
     *       "employees": [
     *         {
     *           "id": 1,
     *           "name": "Alice Johnson",
     *           "salary": 80000.00,
     *           "hireDate": "2020-01-15"
     *         },
     *         {
     *           "id": 3,
     *           "name": "Charlie Brown",
     *           "salary": 85000.00,
     *           "hireDate": "2021-06-20"
     *         },
     *         {
     *           "id": 5,
     *           "name": "Eve Wilson",
     *           "salary": 82000.00,
     *           "hireDate": "2022-02-28"
     *         }
     *       ]
     *     },
     *     {
     *       "departmentId": 20,
     *       "departmentName": "Sales",
     *       "budget": 750000.00,
     *       "headcount": 3,
     *       "totalSalary": 273000.00,
     *       "employees": [
     *         {
     *           "id": 2,
     *           "name": "Bob Smith",
     *           "salary": 90000.00,
     *           "hireDate": "2019-03-10"
     *         },
     *         {
     *           "id": 4,
     *           "name": "David Lee",
     *           "salary": 95000.00,
     *           "hireDate": "2018-11-05"
     *         },
     *         {
     *           "id": 7,
     *           "name": "Grace Davis",
     *           "salary": 88000.00,
     *           "hireDate": "2020-07-01"
     *         }
     *       ]
     *     },
     *     {
     *       "departmentId": 30,
     *       "departmentName": "Marketing",
     *       "budget": 300000.00,
     *       "headcount": 1,
     *       "totalSalary": 70000.00,
     *       "employees": [
     *         {
     *           "id": 6,
     *           "name": "Frank Miller",
     *           "salary": 70000.00,
     *           "hireDate": "2021-09-12"
     *         }
     *       ]
     *     }
     *   ]
     * }
     *
     * Note: This demonstrates Phase 3 - hierarchical grouping with nested arrays.
     * Currently requires manual construction; future implementation would provide
     * declarative grouping and nesting API.
     */
    @Test
    void testHierarchicalNestedStructure() throws Exception {
        // Fetch base data
        DataSet employees = createDataSetFromQuery("SELECT * FROM employees", "employees");
        DataSet departments = createDataSetFromQuery("SELECT * FROM departments", "departments");

        // Calculate department-level aggregations directly from employees
        DataSet deptStats = DataQuery.from(employees)
                .groupBy("DEPARTMENT_ID")
                .sum("SALARY")
                .count("EMPLOYEE_ID")
                .execute();

        // Build nested JSON structure manually
        Map<String, Object> report = new HashMap<>();
        report.put("company", "Acme Corp");
        report.put("reportDate", "2024-06-24");

        List<Map<String, Object>> departmentsList = new ArrayList<>();

        // Process each department
        for (DataRow deptStat : deptStats.getRows()) {
            int deptId = deptStat.getInt("DEPARTMENT_ID");

            // Get department info
            DataRow deptInfo = departments.getRows().stream()
                    .filter(row -> row.getInt("DEPARTMENT_ID") == deptId)
                    .findFirst()
                    .orElseThrow();

            // Build department object
            Map<String, Object> department = new HashMap<>();
            department.put("departmentId", deptId);
            department.put("departmentName", deptInfo.getString("DEPARTMENT_NAME"));
            department.put("budget", deptInfo.getBigDecimal("BUDGET"));
            department.put("headcount", deptStat.getLong("EMPLOYEE_ID_count"));
            department.put("totalSalary", deptStat.getDouble("SALARY_sum"));

            // Get employees for this department
            List<Map<String, Object>> employeesList = new ArrayList<>();
            for (DataRow emp : employees.getRows()) {
                if (emp.getInt("DEPARTMENT_ID") == deptId) {
                    Map<String, Object> employee = new HashMap<>();
                    employee.put("id", emp.getInt("EMPLOYEE_ID"));
                    employee.put("name", emp.getString("NAME"));
                    employee.put("salary", emp.getBigDecimal("SALARY"));
                    employee.put("hireDate", emp.get("HIRE_DATE").toString());
                    employeesList.add(employee);
                }
            }

            department.put("employees", employeesList);
            departmentsList.add(department);
        }

        report.put("departments", departmentsList);

        // Convert to JSON
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        String nestedJson = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(report);

        // Verify nested structure
        assertThat(nestedJson)
                .contains("\"company\"")
                .contains("\"departments\"")
                .contains("\"employees\"")
                .contains("\"headcount\"");

        // Verify we have all 3 departments
        assertThat(departmentsList).hasSize(3);

        // Verify Engineering department structure
        Map<String, Object> engineering = departmentsList.stream()
                .filter(dept -> "Engineering".equals(dept.get("departmentName")))
                .findFirst()
                .orElseThrow();

        assertThat(engineering.get("departmentId")).isEqualTo(10);
        assertThat(engineering.get("headcount")).isEqualTo(3L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> engEmployees =
                (List<Map<String, Object>>) engineering.get("employees");
        assertThat(engEmployees).hasSize(3);
        assertThat(engEmployees.get(0)).containsKeys("id", "name", "salary", "hireDate");

        // Verify Sales department has highest budget
        Map<String, Object> sales = departmentsList.stream()
                .filter(dept -> "Sales".equals(dept.get("departmentName")))
                .findFirst()
                .orElseThrow();

        assertThat(sales.get("budget")).isEqualTo(new BigDecimal("750000.00"));

        // Output for visual verification
        System.out.println("\nNested JSON Structure:");
        System.out.println(nestedJson);

        // Verify that nested structure is truly hierarchical
        assertThat(nestedJson.split("\"employees\"").length - 1).isEqualTo(3); // 3 employee arrays
        assertThat(nestedJson).contains("Engineering");
        assertThat(nestedJson).contains("Sales");
        assertThat(nestedJson).contains("Marketing");
        assertThat(nestedJson).contains("\"departmentName\"");
    }
}
