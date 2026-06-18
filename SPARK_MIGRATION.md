# Spark Migration Guide

This guide documents the migration from the POJO-based reporting framework to the Apache Spark-based implementation.

## Overview of Changes

The `spark-impl` branch represents a **complete replacement** of the POJO mapping approach with Apache Spark for dynamic data operations. This is not a backwards-compatible change—it's a new implementation paradigm.

### Key Differences

| Aspect | POJO-based (main branch) | Spark-based (spark-impl branch) |
|--------|-------------------------|----------------------------------|
| **Schema Definition** | Explicit column mappings in JSON | Auto-inferred from ResultSet |
| **Return Type** | `ReportResult<T>` (typed list of POJOs) | `SparkReportResult` (Dataset + output params) |
| **Data Transformation** | Manual Java code | Spark DataFrame operations |
| **Output Format** | Typed Java objects | JSON strings or Spark Dataset |
| **Flexibility** | Fixed schema per report | Dynamic transformations at runtime |
| **Dependencies** | Jackson, JDBC | Spark SQL, JDBC |

---

## Architecture Comparison

### POJO-based Execution Flow (main branch)

```
ReportService.execute(reportName, params, EmployeeReport.class)
  ↓
Load metadata (with columnMappings + resultClass)
  ↓
Execute stored procedure → ResultSet
  ↓
ResultSetToMapConverter (uses explicit column-to-field mappings)
  ↓
JacksonPojoMapper (Map → POJO conversion)
  ↓
ReportResult<EmployeeReport> (typed list of POJOs)
```

### Spark-based Execution Flow (spark-impl branch)

```
ReportService.execute(reportName, params)
  ↓
Load metadata (NO columnMappings, NO resultClass)
  ↓
Execute stored procedure → ResultSet
  ↓
SparkDatasetConverter (auto-infer schema from ResultSetMetaData)
  ↓
Dataset<Row> (Spark DataFrame)
  ↓
SparkReportResult (Dataset + output params → JSON)
```

---

## Metadata Changes

### Before: POJO-based Metadata

```json
{
  "reportName": "employee_report",
  "storedProcedure": "dbo.usp_GetEmployees",
  "resultClass": "com.reporting.framework.example.EmployeeReport",
  "inputParameters": [
    {
      "name": "DepartmentId",
      "sqlType": "INTEGER",
      "javaType": "java.lang.Integer",
      "required": true
    }
  ],
  "outputParameters": [
    {
      "name": "TotalCount",
      "sqlType": "INTEGER",
      "javaType": "java.lang.Integer"
    }
  ],
  "resultSetMapping": {
    "strategy": "EXPLICIT",
    "columnMappings": [
      {"column": "employee_id", "field": "employeeId", "required": true},
      {"column": "first_name", "field": "firstName", "required": true},
      {"column": "last_name", "field": "lastName", "required": true},
      {"column": "email", "field": "email", "required": true},
      {"column": "salary", "field": "salary", "required": true},
      {"column": "hire_date", "field": "hireDate", "required": true},
      {"column": "department_id", "field": "departmentId", "required": true}
    ],
    "unmappedColumnsStrategy": "IGNORE"
  }
}
```

### After: Spark-based Metadata

```json
{
  "reportName": "employee_report",
  "storedProcedure": "dbo.usp_GetEmployees",
  "inputParameters": [
    {
      "name": "DepartmentId",
      "sqlType": "INTEGER",
      "javaType": "java.lang.Integer",
      "required": true
    }
  ],
  "outputParameters": [
    {
      "name": "TotalCount",
      "sqlType": "INTEGER",
      "javaType": "java.lang.Integer"
    }
  ],
  "sparkConfig": {
    "enabled": true,
    "partitions": 10,
    "cacheResult": false
  }
}
```

**Key Changes:**
- Removed `resultClass` field
- Removed `resultSetMapping` and all `columnMappings`
- Added optional `sparkConfig` for Spark-specific settings
- Schema is now inferred automatically from database column names

---

## Code Examples

### Example 1: Basic Report Execution

#### POJO-based (main branch)

```java
// Define POJO class
public class EmployeeReport {
    private Integer employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private BigDecimal salary;
    private LocalDate hireDate;
    private Integer departmentId;
    // getters and setters...
}

// Execute report
ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
ReportService service = new ReportService(provider);

Map<String, Object> params = Map.of("DepartmentId", 10);
ReportResult<EmployeeReport> result = service.execute(
    "employee_report",
    params,
    EmployeeReport.class
);

// Use typed POJOs
List<EmployeeReport> employees = result.getResults();
employees.forEach(emp -> {
    System.out.println(emp.getFirstName() + " " + emp.getLastName());
    System.out.println("Salary: $" + emp.getSalary());
});

Integer totalCount = (Integer) result.getOutputParameters().get("TotalCount");
System.out.println("Total employees: " + totalCount);
```

#### Spark-based (spark-impl branch)

```java
// No POJO class needed!

// Execute report
SparkConfig sparkConfig = SparkConfig.builder()
    .appName("Reporting Framework")
    .master("local[*]")
    .build();

ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
ReportService service = new ReportService(sparkConfig, provider);

Map<String, Object> params = Map.of("DepartmentId", 10);
SparkReportResult result = service.execute("employee_report", params);

// Option 1: Get JSON directly
String json = result.toJSON();
System.out.println(json);

// Option 2: Use Spark Dataset for transformations
Dataset<Row> employees = result.getDataset();
employees.show(); // Display in console

// Option 3: Process rows
employees.collectAsList().forEach(row -> {
    System.out.println(row.getAs("first_name") + " " + row.getAs("last_name"));
    System.out.println("Salary: $" + row.getAs("salary"));
});

Integer totalCount = (Integer) result.getOutputParameters().get("TotalCount");
System.out.println("Total employees: " + totalCount);
```

### Example 2: Data Transformations

#### POJO-based (main branch)

```java
// Manual Java stream operations
List<EmployeeReport> highEarners = result.getResults().stream()
    .filter(emp -> emp.getSalary().compareTo(BigDecimal.valueOf(80000)) > 0)
    .sorted(Comparator.comparing(EmployeeReport::getSalary).reversed())
    .limit(10)
    .collect(Collectors.toList());

// Calculate average salary manually
double avgSalary = result.getResults().stream()
    .mapToDouble(emp -> emp.getSalary().doubleValue())
    .average()
    .orElse(0.0);
```

#### Spark-based (spark-impl branch)

```java
import static com.reporting.framework.spark.SparkOperations.*;
import static org.apache.spark.sql.functions.*;

// Spark DataFrame transformations
Dataset<Row> employees = result.getDataset();

// Filter high earners
Dataset<Row> highEarners = filter(employees, "salary > 80000");
Dataset<Row> top10 = orderByDesc(highEarners, "salary");
top10 = limit(top10, 10);

// Calculate statistics
Dataset<Row> stats = employees.groupBy(
    new String[]{"department_id"},
    Map.of("salary", "avg", "employee_id", "count")
);
stats.show();

// Complex aggregations
Dataset<Row> summary = employees
    .groupBy("department_id")
    .agg(
        avg("salary").as("avg_salary"),
        max("salary").as("max_salary"),
        min("salary").as("min_salary"),
        count("*").as("employee_count")
    );

// Convert to JSON
String summaryJson = summary.toJSON().collectAsList().toString();
```

### Example 3: Joining Multiple Reports

#### POJO-based (main branch)

```java
// Requires manual joining in Java
List<EmployeeReport> employees = employeeResult.getResults();
List<DepartmentReport> departments = deptResult.getResults();

Map<Integer, DepartmentReport> deptMap = departments.stream()
    .collect(Collectors.toMap(DepartmentReport::getDepartmentId, d -> d));

List<EmployeeWithDept> joined = employees.stream()
    .map(emp -> {
        DepartmentReport dept = deptMap.get(emp.getDepartmentId());
        return new EmployeeWithDept(emp, dept);
    })
    .collect(Collectors.toList());
```

#### Spark-based (spark-impl branch)

```java
// Native Spark join operations
Dataset<Row> employees = empResult.getDataset();
Dataset<Row> departments = deptResult.getDataset();

// Inner join on department_id
Dataset<Row> joined = SparkOperations.join(employees, departments, "department_id");

// Or custom join with left outer
import static org.apache.spark.sql.functions.col;
Dataset<Row> joined = employees.join(
    departments,
    col("department_id").equalTo(col("dept_id")),
    "left_outer"
);

// Select specific columns
Dataset<Row> result = SparkOperations.select(
    joined,
    "first_name", "last_name", "department_name", "salary"
);

// Convert to JSON
String json = result.toJSON().collectAsList().toString();
```

---

## Migration Checklist

### Database Changes

- [ ] Update `REPORT_METADATA` table schema (make `RESULT_CLASS` nullable)
  ```sql
  ALTER TABLE REPORT_METADATA ALTER COLUMN RESULT_CLASS VARCHAR(500) NULL;
  ```

- [ ] Update existing metadata JSON:
  - [ ] Remove `resultClass` field
  - [ ] Remove `resultSetMapping` and all `columnMappings`
  - [ ] Add optional `sparkConfig` section

### Code Changes

- [ ] Add Apache Spark dependency to `pom.xml`
- [ ] Create `SparkConfig` configuration
- [ ] Update `ReportService` instantiation to accept `SparkConfig`
- [ ] Change all `ReportResult<T>` return types to `SparkReportResult`
- [ ] Remove POJO class definitions (EmployeeReport, etc.)
- [ ] Update result processing code to use `Dataset<Row>` or JSON
- [ ] Add Spark transformation logic as needed

### Deployment Considerations

- [ ] Spark requires more memory than POJO-based approach
  - Recommended: 1GB+ for executor and driver
- [ ] Consider Spark master configuration:
  - Development: `local[*]`
  - Production: Connect to Spark cluster
- [ ] Monitor Spark UI (if enabled) for performance tuning
- [ ] Consider caching frequently accessed Datasets

---

## Benefits of Spark Approach

### Advantages

1. **No Manual Mapping Required**
   - Schema auto-inferred from database
   - No need to maintain POJO classes
   - No explicit column-to-field mappings

2. **Dynamic Transformations**
   - Filter, join, aggregate at runtime
   - No code changes for new transformations
   - Full Spark SQL capabilities

3. **Scalability**
   - Can scale to cluster mode
   - Built-in optimization (Catalyst optimizer)
   - Lazy evaluation for efficiency

4. **Flexibility**
   - Output JSON for APIs
   - Output Datasets for further processing
   - Output to multiple formats (Parquet, CSV, etc.)

### Trade-offs

1. **Type Safety**
   - No compile-time type checking on row fields
   - Must use string column names
   - Runtime errors for missing columns

2. **Resource Usage**
   - Higher memory requirements
   - More dependencies (Spark ecosystem)
   - Slower startup time (SparkSession initialization)

3. **Learning Curve**
   - Requires Spark/DataFrame knowledge
   - Different paradigm from Java streams
   - Need to understand Spark execution model

---

## Example: Complete Migration

### Before (POJO-based)

```java
// 1. Define POJO
public class EmployeeReport {
    private Integer employeeId;
    private String firstName;
    // ... 20 more fields
}

// 2. Define metadata JSON with 20 column mappings
{
  "resultClass": "com.example.EmployeeReport",
  "resultSetMapping": {
    "columnMappings": [
      {"column": "employee_id", "field": "employeeId"},
      // ... 19 more mappings
    ]
  }
}

// 3. Execute and process
ReportService service = new ReportService(provider);
ReportResult<EmployeeReport> result = service.execute("report", params, EmployeeReport.class);
List<EmployeeReport> data = result.getResults();
```

### After (Spark-based)

```java
// 1. No POJO needed!

// 2. Simplified metadata JSON (no mappings)
{
  "sparkConfig": {"enabled": true}
}

// 3. Execute and process
SparkConfig config = SparkConfig.builder().build();
ReportService service = new ReportService(config, provider);
SparkReportResult result = service.execute("report", params);

// Option A: JSON output
String json = result.toJSON();

// Option B: Spark transformations
Dataset<Row> data = result.getDataset();
Dataset<Row> filtered = data.filter("salary > 80000");
```

**Lines of code:** ~100 → ~10

---

## Support

For questions or issues with the Spark migration, please refer to:
- Apache Spark documentation: https://spark.apache.org/docs/latest/
- Spark SQL guide: https://spark.apache.org/docs/latest/sql-programming-guide.html
