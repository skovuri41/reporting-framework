package com.reporting.framework.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.reporting.framework.connection.SimpleConnectionProvider;
import com.reporting.framework.data.DataOperations;
import com.reporting.framework.data.DataQuery;
import com.reporting.framework.data.DataRow;
import com.reporting.framework.data.DataSet;
import com.reporting.framework.metadata.StoredProcedureMetadata;
import com.reporting.framework.test.TestMetadataLoader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Fully refactored integration test demonstrating:
 * - 4 stored procedures (employees, departments, sales, regions)
 * - camelCase field names throughout
 * - Multi-table joins (4-way join)
 * - Complex transformations (filter, aggregate, computed columns)
 * - Nested JSON generation (regions → departments → employees)
 *
 * This test uses TestMetadataLoader to bypass H2's stored procedure limitation.
 */
class FullWorkflowIntegrationTestRefactored {

    private static SimpleConnectionProvider connectionProvider;
    private static TestMetadataLoader metadataLoader;

    @BeforeAll
    static void setUp() throws Exception {
        // Create in-memory H2 database
        String jdbcUrl = "jdbc:h2:mem:refactored_" + System.currentTimeMillis() +
                ";DB_CLOSE_DELAY=-1;MODE=MSSQLServer";
        connectionProvider = new SimpleConnectionProvider(jdbcUrl, "sa", "");

        // Initialize test metadata loader
        metadataLoader = new TestMetadataLoader();

        // Create schema and insert test data
        createSchema();
        insertTestData();

        // Register procedure metadata
        registerProcedureMetadata();
    }

    private static void createSchema() throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE employees (
                    employee_id INT PRIMARY KEY,
                    first_name VARCHAR(50),
                    last_name VARCHAR(50),
                    department_id INT,
                    salary DECIMAL(10,2),
                    hire_date DATE,
                    region_id INT
                )
            """);

            stmt.execute("""
                CREATE TABLE departments (
                    department_id INT PRIMARY KEY,
                    department_name VARCHAR(100),
                    budget DECIMAL(12,2),
                    region_id INT
                )
            """);

            stmt.execute("""
                CREATE TABLE sales (
                    sale_id INT PRIMARY KEY,
                    employee_id INT,
                    sale_amount DECIMAL(10,2),
                    sale_date DATE,
                    product_category VARCHAR(50)
                )
            """);

            stmt.execute("""
                CREATE TABLE regions (
                    region_id INT PRIMARY KEY,
                    region_name VARCHAR(100),
                    region_code VARCHAR(10)
                )
            """);
        }
    }

    private static void insertTestData() throws SQLException {
        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement()) {

            // Insert regions
            stmt.execute("INSERT INTO regions VALUES (1, 'North America', 'NA')");
            stmt.execute("INSERT INTO regions VALUES (2, 'Europe', 'EU')");
            stmt.execute("INSERT INTO regions VALUES (3, 'Asia Pacific', 'APAC')");

            // Insert departments
            stmt.execute("INSERT INTO departments VALUES (10, 'Engineering', 500000, 1)");
            stmt.execute("INSERT INTO departments VALUES (20, 'Sales', 300000, 2)");
            stmt.execute("INSERT INTO departments VALUES (30, 'Marketing', 250000, 3)");

            // Insert employees
            stmt.execute("INSERT INTO employees VALUES (1, 'Alice', 'Johnson', 10, 95000, '2019-03-15', 1)");
            stmt.execute("INSERT INTO employees VALUES (2, 'Bob', 'Smith', 10, 88000, '2020-01-10', 1)");
            stmt.execute("INSERT INTO employees VALUES (3, 'Carol', 'Williams', 20, 92000, '2018-07-20', 2)");
            stmt.execute("INSERT INTO employees VALUES (4, 'David', 'Brown', 20, 78000, '2021-05-12', 2)");
            stmt.execute("INSERT INTO employees VALUES (5, 'Eve', 'Davis', 30, 85000, '2019-11-08', 3)");
            stmt.execute("INSERT INTO employees VALUES (6, 'Frank', 'Miller', 30, 72000, '2022-02-14', 3)");
            stmt.execute("INSERT INTO employees VALUES (7, 'Grace', 'Wilson', 10, 102000, '2017-09-01', 1)");
            stmt.execute("INSERT INTO employees VALUES (8, 'Henry', 'Moore', 20, 91000, '2020-08-22', 2)");
            stmt.execute("INSERT INTO employees VALUES (9, 'Iris', 'Taylor', 30, 67000, '2022-06-30', 3)");
            stmt.execute("INSERT INTO employees VALUES (10, 'Jack', 'Anderson', 10, 83000, '2021-03-17', 1)");

            // Insert sales data
            stmt.execute("INSERT INTO sales VALUES (1, 3, 15000, '2024-01-05', 'Software')");
            stmt.execute("INSERT INTO sales VALUES (2, 3, 22000, '2024-01-12', 'Hardware')");
            stmt.execute("INSERT INTO sales VALUES (3, 4, 8500, '2024-01-18', 'Software')");
            stmt.execute("INSERT INTO sales VALUES (4, 8, 31000, '2024-01-25', 'Services')");
            stmt.execute("INSERT INTO sales VALUES (5, 3, 19000, '2024-02-03', 'Software')");
            stmt.execute("INSERT INTO sales VALUES (6, 4, 12000, '2024-02-10', 'Hardware')");
            stmt.execute("INSERT INTO sales VALUES (7, 8, 27000, '2024-02-15', 'Services')");
            stmt.execute("INSERT INTO sales VALUES (8, 3, 16500, '2024-02-22', 'Software')");
            stmt.execute("INSERT INTO sales VALUES (9, 4, 9800, '2024-03-01', 'Hardware')");
            stmt.execute("INSERT INTO sales VALUES (10, 8, 24000, '2024-03-08', 'Services')");
            stmt.execute("INSERT INTO sales VALUES (11, 3, 21000, '2024-03-15', 'Software')");
            stmt.execute("INSERT INTO sales VALUES (12, 4, 14500, '2024-03-20', 'Hardware')");
            stmt.execute("INSERT INTO sales VALUES (13, 8, 29000, '2024-03-25', 'Services')");
            stmt.execute("INSERT INTO sales VALUES (14, 3, 18000, '2024-03-28', 'Software')");
            stmt.execute("INSERT INTO sales VALUES (15, 4, 11000, '2024-03-30', 'Hardware')");
        }
    }

    private static void registerProcedureMetadata() {
        // Employees metadata
        StoredProcedureMetadata empMetadata = StoredProcedureMetadata.builder()
                .procedureId("emp_001")
                .procedureName("usp_GetEmployees")
                .addParameter("departmentId", "departmentId", "INTEGER", true)
                .addParameter("minSalary", "minSalary", "DECIMAL", true)
                .addColumn("EMPLOYEE_ID", "employeeId", "INTEGER")
                .addColumn("FIRST_NAME", "firstName", "VARCHAR")
                .addColumn("LAST_NAME", "lastName", "VARCHAR")
                .addColumn("DEPARTMENT_ID", "departmentId", "INTEGER")
                .addColumn("SALARY", "salary", "DECIMAL")
                .addColumn("HIRE_DATE", "hireDate", "DATE")
                .addColumn("REGION_ID", "regionId", "INTEGER")
                .build();

        // Departments metadata
        StoredProcedureMetadata deptMetadata = StoredProcedureMetadata.builder()
                .procedureId("dept_001")
                .procedureName("usp_GetDepartments")
                .addColumn("DEPARTMENT_ID", "departmentId", "INTEGER")
                .addColumn("DEPARTMENT_NAME", "departmentName", "VARCHAR")
                .addColumn("BUDGET", "budget", "DECIMAL")
                .addColumn("REGION_ID", "regionId", "INTEGER")
                .build();

        // Sales metadata
        StoredProcedureMetadata salesMetadata = StoredProcedureMetadata.builder()
                .procedureId("sales_001")
                .procedureName("usp_GetSales")
                .addParameter("startDate", "startDate", "DATE", true)
                .addColumn("SALE_ID", "saleId", "INTEGER")
                .addColumn("EMPLOYEE_ID", "employeeId", "INTEGER")
                .addColumn("SALE_AMOUNT", "saleAmount", "DECIMAL")
                .addColumn("SALE_DATE", "saleDate", "DATE")
                .addColumn("PRODUCT_CATEGORY", "productCategory", "VARCHAR")
                .build();

        // Regions metadata
        StoredProcedureMetadata regionsMetadata = StoredProcedureMetadata.builder()
                .procedureId("region_001")
                .procedureName("usp_GetRegions")
                .addColumn("REGION_ID", "regionId", "INTEGER")
                .addColumn("REGION_NAME", "regionName", "VARCHAR")
                .addColumn("REGION_CODE", "regionCode", "VARCHAR")
                .build();

        metadataLoader.registerTestMetadata("emp_001", empMetadata);
        metadataLoader.registerTestMetadata("dept_001", deptMetadata);
        metadataLoader.registerTestMetadata("sales_001", salesMetadata);
        metadataLoader.registerTestMetadata("region_001", regionsMetadata);
    }

    /**
     * Helper to create DataSet from SQL with camelCase field names
     */
    private static DataSet createDataSetFromQuery(String sql, String procedureId) throws SQLException {
        StoredProcedureMetadata metadata = metadataLoader.getMetadata(procedureId);
        Map<String, String> columnMapping = metadata.getFieldNameMapping();

        List<DataRow> rows = new ArrayList<>();

        try (Connection conn = connectionProvider.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData rsMetadata = rs.getMetaData();
            int columnCount = rsMetadata.getColumnCount();

            while (rs.next()) {
                Map<String, Object> rowData = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String sqlColumnName = rsMetadata.getColumnName(i);
                    String fieldName = columnMapping.getOrDefault(sqlColumnName, sqlColumnName);
                    Object value = rs.getObject(i);
                    rowData.put(fieldName, value);
                }
                rows.add(DataRow.of(rowData));
            }
        }

        return new DataSet(rows, new HashMap<>(), procedureId);
    }

    @Test
    void testComplexMultiProcedureWorkflowWithNestedJson() throws Exception {
        // ============================================
        // STEP 1: Execute 4 stored procedures
        // ============================================

        DataSet employees = createDataSetFromQuery("SELECT * FROM employees", "emp_001");
        DataSet departments = createDataSetFromQuery("SELECT * FROM departments", "dept_001");
        DataSet sales = createDataSetFromQuery("SELECT * FROM sales WHERE sale_date >= '2024-01-01'", "sales_001");
        DataSet regions = createDataSetFromQuery("SELECT * FROM regions", "region_001");

        // Verify camelCase field names (not SQL column names)
        DataRow firstEmp = employees.first();
        assertThat(firstEmp.getInt("employeeId")).isEqualTo(1);
        assertThat(firstEmp.getString("firstName")).isEqualTo("Alice");
        assertThat(firstEmp.getString("lastName")).isEqualTo("Johnson");

        // ============================================
        // STEP 2: Aggregate sales by employee
        // ============================================

        DataSet salesByEmployee = DataQuery.from(sales)
                .groupBy("employeeId")
                .sum("saleAmount")
                .count("saleId")
                .execute();

        assertThat(salesByEmployee.count()).isGreaterThan(0);

        // ============================================
        // STEP 3: 4-way join
        // ============================================

        // Join employees with departments
        DataSet empWithDept = DataOperations.innerJoin(
                employees, departments,
                "departmentId", "departmentId"
        );

        // Join with regions
        DataSet empWithDeptAndRegion = DataOperations.innerJoin(
                empWithDept, regions,
                "regionId", "regionId"
        );

        // Join with aggregated sales (left join - not all employees have sales)
        DataSet fullData = DataOperations.leftJoin(
                empWithDeptAndRegion, salesByEmployee,
                "employeeId", "employeeId"
        );

        // ============================================
        // STEP 4: Filter and transform
        // ============================================

        DataSet result = DataQuery.from(fullData)
                // Filter: only high earners
                .filter(row -> row.getBigDecimal("salary")
                        .compareTo(BigDecimal.valueOf(85000)) >= 0)

                // Add computed columns
                .withColumn("fullName", row ->
                        row.getString("firstName") + " " + row.getString("lastName"))

                .withColumn("totalSales", row -> {
                    BigDecimal salesSum = row.getBigDecimal("saleAmount_sum");
                    return salesSum != null ? salesSum : BigDecimal.ZERO;
                })

                .withColumn("salesCount", row -> {
                    Long count = row.getLong("saleId_count");
                    return count != null ? count : 0L;
                })

                // Select relevant columns
                .select("employeeId", "fullName", "departmentName", "regionName",
                        "salary", "totalSales", "salesCount")

                // Sort by total sales descending
                .orderBy("totalSales").desc()

                .execute();

        // ============================================
        // STEP 5: Verify results
        // ============================================

        assertThat(result.isEmpty()).isFalse();
        assertThat(result.count()).isGreaterThan(0);

        // Verify top performer
        DataRow topPerformer = result.first();
        assertThat(topPerformer.getString("fullName")).contains("Carol");
        assertThat(topPerformer.getBigDecimal("totalSales"))
                .isGreaterThan(BigDecimal.valueOf(100000));

        // ============================================
        // STEP 6: Generate nested JSON by region
        // ============================================

        String nestedJson = generateNestedJsonByRegion(result);

        System.out.println("Nested JSON Output:");
        System.out.println(nestedJson);

        // Verify nested structure
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(nestedJson);

        assertThat(root.has("regions")).isTrue();
        assertThat(root.get("regions").isArray()).isTrue();

        JsonNode firstRegion = root.get("regions").get(0);
        assertThat(firstRegion.has("regionName")).isTrue();
        assertThat(firstRegion.has("departments")).isTrue();
        assertThat(firstRegion.get("departments").isArray()).isTrue();

        JsonNode firstDept = firstRegion.get("departments").get(0);
        assertThat(firstDept.has("departmentName")).isTrue();
        assertThat(firstDept.has("employees")).isTrue();
        assertThat(firstDept.get("employees").isArray()).isTrue();

        JsonNode firstEmployee = firstDept.get("employees").get(0);
        assertThat(firstEmployee.has("fullName")).isTrue();
        assertThat(firstEmployee.has("salary")).isTrue();
        assertThat(firstEmployee.has("totalSales")).isTrue();
    }

    /**
     * Generate nested JSON structure: Regions → Departments → Employees
     */
    private String generateNestedJsonByRegion(DataSet data) throws Exception {
        // Group by region
        Map<String, List<DataRow>> byRegion = new LinkedHashMap<>();
        for (DataRow row : data.getRows()) {
            String region = row.getString("regionName");
            byRegion.computeIfAbsent(region, k -> new ArrayList<>()).add(row);
        }

        // Build nested structure
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode regionsArray = mapper.createArrayNode();

        for (Map.Entry<String, List<DataRow>> regionEntry : byRegion.entrySet()) {
            ObjectNode regionNode = mapper.createObjectNode();
            regionNode.put("regionName", regionEntry.getKey());

            // Group employees by department within region
            Map<String, List<DataRow>> byDept = new LinkedHashMap<>();
            for (DataRow row : regionEntry.getValue()) {
                String dept = row.getString("departmentName");
                byDept.computeIfAbsent(dept, k -> new ArrayList<>()).add(row);
            }

            ArrayNode deptsArray = mapper.createArrayNode();
            for (Map.Entry<String, List<DataRow>> deptEntry : byDept.entrySet()) {
                ObjectNode deptNode = mapper.createObjectNode();
                deptNode.put("departmentName", deptEntry.getKey());

                // Add employees array
                ArrayNode employeesArray = mapper.createArrayNode();
                for (DataRow emp : deptEntry.getValue()) {
                    ObjectNode empNode = mapper.createObjectNode();
                    empNode.put("employeeId", emp.getInt("employeeId"));
                    empNode.put("fullName", emp.getString("fullName"));
                    empNode.put("salary", emp.getBigDecimal("salary").doubleValue());
                    empNode.put("totalSales", emp.getBigDecimal("totalSales").doubleValue());
                    empNode.put("salesCount", emp.getLong("salesCount"));
                    employeesArray.add(empNode);
                }

                deptNode.set("employees", employeesArray);
                deptsArray.add(deptNode);
            }

            regionNode.set("departments", deptsArray);
            regionsArray.add(regionNode);
        }

        root.set("regions", regionsArray);

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
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
}
