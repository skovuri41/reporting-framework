# Metadata-Driven Java Reporting Framework

A lightweight, metadata-driven framework for executing SQL Server stored procedures with dynamic data transformations. Features a fluent DSL for filtering, joining, aggregating, and pivoting data without requiring POJOs.

## 🌟 Features

- **Zero-Code Reports**: Add new reports via metadata only - no framework code changes
- **Fluent DSL**: Type-safe, chainable transformations (filter, select, join, groupBy, pivot)
- **Dynamic or Typed**: Use POJOs for type safety or dynamic DataSets for flexibility
- **Explicit Mapping**: Clear column-to-field mappings for self-documenting code
- **Type-Safe Access**: `DataRow` provides type-safe getters (`getString()`, `getInt()`, `getBigDecimal()`)
- **Data Operations**: Built-in joins, unions, pivots, aggregations, running totals
- **Metadata Caching**: In-memory caching for performance
- **Lightweight**: ~10MB JAR, no Spark dependencies

## 🚀 Quick Start

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
DataSource dataSource = ... // HikariCP or other
ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
ReportService reportService = new ReportService(provider);

// Execute and transform with fluent DSL
DataSet result = reportService.executeDynamic("employees",
    Map.of("DepartmentId", 10, "MinSalary", 80000));

DataSet processed = DataQuery.from(result)
    .filter(row -> row.getBigDecimal("salary").compareTo(BigDecimal.valueOf(100000)) < 0)
    .select("name", "salary", "hire_date")
    .orderBy("salary").desc()
    .limit(10)
    .execute();

// Access results with type-safe getters
for (DataRow row : processed.getRows()) {
    String name = row.getString("name");
    BigDecimal salary = row.getBigDecimal("salary");
    LocalDate hireDate = row.getLocalDate("hire_date");
    System.out.println(name + ": $" + salary);
}
```

## 📖 Core Components

### DataRow - Type-Safe Row Wrapper

Immutable wrapper over `Map<String, Object>` with type-safe accessors.

```java
DataRow row = DataRow.builder()
    .put("employee_id", 1)
    .put("name", "Alice")
    .put("salary", BigDecimal.valueOf(85000))
    .build();

// Type-safe access
int id = row.getInt("employee_id");              // No casting needed
String name = row.getString("name");
BigDecimal salary = row.getBigDecimal("salary");

// Null-safe with defaults
String dept = row.getString("department", "Unknown");

// Immutable transformations
DataRow withBonus = row.with("bonus", salary.multiply(BigDecimal.valueOf(0.1)));
DataRow subset = row.select("name", "salary");
```

### DataSet - Collection of Rows

Collection of `DataRow` objects with metadata.

```java
DataSet dataSet = reportService.executeDynamic("employees");

System.out.println("Report: " + dataSet.getReportName());
System.out.println("Rows: " + dataSet.count());

DataRow first = dataSet.first();
DataRow last = dataSet.last();
String json = dataSet.toJSON();
```

### DataQuery - Fluent DSL

Chainable transformations for readable data processing.

```java
DataSet result = DataQuery.from(dataSet)
    // Multiple filters
    .filter(row -> row.getInt("department_id") == 10)
    .filter(row -> row.getBigDecimal("salary").compareTo(BigDecimal.valueOf(80000)) >= 0)

    // Add computed columns
    .withColumn("bonus", row -> row.getBigDecimal("salary").multiply(BigDecimal.valueOf(0.1)))
    .withColumn("full_name", row -> row.getString("first_name") + " " + row.getString("last_name"))

    // Select specific columns
    .select("name", "salary", "bonus")

    // Sort and limit
    .orderBy("salary").desc()
    .limit(10)

    .execute();
```

**Aggregations:**

```java
DataSet summary = DataQuery.from(dataSet)
    .groupBy("department_id")
    .avg("salary")
    .count("employee_id")
    .sum("revenue")
    .min("hire_date")
    .max("salary")
    .execute();

// Access aggregated results
DataRow dept = summary.first();
Double avgSalary = dept.getDouble("salary_avg");
Long employeeCount = dept.getLong("employee_id_count");
```

### DataOperations - Complex Operations

Static utility methods for joins, unions, pivots.

**Joins:**

```java
DataSet employees = reportService.executeDynamic("employees");
DataSet departments = reportService.executeDynamic("departments");

// Inner join
DataSet joined = DataOperations.innerJoin(
    employees, departments, "department_id", "department_id");

// Left join
DataSet leftJoined = DataOperations.leftJoin(
    employees, departments, "department_id", "department_id");

// Other join types: rightJoin, fullOuterJoin, crossJoin
```

**Unions:**

```java
DataSet combined = DataOperations.union(dataset1, dataset2);
DataSet distinct = DataOperations.unionDistinct(dataset1, dataset2);
```

**Pivot:**

```java
// Pivot sales data from rows to columns
List<DataRow> pivoted = DataOperations.pivot(
    salesData,
    "product",      // Grouping column (rows)
    "month",        // Pivot column (becomes column names)
    "amount"        // Value column (cell values)
);

// Result: product | Jan | Feb | Mar
//         Widget  | 100 | 150 | 200
//         Gadget  | 250 | 300 | 350
```

**Running Totals:**

```java
DataSet withRunningTotal = DataOperations.withRunningTotal(
    salesData, "revenue", "running_total");
```

## 💡 Usage Patterns

### Complex Workflow Example

```java
// Fetch data
DataSet employees = reportService.executeDynamic("employees");
DataSet departments = reportService.executeDynamic("departments");

// Transform: Filter → Join → Select → Sort
DataSet highEarners = DataQuery.from(employees)
    .filter(row -> row.getBigDecimal("salary").compareTo(BigDecimal.valueOf(85000)) >= 0)
    .execute();

DataSet enriched = DataOperations.innerJoin(
    highEarners, departments, "department_id", "department_id");

DataSet result = DataQuery.from(enriched)
    .select("name", "salary", "department_name")
    .orderBy("salary").desc()
    .limit(20)
    .execute();

// Export to JSON
String json = result.toPrettyJSON();
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
    .execute();

// Print summary
for (DataRow dept : summary.getRows()) {
    System.out.printf("Dept %d: %d employees, Avg Salary: $%.2f%n",
        dept.getInt("department_id"),
        dept.getLong("employee_id_count"),
        dept.getDouble("salary_avg"));
}
```

### Using POJOs (Type-Safe Alternative)

For stable schemas, you can still use POJOs for compile-time type safety:

```java
// Create POJO
public class EmployeeReport {
    private Integer employeeId;
    private String name;
    private BigDecimal salary;
    private LocalDate hireDate;
    // getters/setters
}

// Metadata with explicit mappings
{
  "resultClass": "com.example.EmployeeReport",
  "resultSetMapping": {
    "strategy": "EXPLICIT",
    "columnMappings": [
      {"column": "employee_id", "field": "employeeId", "required": true},
      {"column": "name", "field": "name", "required": true},
      {"column": "salary", "field": "salary", "required": true},
      {"column": "hire_date", "field": "hireDate", "required": true}
    ]
  }
}

// Execute with type safety
ReportResult<EmployeeReport> result = reportService.execute("employee_report", params);
List<EmployeeReport> employees = result.getResults();
```

## 🔧 Configuration

### Metadata JSON Structure

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

For POJO mapping, add explicit column mappings:

```json
{
  "resultClass": "com.example.EmployeeReport",
  "resultSetMapping": {
    "strategy": "EXPLICIT",
    "columnMappings": [
      {"column": "employee_id", "field": "employeeId", "required": true},
      {"column": "first_name", "field": "firstName", "required": true}
    ],
    "unmappedColumnsStrategy": "IGNORE"
  }
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

```
Client → ReportService
            ↓
         MetadataLoader (cached) → Database
            ↓
         StoredProcedureExecutor → CallableStatement
            ↓
         ResultSetToMapConverter → List<Map<String, Object>>
            ↓
         DataRow.of() → List<DataRow>
            ↓
         DataSet
```

**For POJO mapping:**
```
List<Map<String, Object>> → JacksonPojoMapper → List<POJO>
```

## 🎯 Design Principles

- **Open/Closed**: Add reports without modifying framework code
- **Separation of Concerns**: DataSet (storage), DataQuery (transformations), DataOperations (complex ops)
- **Immutability**: All data structures are immutable
- **Fluent API**: Readable, chainable method calls
- **Type Safety**: DataRow provides type-safe accessors

## 📦 Maven Dependency

```xml
<dependency>
    <groupId>com.reporting</groupId>
    <artifactId>reporting-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 🔍 Testing

Run tests:
```bash
mvn test
```

The framework includes:
- **Unit tests**: DataSetTest (21 tests), DataOperationsTest (11 tests)
- **Integration tests**: Full workflow tests with H2 database

## 📋 Requirements

- Java 17+
- SQL Server (or H2 for testing)
- Jackson 2.17+
- SLF4J for logging

## 📝 License

MIT License

## 🤝 Contributing

Contributions welcome! Submit pull requests or open issues.
