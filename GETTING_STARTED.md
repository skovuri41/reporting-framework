# Getting Started Guide

Complete guide to building your first report with the Metadata-Driven Java Reporting Framework.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Database Setup](#database-setup)
3. [Java Setup](#java-setup)
4. [Your First Report](#your-first-report)
5. [Data Transformations](#data-transformations)
6. [Advanced Scenarios](#advanced-scenarios)

---

## Prerequisites

### Required
- Java 17 or higher
- SQL Server database (or H2 for testing)
- Maven 3.6+

### Recommended
- Connection pooling library (HikariCP)
- IDE with Java support (IntelliJ, Eclipse, VS Code)

---

## Database Setup

### Step 1: Create Metadata Stored Procedure

This stored procedure returns metadata for your business stored procedures.

```sql
CREATE PROCEDURE usp_GetProcedureMetadata
    @ProcedureId VARCHAR(100) = NULL,
    @MetadataJson NVARCHAR(MAX) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    -- If NULL, return all procedures (for cache warming)
    IF @ProcedureId IS NULL
    BEGIN
        SELECT @MetadataJson = (
            SELECT
                procedure_id as procedureId,
                procedure_name as procedureName,
                description,
                parameters,
                columns,
                output_parameters as outputParameters
            FROM STORED_PROCEDURE_METADATA
            FOR JSON PATH
        );
    END
    ELSE
    BEGIN
        SELECT @MetadataJson = (
            SELECT
                procedure_id as procedureId,
                procedure_name as procedureName,
                description,
                parameters,
                columns,
                output_parameters as outputParameters
            FROM STORED_PROCEDURE_METADATA
            WHERE procedure_id = @ProcedureId
            FOR JSON PATH, WITHOUT_ARRAY_WRAPPER
        );
    END
END;
```

### Step 2: Create Metadata Storage Table

```sql
CREATE TABLE STORED_PROCEDURE_METADATA (
    procedure_id VARCHAR(100) PRIMARY KEY,
    procedure_name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    parameters NVARCHAR(MAX), -- JSON array
    columns NVARCHAR(MAX),     -- JSON array
    output_parameters NVARCHAR(MAX), -- JSON array
    created_date DATETIME DEFAULT GETDATE(),
    modified_date DATETIME DEFAULT GETDATE()
);
```

### Step 3: Create Your Business Stored Procedure

```sql
CREATE PROCEDURE usp_GetEmployees
    @DepartmentId INT,
    @MinSalary DECIMAL(18,2) = NULL
AS
BEGIN
    SELECT
        employee_id,
        first_name,
        last_name,
        department_id,
        salary,
        hire_date
    FROM employees
    WHERE department_id = @DepartmentId
        AND (@MinSalary IS NULL OR salary >= @MinSalary);
END;
```

### Step 4: Register Procedure Metadata

```sql
INSERT INTO STORED_PROCEDURE_METADATA
(procedure_id, procedure_name, description, parameters, columns, output_parameters)
VALUES (
    'emp_001',
    'usp_GetEmployees',
    'Fetch employees by department and salary threshold',

    -- Parameters JSON
    '[
        {
            "name": "DepartmentId",
            "fieldName": "departmentId",
            "type": "INTEGER",
            "nullable": false
        },
        {
            "name": "MinSalary",
            "fieldName": "minSalary",
            "type": "DECIMAL",
            "nullable": true
        }
    ]',

    -- Columns JSON (maps SQL columns to camelCase field names)
    '[
        {"columnName": "EMPLOYEE_ID", "fieldName": "employeeId", "columnType": "INTEGER"},
        {"columnName": "FIRST_NAME", "fieldName": "firstName", "columnType": "VARCHAR"},
        {"columnName": "LAST_NAME", "fieldName": "lastName", "columnType": "VARCHAR"},
        {"columnName": "DEPARTMENT_ID", "fieldName": "departmentId", "columnType": "INTEGER"},
        {"columnName": "SALARY", "fieldName": "salary", "columnType": "DECIMAL"},
        {"columnName": "HIRE_DATE", "fieldName": "hireDate", "columnType": "DATE"}
    ]',

    -- Output parameters (if any)
    '[]'
);
```

---

## Java Setup

### Step 1: Add Maven Dependency

```xml
<dependency>
    <groupId>com.reporting</groupId>
    <artifactId>reporting-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Optional: HikariCP for connection pooling -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.0.1</version>
</dependency>
```

### Step 2: Configure Connection Provider

```java
import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.connection.DataSourceConnectionProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseConfig {

    public static ConnectionProvider createConnectionProvider() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlserver://localhost:1433;databaseName=YourDB");
        config.setUsername("your_username");
        config.setPassword("your_password");
        config.setMaximumPoolSize(10);

        HikariDataSource dataSource = new HikariDataSource(config);
        return new DataSourceConnectionProvider(dataSource);
    }
}
```

### Step 3: Initialize ReportService

```java
import com.reporting.framework.api.ReportService;

public class ReportingService {

    private final ReportService reportService;

    public ReportingService() {
        ConnectionProvider provider = DatabaseConfig.createConnectionProvider();

        // Enable metadata caching (recommended)
        this.reportService = new ReportService(provider, true);
    }

    public ReportService getReportService() {
        return reportService;
    }
}
```

---

## Your First Report

### Execute a Simple Report

```java
import com.reporting.framework.data.DataSet;
import com.reporting.framework.data.DataRow;
import java.util.Map;

public class EmployeeReportExample {

    public static void main(String[] args) {
        ReportingService service = new ReportingService();
        ReportService reportService = service.getReportService();

        // Execute stored procedure
        DataSet employees = reportService.execute("emp_001",
            Map.of("departmentId", 10, "minSalary", 50000));

        System.out.println("Found " + employees.count() + " employees");

        // Access data with type-safe methods
        for (DataRow employee : employees.getRows()) {
            Integer id = employee.getInt("employeeId");
            String firstName = employee.getString("firstName");
            String lastName = employee.getString("lastName");
            BigDecimal salary = employee.getBigDecimal("salary");

            System.out.printf("Employee: %s %s (ID: %d) - Salary: $%,.2f%n",
                firstName, lastName, id, salary);
        }
    }
}
```

### Export to JSON

```java
DataSet employees = reportService.execute("emp_001", params);

// Pretty-printed JSON
String json = employees.toPrettyJSON();
System.out.println(json);

// Compact JSON
String compactJson = employees.toJSON();

// Save to file
Files.writeString(Path.of("employees.json"), json);
```

**Output:**
```json
[
  {
    "employeeId": 1,
    "firstName": "Alice",
    "lastName": "Johnson",
    "departmentId": 10,
    "salary": 85000.00,
    "hireDate": "2020-01-15"
  }
]
```

---

## Data Transformations

### Filtering Data

```java
DataSet employees = reportService.execute("emp_001", params);

// Filter high earners
DataSet highEarners = DataQuery.from(employees)
    .filter(row -> row.getBigDecimal("salary")
        .compareTo(BigDecimal.valueOf(100000)) >= 0)
    .execute();

// Multiple filters
DataSet filtered = DataQuery.from(employees)
    .filter(row -> row.getInt("departmentId") == 10)
    .filter(row -> row.getLocalDate("hireDate").getYear() >= 2020)
    .execute();
```

### Selecting Columns

```java
// Select specific columns
DataSet namesSalaries = DataQuery.from(employees)
    .select("firstName", "lastName", "salary")
    .execute();

// Combine with filters
DataSet result = DataQuery.from(employees)
    .filter(row -> row.getBigDecimal("salary").compareTo(BigDecimal.valueOf(80000)) >= 0)
    .select("firstName", "lastName", "salary", "hireDate")
    .orderBy("salary").desc()
    .execute();
```

### Sorting Data

```java
// Sort ascending
DataSet sorted = DataQuery.from(employees)
    .orderBy("lastName").asc()
    .execute();

// Sort descending
DataSet sorted = DataQuery.from(employees)
    .orderBy("salary").desc()
    .execute();

// Top N results
DataSet top10 = DataQuery.from(employees)
    .orderBy("salary").desc()
    .limit(10)
    .execute();
```

### Adding Computed Columns

```java
DataSet enriched = DataQuery.from(employees)
    .withColumn("fullName", row ->
        row.getString("firstName") + " " + row.getString("lastName"))

    .withColumn("bonus", row ->
        row.getBigDecimal("salary").multiply(BigDecimal.valueOf(0.1)))

    .withColumn("yearsEmployed", row -> {
        LocalDate hired = row.getLocalDate("hireDate");
        return Period.between(hired, LocalDate.now()).getYears();
    })

    .execute();

// Access computed columns
DataRow emp = enriched.first();
String fullName = emp.getString("fullName");
BigDecimal bonus = emp.getBigDecimal("bonus");
Integer years = emp.getInt("yearsEmployed");
```

### Aggregations

```java
// Group by department with aggregations
DataSet summary = DataQuery.from(employees)
    .groupBy("departmentId")
        .avg("salary")
        .count("employeeId")
        .max("salary")
        .min("salary")
        .sum("salary")
    .execute();

// Access aggregated results
for (DataRow dept : summary.getRows()) {
    Integer deptId = dept.getInt("departmentId");
    Long empCount = dept.getLong("employeeId_count");
    Double avgSalary = dept.getDouble("salary_avg");
    Double maxSalary = dept.getDouble("salary_max");

    System.out.printf("Dept %d: %d employees, Avg: $%,.2f, Max: $%,.2f%n",
        deptId, empCount, avgSalary, maxSalary);
}
```

---

## Advanced Scenarios

### Scenario 1: Multi-Table Analysis

```java
// Execute multiple stored procedures
DataSet employees = reportService.execute("emp_001",
    Map.of("departmentId", 10));

DataSet departments = reportService.execute("dept_001",
    Map.of());

DataSet sales = reportService.execute("sales_001",
    Map.of("startDate", LocalDate.of(2024, 1, 1)));

// Join employees with departments
DataSet empWithDept = DataOperations.innerJoin(
    employees, departments,
    "departmentId", "departmentId"
);

// Aggregate sales by employee
DataSet salesByEmp = DataQuery.from(sales)
    .groupBy("employeeId")
        .sum("saleAmount")
        .count("saleId")
    .execute();

// Join with sales data (left join - not all employees have sales)
DataSet fullData = DataOperations.leftJoin(
    empWithDept, salesByEmp,
    "employeeId", "employeeId"
);

// Final transformations
DataSet analysis = DataQuery.from(fullData)
    .withColumn("fullName", row ->
        row.getString("firstName") + " " + row.getString("lastName"))

    .withColumn("totalSales", row -> {
        BigDecimal sum = row.getBigDecimal("saleAmount_sum");
        return sum != null ? sum : BigDecimal.ZERO;
    })

    .select("fullName", "departmentName", "salary", "totalSales")
    .orderBy("totalSales").desc()
    .execute();

String json = analysis.toPrettyJSON();
```

### Scenario 2: ETL Pipeline

```java
public class SalesETLPipeline {

    public void extractTransformLoad() {
        ReportService reportService = new ReportingService().getReportService();

        // EXTRACT: Get data from stored procedures
        DataSet rawSales = reportService.execute("daily_sales",
            Map.of("date", LocalDate.now().minusDays(1)));

        DataSet products = reportService.execute("products", Map.of());
        DataSet regions = reportService.execute("regions", Map.of());

        // TRANSFORM: Join and enrich
        DataSet salesWithProducts = DataOperations.innerJoin(
            rawSales, products,
            "productId", "productId"
        );

        DataSet enriched = DataOperations.innerJoin(
            salesWithProducts, regions,
            "regionId", "regionId"
        );

        // TRANSFORM: Add business logic
        DataSet transformed = DataQuery.from(enriched)
            .withColumn("revenue", row ->
                row.getBigDecimal("quantity")
                    .multiply(row.getBigDecimal("unitPrice")))

            .withColumn("commission", row ->
                row.getBigDecimal("revenue")
                    .multiply(BigDecimal.valueOf(0.05)))

            .select("transactionId", "productName", "regionName",
                    "quantity", "revenue", "commission")
            .execute();

        // LOAD: Export to JSON for downstream systems
        String json = transformed.toJSON();

        // Send to API, save to file, load to warehouse, etc.
        sendToDataWarehouse(json);
        publishToAPI(json);
        saveToFile(json, "daily_sales_summary.json");
    }
}
```

### Scenario 3: Pivot Table

```java
// Monthly sales by product
DataSet monthlySales = reportService.execute("sales_by_month", params);

List<DataRow> pivoted = DataOperations.pivot(
    monthlySales,
    "productName",      // Row grouping
    "month",            // Becomes columns
    "saleAmount"        // Cell values
);

// Result format:
// productName | Jan     | Feb     | Mar     | Apr
// Widget      | 10000   | 15000   | 20000   | 25000
// Gadget      | 30000   | 35000   | 40000   | 45000
```

### Scenario 4: Running Totals

```java
DataSet dailySales = reportService.execute("daily_sales", params);

DataSet withRunningTotal = DataOperations.withRunningTotal(
    dailySales,
    "revenue",          // Column to accumulate
    "runningTotal"      // New column name
);

// Each row now has cumulative total
for (DataRow row : withRunningTotal.getRows()) {
    LocalDate date = row.getLocalDate("date");
    BigDecimal revenue = row.getBigDecimal("revenue");
    BigDecimal running = row.getBigDecimal("runningTotal");

    System.out.printf("%s: $%,.2f (Running: $%,.2f)%n",
        date, revenue, running);
}
```

---

## Parameter Filtering

The framework automatically filters parameters based on metadata:

```java
// You can pass a superset of parameters
Map<String, Object> allParams = Map.of(
    "departmentId", 10,
    "minSalary", 50000,
    "maxSalary", 150000,  // Not defined in metadata - filtered out
    "region", "US",        // Not defined in metadata - filtered out
    "unused", "value"      // Not defined in metadata - filtered out
);

// Framework only passes parameters defined in metadata
DataSet result = reportService.execute("emp_001", allParams);
// Only departmentId and minSalary are passed to stored procedure
```

---

## Error Handling

```java
import com.reporting.framework.exception.*;

try {
    DataSet result = reportService.execute("emp_001", params);

} catch (MetadataNotFoundException e) {
    // Procedure metadata not found
    System.err.println("Procedure not configured: " + e.getMessage());

} catch (MissingParameterException e) {
    // Required parameter missing
    System.err.println("Missing parameter: " + e.getParameterName());

} catch (MetadataParseException e) {
    // Invalid metadata JSON
    System.err.println("Metadata configuration error: " + e.getMessage());

} catch (ReportExecutionException e) {
    // Stored procedure execution failed
    System.err.println("Execution failed: " + e.getMessage());
}
```

---

## Metadata Cache Management

```java
// Cache is warmed automatically on startup (loads all procedures)

// Invalidate specific procedure cache (after metadata update)
reportService.invalidateMetadataCache("emp_001");

// Clear entire cache (force reload)
reportService.clearMetadataCache();

// Check cache size
int cachedProcedures = reportService.getMetadataCacheSize();
System.out.println("Cached procedures: " + cachedProcedures);
```

---

## Next Steps

- **[API Reference](API_REFERENCE.md)** - Complete API documentation
- **[Architecture Guide](ARCHITECTURE.md)** - Understanding how it works
- **Examples** - See `/src/test/java/com/reporting/framework/integration/` for working examples

---

## Common Patterns Cheat Sheet

```java
// Execute
DataSet data = reportService.execute("procId", Map.of("key", value));

// Filter
DataQuery.from(data).filter(row -> condition).execute();

// Select columns
DataQuery.from(data).select("col1", "col2").execute();

// Sort
DataQuery.from(data).orderBy("col").desc().execute();

// Limit
DataQuery.from(data).limit(10).execute();

// Computed column
DataQuery.from(data).withColumn("new", row -> calculation).execute();

// Aggregate
DataQuery.from(data).groupBy("col").sum("amount").count("id").execute();

// Join
DataOperations.innerJoin(data1, data2, "key1", "key2");

// Export JSON
String json = data.toJSON();
```

---

**Ready to build your first report? Start with [Your First Report](#your-first-report) above!**
