# Metadata-Driven Java Reporting Framework

A lightweight, metadata-driven framework for executing SQL Server stored procedures with two execution modes: **POJO-based** (type-safe) and **Dynamic** (schema-free with fluent transformations).

## 🎯 Two Execution Modes

### 1. **POJO Mode** - Compile-Time Type Safety
For stable schemas where you want compile-time type checking.

```java
ReportResult<EmployeeReport> result = reportService.execute("employee_report", params);
List<EmployeeReport> employees = result.getResults();
```

### 2. **Dynamic Mode** - Runtime Flexibility ⭐
For dynamic schemas with powerful transformations (filter, join, aggregate, pivot).

```java
DataSet result = reportService.executeDynamic("employees", params);
DataSet processed = DataQuery.from(result)
    .filter(row -> row.getInt("dept_id") > 10)
    .orderBy("salary").desc()
    .execute();
```

## 🌟 Features

- **Dual Mode**: POJO mode for type safety, Dynamic mode for flexibility
- **Type-Safe Access**: `DataRow` with `getString()`, `getInt()`, `getBigDecimal()` - no casting
- **Fluent DSL**: Chainable transformations via `DataQuery` builder
- **Data Operations**: Joins, unions, pivots, aggregations, running totals via `DataOperations`
- **Zero-Code Reports**: Add reports via metadata only
- **Explicit Mapping**: Self-documenting column-to-field mappings
- **Metadata Caching**: In-memory caching for performance
- **Lightweight**: ~10MB JAR, no Spark dependencies

## 🚀 Quick Start - Dynamic Mode

### 1. Database Setup

```sql
CREATE TABLE REPORT_METADATA (
    REPORT_NAME VARCHAR(100) PRIMARY KEY,
    STORED_PROCEDURE VARCHAR(200) NOT NULL,
    RESULT_CLASS VARCHAR(500) NOT NULL,
    METADATA_JSON NVARCHAR(MAX) NOT NULL
);
```

### 2. Create Stored Procedure

```sql
CREATE PROCEDURE dbo.usp_GetEmployees
    @DepartmentId INT,
    @MinSalary DECIMAL(18,2) = NULL
AS
BEGIN
    SELECT employee_id, name, department_id, salary, hire_date
    FROM employees
    WHERE department_id = @DepartmentId
        AND (@MinSalary IS NULL OR salary >= @MinSalary);
END;
```

### 3. Insert Metadata

```sql
INSERT INTO REPORT_METADATA (REPORT_NAME, STORED_PROCEDURE, RESULT_CLASS, METADATA_JSON)
VALUES ('employees', 'dbo.usp_GetEmployees', 'java.util.Map',
'{
  "reportName": "employees",
  "storedProcedure": "dbo.usp_GetEmployees",
  "resultClass": "java.util.Map",
  "inputParameters": [
    {"name": "DepartmentId", "sqlType": "INTEGER", "javaType": "java.lang.Integer", "required": true},
    {"name": "MinSalary", "sqlType": "DECIMAL", "javaType": "java.math.BigDecimal", "required": false}
  ]
}');
```

### 4. Execute with Transformations

```java
// Setup
DataSource dataSource = ... // HikariCP, etc.
ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
ReportService reportService = new ReportService(provider);

// Execute dynamically
DataSet employees = reportService.executeDynamic("employees",
    Map.of("DepartmentId", 10));

// Transform with fluent DSL
DataSet highEarners = DataQuery.from(employees)
    .filter(row -> row.getBigDecimal("salary").compareTo(BigDecimal.valueOf(85000)) >= 0)
    .select("name", "salary", "hire_date")
    .orderBy("salary").desc()
    .limit(10)
    .execute();

// Access with type-safe getters
for (DataRow row : highEarners.getRows()) {
    String name = row.getString("name");
    BigDecimal salary = row.getBigDecimal("salary");
    LocalDate hireDate = row.getLocalDate("hire_date");
    System.out.printf("%s: $%,.2f (hired %s)%n", name, salary, hireDate);
}
```

## 📖 Core Components (Dynamic Mode)

### DataRow - Type-Safe Row Wrapper

Immutable wrapper over `Map<String, Object>` with type-safe accessors.

```java
DataRow row = DataRow.builder()
    .put("employee_id", 1)
    .put("name", "Alice")
    .put("salary", BigDecimal.valueOf(85000))
    .put("hire_date", LocalDate.of(2020, 1, 15))
    .build();

// Type-safe access - no casting!
Integer id = row.getInt("employee_id");
String name = row.getString("name");
BigDecimal salary = row.getBigDecimal("salary");
LocalDate hireDate = row.getLocalDate("hire_date");

// Null-safe with defaults
String dept = row.getString("department", "Unknown");
Integer bonus = row.getInt("bonus", 0);

// Check if key exists
if (row.has("manager_id")) {
    Integer managerId = row.getInt("manager_id");
}

// Get all column names
Set<String> columns = row.keys();

// Immutable transformations
DataRow withBonus = row.with("bonus", salary.multiply(BigDecimal.valueOf(0.1)));
DataRow nameOnly = row.select("name", "salary");
DataRow withoutId = row.without("employee_id");
```

**Available Type-Safe Getters:**
- `getString(key)` / `getString(key, default)`
- `getInt(key)` / `getInt(key, default)`
- `getLong(key)` / `getLong(key, default)`
- `getDouble(key)` / `getDouble(key, default)`
- `getBigDecimal(key)` / `getBigDecimal(key, default)`
- `getBoolean(key)` / `getBoolean(key, default)`
- `getLocalDate(key)`, `getLocalDateTime(key)`, `getLocalTime(key)`

### DataSet - Immutable Collection

Collection of `DataRow` objects with metadata.

```java
DataSet dataSet = reportService.executeDynamic("employees");

// Basic info
String reportName = dataSet.getReportName();
int rowCount = dataSet.count();
boolean empty = dataSet.isEmpty();

// Access rows
DataRow first = dataSet.first();
DataRow last = dataSet.last();
DataRow specific = dataSet.getRow(5);
List<DataRow> allRows = dataSet.getRows();

// Output parameters
Object totalCount = dataSet.getOutputParameter("total_count");
boolean hasParam = dataSet.hasOutputParameter("total_count");

// Export
String json = dataSet.toJSON();
String prettyJson = dataSet.toPrettyJSON();
List<Map<String, Object>> maps = dataSet.toMaps();
```

### DataQuery - Fluent DSL Builder

Chainable transformations builder (immutable - returns new DataSet).

**Basic Operations:**

```java
DataSet result = DataQuery.from(dataSet)
    // Filter rows
    .filter(row -> row.getInt("department_id") == 10)
    .filter(row -> row.getBigDecimal("salary").compareTo(BigDecimal.valueOf(80000)) >= 0)

    // Select columns
    .select("name", "salary", "department_id")

    // Sort
    .orderBy("salary").desc()      // descending
    .orderBy("name").asc()          // ascending

    // Limit results
    .limit(10)

    // Remove duplicates
    .distinct()

    .execute();
```

**Computed Columns:**

```java
DataSet enriched = DataQuery.from(dataSet)
    .withColumn("bonus", row ->
        row.getBigDecimal("salary").multiply(BigDecimal.valueOf(0.1)))
    .withColumn("full_name", row ->
        row.getString("first_name") + " " + row.getString("last_name"))
    .withColumn("years_service", row ->
        java.time.Period.between(row.getLocalDate("hire_date"), LocalDate.now()).getYears())
    .execute();
```

**Aggregations:**

```java
DataSet summary = DataQuery.from(employees)
    .groupBy("department_id")
        .avg("salary")
        .count("employee_id")
        .sum("revenue")
        .min("hire_date")
        .max("salary")
    .execute();

// Access aggregated data
for (DataRow dept : summary.getRows()) {
    Integer deptId = dept.getInt("department_id");
    Double avgSalary = dept.getDouble("salary_avg");
    Long employeeCount = dept.getLong("employee_id_count");
    Double totalRevenue = dept.getDouble("revenue_sum");

    System.out.printf("Dept %d: %d employees, Avg: $%.2f%n",
        deptId, employeeCount, avgSalary);
}
```

### DataOperations - Complex Operations

Static utility methods for joins, unions, pivots.

**Joins:**

```java
DataSet employees = reportService.executeDynamic("employees");
DataSet departments = reportService.executeDynamic("departments");

// Inner join
DataSet joined = DataOperations.innerJoin(
    employees, departments,
    "department_id", "department_id");

// Left join - all employees, matching departments
DataSet leftJoined = DataOperations.leftJoin(
    employees, departments,
    "department_id", "department_id");

// Right join - all departments, matching employees
DataSet rightJoined = DataOperations.rightJoin(
    employees, departments,
    "department_id", "department_id");

// Full outer join
DataSet fullJoined = DataOperations.fullOuterJoin(
    employees, departments,
    "department_id", "department_id");

// Cross join (Cartesian product)
DataSet crossed = DataOperations.crossJoin(set1, set2);
```

**Unions:**

```java
DataSet combined = DataOperations.union(dataset1, dataset2);
DataSet distinct = DataOperations.unionDistinct(dataset1, dataset2);
```

**Pivot:**

```java
// Transform sales data from rows to columns
DataSet salesData = reportService.executeDynamic("monthly_sales");

List<DataRow> pivoted = DataOperations.pivot(
    salesData,
    "product",      // Grouping column (rows in output)
    "month",        // Pivot column (becomes column names)
    "amount"        // Value column (cell values)
);

// Result format:
// product | Jan | Feb | Mar | Apr
// Widget  | 100 | 150 | 200 | 250
// Gadget  | 300 | 350 | 400 | 450
```

**Running Totals:**

```java
DataSet withRunningTotal = DataOperations.withRunningTotal(
    salesData,
    "revenue",          // Column to accumulate
    "running_total"     // New column name
);
```

## 💡 Usage Patterns

### Complex Workflow

```java
// Fetch data from two reports
DataSet employees = reportService.executeDynamic("employees");
DataSet departments = reportService.executeDynamic("departments");

// Step 1: Filter high earners
DataSet highEarners = DataQuery.from(employees)
    .filter(row -> row.getBigDecimal("salary").compareTo(BigDecimal.valueOf(85000)) >= 0)
    .execute();

// Step 2: Join with departments
DataSet enriched = DataOperations.innerJoin(
    highEarners, departments, "department_id", "department_id");

// Step 3: Transform and sort
DataSet result = DataQuery.from(enriched)
    .select("name", "salary", "department_name")
    .withColumn("bonus", row -> row.getBigDecimal("salary").multiply(BigDecimal.valueOf(0.1)))
    .orderBy("salary").desc()
    .limit(20)
    .execute();

// Export
String json = result.toPrettyJSON();
System.out.println(json);
```

### Department Summary Report

```java
DataSet employees = reportService.executeDynamic("employees");

// Group by department with multiple aggregations
DataSet summary = DataQuery.from(employees)
    .groupBy("department_id")
        .avg("salary")
        .count("employee_id")
        .max("salary")
        .min("salary")
        .sum("revenue")
    .execute();

// Print formatted summary
for (DataRow dept : summary.getRows()) {
    System.out.printf(
        "Department %d:%n" +
        "  Employees: %d%n" +
        "  Avg Salary: $%,.2f%n" +
        "  Salary Range: $%,.2f - $%,.2f%n" +
        "  Total Revenue: $%,.2f%n%n",
        dept.getInt("department_id"),
        dept.getLong("employee_id_count"),
        dept.getDouble("salary_avg"),
        dept.getDouble("salary_min"),
        dept.getDouble("salary_max"),
        dept.getDouble("revenue_sum")
    );
}
```

## 📋 Configuration

### Metadata JSON Structure

**For Dynamic Mode (java.util.Map):**

```json
{
  "reportName": "report_name",
  "storedProcedure": "dbo.usp_ProcedureName",
  "resultClass": "java.util.Map",
  "inputParameters": [
    {
      "name": "ParameterName",
      "sqlType": "INTEGER|VARCHAR|DATE|DECIMAL",
      "javaType": "java.lang.Integer|java.lang.String|...",
      "required": true
    }
  ],
  "outputParameters": [
    {
      "name": "OutputParam",
      "sqlType": "INTEGER",
      "javaType": "java.lang.Integer"
    }
  ]
}
```


### Supported SQL Types

| SQL Type | Java Type |
|----------|-----------|
| INTEGER, INT | java.lang.Integer |
| BIGINT | java.lang.Long |
| DECIMAL, NUMERIC | java.math.BigDecimal |
| VARCHAR, NVARCHAR | java.lang.String |
| DATE | java.time.LocalDate |
| DATETIME, TIMESTAMP | java.time.LocalDateTime |
| TIME | java.time.LocalTime |
| BIT, BOOLEAN | java.lang.Boolean |
| FLOAT, REAL | java.lang.Float |
| DOUBLE | java.lang.Double |

### Metadata Caching

```java
// Clear entire cache
reportService.clearMetadataCache();

// Invalidate specific report
reportService.invalidateMetadataCache("employee_report");

// Check cache size
int size = reportService.getMetadataCacheSize();
```

## 🏗️ Architecture

### Dynamic Mode Flow

```
Client
  ↓
ReportService.executeDynamic(reportName, params)
  ↓
MetadataLoader → Database (fetch metadata)
  ↓
StoredProcedureExecutor → CallableStatement
  ↓
ResultSetToMapConverter → List<Map<String, Object>>
  ↓
DataRow.of() → List<DataRow>
  ↓
new DataSet(rows, outputParams, reportName)
  ↓
DataQuery.from(dataSet).filter(...).execute() → DataSet
  ↓
DataOperations.join(dataSet1, dataSet2) → DataSet
```

## 🎯 Design Principles

- **Open/Closed**: Add reports without modifying framework code
- **Dual Mode**: POJO for type safety, Dynamic for flexibility
- **Separation of Concerns**:
  - `DataSet` = immutable storage
  - `DataQuery` = transformation builder
  - `DataOperations` = complex operations
- **Immutability**: All transformations return new instances
- **Type Safety**: `DataRow` eliminates casting
- **Fluent API**: Readable, chainable method calls

## 🔍 Testing

Run all tests:
```bash
mvn test
```

The framework includes:
- **Unit Tests**:
  - `DataSetTest` (21 tests) - DataSet and DataQuery functionality
  - `DataOperations Test` (11 tests) - Joins, unions, pivots, aggregations
- **Integration Tests**: Full workflow tests with H2 in-memory database

## 📦 Maven Dependency

```xml
<dependency>
    <groupId>com.reporting</groupId>
    <artifactId>reporting-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 📋 Requirements

- **Java**: 17+
- **Database**: SQL Server (or H2 for testing)
- **Dependencies**:
  - Jackson 2.17+ (JSON mapping)
  - SLF4J (logging)
  - JDBC driver for your database

## 📖 Key Differences from Other Branches

### dataset-impl Branch (This Branch)

- ✅ Dual execution modes: `execute()` and `executeDynamic()`
- ✅ `DataRow` with type-safe getters
- ✅ `DataSet` as immutable collection
- ✅ `DataQuery` as fluent DSL builder
- ✅ `DataOperations` for complex operations
- ✅ 32 comprehensive tests

## 📝 License

MIT License

## 🤝 Contributing

Contributions welcome! Submit pull requests or open issues.
