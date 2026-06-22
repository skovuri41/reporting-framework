# Apache Spark-Based Reporting Framework

A metadata-driven Java framework for executing SQL Server stored procedures and transforming results using Apache Spark DataFrames. **No POJOs or column mappings required!**

> **Note:** This is the `spark-impl` branch, which uses Apache Spark for dynamic data operations. For the POJO-based approach with explicit column mappings, see the `main` branch.

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Quick Start](#quick-start)
- [Architecture](#architecture)
- [Usage Examples](#usage-examples)
- [Metadata Structure](#metadata-structure)
- [Data Transformations](#data-transformations)
- [Migration from POJO-Based Approach](#migration-from-pojo-based-approach)
- [Build and Test](#build-and-test)
- [Benefits and Trade-offs](#benefits-and-trade-offs)
- [API Reference](#api-reference)
- [Requirements](#requirements)

---

## Overview

This framework replaces manual POJO mapping with **Apache Spark's Dataset API** for dynamic data processing. The core principle remains: **add new reports without modifying framework code** (Open/Closed Principle).

### What's Different from POJO-Based Approach?

| Aspect | POJO-based (main branch) | Spark-based (this branch) |
|--------|--------------------------|----------------------------|
| **Schema Definition** | Explicit column mappings in JSON | Auto-inferred from ResultSet |
| **Return Type** | `ReportResult<T>` (typed POJOs) | `SparkReportResult` (Dataset<Row>) |
| **POJO Classes** | Required for each report | Not needed |
| **Column Mappings** | Explicit in metadata | Auto-generated |
| **Transformations** | Manual Java streams | Spark DataFrame operations |
| **Output Format** | Typed Java objects | JSON, Dataset, or custom |
| **Scalability** | Single-machine | Can scale to Spark cluster |

---

## Key Features

- **Zero POJO Classes**: No need to create and maintain Java classes for each report
- **Auto-Inferred Schema**: Database column names become Dataset schema automatically
- **Spark SQL Power**: Full access to Spark DataFrame API for transformations
- **Dynamic Operations**: Filter, join, aggregate, pivot without code changes
- **Metadata-Driven**: Report configuration stored in database, fetched at runtime
- **JSON Output**: Convert results to JSON for REST APIs
- **Lazy Evaluation**: Spark's lazy execution for optimized performance
- **Scalable**: Can run locally or on a Spark cluster
- **Output Parameters**: Full support for stored procedure output parameters
- **Metadata Caching**: In-memory caching for performance

---

## Quick Start

### 1. Setup Database Schema

```sql
CREATE TABLE REPORT_METADATA (
    REPORT_NAME VARCHAR(100) PRIMARY KEY,
    STORED_PROCEDURE VARCHAR(200) NOT NULL,
    RESULT_CLASS VARCHAR(500) NULL,  -- Nullable for Spark approach
    METADATA_JSON NVARCHAR(MAX) NOT NULL,
    CREATED_DATE DATETIME DEFAULT GETDATE(),
    UPDATED_DATE DATETIME DEFAULT GETDATE()
);
```

### 2. Create a Stored Procedure

```sql
CREATE PROCEDURE dbo.usp_GetEmployees
    @DepartmentId INT,
    @StartDate DATE = NULL,
    @TotalCount INT OUTPUT
AS
BEGIN
    SELECT employee_id, first_name, last_name, email, salary, hire_date, department_id
    FROM employees
    WHERE department_id = @DepartmentId
        AND (@StartDate IS NULL OR hire_date >= @StartDate)
    ORDER BY hire_date DESC;

    SELECT @TotalCount = COUNT(*)
    FROM employees
    WHERE department_id = @DepartmentId
        AND (@StartDate IS NULL OR hire_date >= @StartDate);
END;
```

### 3. Insert Metadata (No POJO, No Column Mappings!)

```sql
INSERT INTO REPORT_METADATA (REPORT_NAME, STORED_PROCEDURE, METADATA_JSON)
VALUES ('employee_report', 'dbo.usp_GetEmployees',
'{
  "reportName": "employee_report",
  "storedProcedure": "dbo.usp_GetEmployees",
  "inputParameters": [
    {
      "name": "DepartmentId",
      "sqlType": "INTEGER",
      "javaType": "java.lang.Integer",
      "required": true
    },
    {
      "name": "StartDate",
      "sqlType": "DATE",
      "javaType": "java.time.LocalDate",
      "required": false
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
}');
```

**Key differences from POJO approach:**
- ❌ No `resultClass` field
- ❌ No `resultSetMapping` or `columnMappings`
- ✅ Optional `sparkConfig` for Spark-specific settings

### 4. Execute the Report

```java
import com.reporting.framework.api.ReportService;
import com.reporting.framework.spark.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

// Setup Spark
SparkConfig sparkConfig = SparkConfig.builder()
    .appName("Reporting Framework")
    .master("local[*]")
    .build();

// Setup connection
DataSource dataSource = ... // HikariCP or other
ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);

// Create service
ReportService service = new ReportService(sparkConfig, provider);

// Execute report
Map<String, Object> params = Map.of(
    "DepartmentId", 10,
    "StartDate", LocalDate.of(2020, 1, 1)
);

SparkReportResult result = service.execute("employee_report", params);

// Option 1: Get JSON directly
String json = result.toJSON();
System.out.println(json);

// Option 2: Get Spark Dataset for transformations
Dataset<Row> employees = result.getDataset();
employees.show(); // Display in console

// Option 3: Access output parameters
Integer totalCount = (Integer) result.getOutputParameters().get("TotalCount");
System.out.println("Total: " + totalCount);
```

---

## Architecture

### Execution Flow

```
Client Code
    ↓
┌───────────────────────────────────────────────────────────────┐
│ API Layer: ReportService                                       │
│ - Entry point for report execution                             │
│ - Handles Spark session management                             │
│ - Manages metadata caching                                     │
└───────────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────────┐
│ Configuration Layer: MetadataLoader                            │
│ - Loads report metadata from REPORT_METADATA table             │
│ - Parses JSON configuration                                    │
│ - In-memory caching (ConcurrentHashMap)                        │
└───────────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────────┐
│ Execution Layer: StoredProcedureExecutor                       │
│ - Builds dynamic CallableStatement: {call proc(?, ?)}         │
│ - GenericParameterBinder: Java types → SQL types              │
│ - Handles input/output parameter binding                       │
│ - Returns ResultSet + output parameters                        │
└───────────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────────┐
│ Spark Layer: SparkDatasetConverter                            │
│ - Auto-infers schema from ResultSetMetaData                   │
│ - Converts ResultSet → Dataset<Row>                           │
│ - Preserves original database column names                     │
│ - No manual mapping required                                   │
└───────────────────────────────────────────────────────────────┘
    ↓
SparkReportResult (Dataset<Row> + output parameters)
```

### Key Architectural Patterns

**Auto-Schema Inference:**
The framework automatically creates a Spark schema from `ResultSetMetaData`, eliminating the need for explicit column mappings or POJO classes.

**Lazy Evaluation:**
Spark's lazy evaluation means transformations are not executed until an action is triggered (e.g., `collect()`, `show()`, `toJSON()`).

**Dataset API:**
Full access to Spark's powerful DataFrame/Dataset API for complex transformations, aggregations, and joins.

---

## Usage Examples

### Example 1: Basic Report Execution

```java
// No POJO class needed!

SparkConfig config = SparkConfig.builder()
    .appName("Reporting")
    .master("local[*]")
    .build();

ReportService service = new ReportService(config, provider);
Map<String, Object> params = Map.of("DepartmentId", 10);

SparkReportResult result = service.execute("employee_report", params);

// Get JSON
String json = result.toJSON();

// Get Dataset
Dataset<Row> data = result.getDataset();
data.show();

// Access specific columns
data.select("first_name", "last_name", "salary").show();

// Get output parameters
Integer total = (Integer) result.getOutputParameters().get("TotalCount");
```

### Example 2: Filtering and Sorting

```java
import static com.reporting.framework.spark.SparkOperations.*;

SparkReportResult result = service.execute("employee_report", params);
Dataset<Row> employees = result.getDataset();

// Filter high earners
Dataset<Row> highEarners = filter(employees, "salary > 80000");

// Sort by salary descending
Dataset<Row> sorted = orderByDesc(highEarners, "salary");

// Limit to top 10
Dataset<Row> top10 = limit(sorted, 10);

// Show results
top10.show();

// Convert to JSON
String json = top10.toJSON().collectAsList().toString();
```

### Example 3: Aggregations

```java
import static org.apache.spark.sql.functions.*;

Dataset<Row> employees = result.getDataset();

// Group by department with multiple aggregations
Dataset<Row> summary = employees
    .groupBy("department_id")
    .agg(
        avg("salary").as("avg_salary"),
        max("salary").as("max_salary"),
        min("salary").as("min_salary"),
        count("*").as("employee_count")
    );

summary.show();

// Using SparkOperations helper
Dataset<Row> avgSalary = groupBy(
    employees,
    new String[]{"department_id"},
    Map.of("salary", "avg")
);
```

### Example 4: Joining Multiple Reports

```java
// Execute two reports
SparkReportResult empResult = service.execute("employee_report", empParams);
SparkReportResult deptResult = service.execute("department_report", deptParams);

Dataset<Row> employees = empResult.getDataset();
Dataset<Row> departments = deptResult.getDataset();

// Inner join on department_id
Dataset<Row> joined = SparkOperations.join(employees, departments, "department_id");

// Select specific columns
Dataset<Row> result = select(
    joined,
    "first_name", "last_name", "department_name", "salary"
);

result.show();

// Or use Spark DataFrame API directly
import static org.apache.spark.sql.functions.col;

Dataset<Row> customJoin = employees.join(
    departments,
    col("department_id").equalTo(col("dept_id")),
    "left_outer"
);
```

### Example 5: Complex Transformations

```java
Dataset<Row> employees = result.getDataset();

// Add computed columns
Dataset<Row> withBonus = employees.withColumn(
    "bonus",
    col("salary").multiply(0.10)
);

// Filter and transform
Dataset<Row> seniorHighEarners = employees
    .filter(col("hire_date").lt(lit("2015-01-01")))
    .filter(col("salary").gt(100000))
    .withColumn("years_of_service",
        datediff(current_date(), col("hire_date")).divide(365))
    .select("first_name", "last_name", "salary", "years_of_service")
    .orderBy(col("years_of_service").desc());

seniorHighEarners.show();
```

### Example 6: Spring Boot Integration

```java
@Configuration
public class ReportingConfig {

    @Bean
    public SparkConfig sparkConfig() {
        return SparkConfig.builder()
            .appName("Reporting Service")
            .master("local[*]")
            .set("spark.sql.shuffle.partitions", "10")
            .build();
    }

    @Bean
    public ConnectionProvider connectionProvider(DataSource dataSource) {
        return new DataSourceConnectionProvider(dataSource);
    }

    @Bean
    public ReportService reportService(
            SparkConfig sparkConfig,
            ConnectionProvider connectionProvider) {
        return new ReportService(sparkConfig, connectionProvider);
    }
}
```

```java
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/{reportName}")
    public String executeReport(
            @PathVariable String reportName,
            @RequestBody Map<String, Object> params) {

        SparkReportResult result = reportService.execute(reportName, params);
        return result.toJSON();
    }

    @PostMapping("/employees/high-earners")
    public String getHighEarners(@RequestBody Map<String, Object> params) {
        SparkReportResult result = reportService.execute("employee_report", params);

        Dataset<Row> highEarners = result.getDataset()
            .filter("salary > 80000")
            .orderBy(col("salary").desc())
            .limit(10);

        return highEarners.toJSON().collectAsList().toString();
    }
}
```

---

## Metadata Structure

### Simplified JSON Format (No Column Mappings!)

```json
{
  "reportName": "report_name",
  "storedProcedure": "dbo.usp_ProcedureName",
  "inputParameters": [
    {
      "name": "ParameterName",
      "sqlType": "INTEGER|VARCHAR|DATE|DECIMAL|...",
      "javaType": "java.lang.Integer|java.lang.String|...",
      "required": true|false
    }
  ],
  "outputParameters": [
    {
      "name": "OutputParamName",
      "sqlType": "INTEGER|DECIMAL|...",
      "javaType": "java.lang.Integer|java.math.BigDecimal|..."
    }
  ],
  "sparkConfig": {
    "enabled": true,
    "partitions": 10,
    "cacheResult": false
  }
}
```

### Supported SQL Types

| SQL Type | Java Type | Spark Type |
|----------|-----------|------------|
| INTEGER, INT | java.lang.Integer | IntegerType |
| BIGINT | java.lang.Long | LongType |
| DECIMAL, NUMERIC | java.math.BigDecimal | DecimalType |
| VARCHAR, NVARCHAR | java.lang.String | StringType |
| DATE | java.time.LocalDate | DateType |
| DATETIME, TIMESTAMP | java.time.LocalDateTime | TimestampType |
| BIT, BOOLEAN | java.lang.Boolean | BooleanType |
| FLOAT, REAL | java.lang.Float | FloatType |
| DOUBLE | java.lang.Double | DoubleType |

---

## Data Transformations

### SparkOperations Helper Class

The `SparkOperations` utility class provides common transformation operations:

```java
import static com.reporting.framework.spark.SparkOperations.*;

// Filtering
Dataset<Row> filtered = filter(dataset, "column > 100");

// Selection
Dataset<Row> selected = select(dataset, "col1", "col2", "col3");

// Ordering
Dataset<Row> sorted = orderBy(dataset, "column");
Dataset<Row> sortedDesc = orderByDesc(dataset, "column");

// Limiting
Dataset<Row> limited = limit(dataset, 10);

// Distinct
Dataset<Row> unique = distinct(dataset);

// Joins
Dataset<Row> joined = join(left, right, "join_column");
Dataset<Row> leftJoin = leftJoin(left, right, "join_column");
Dataset<Row> rightJoin = rightJoin(left, right, "join_column");

// Grouping and aggregation
Dataset<Row> grouped = groupBy(dataset,
    new String[]{"group_column"},
    Map.of("value_column", "sum", "count_column", "count")
);

// Union
Dataset<Row> combined = union(dataset1, dataset2);
```

### Native Spark DataFrame API

You can also use Spark's native DataFrame API directly:

```java
import static org.apache.spark.sql.functions.*;

Dataset<Row> data = result.getDataset();

// Complex filtering
Dataset<Row> filtered = data
    .filter(col("salary").gt(50000))
    .filter(col("department_id").isin(10, 20, 30))
    .filter(col("hire_date").between("2020-01-01", "2023-12-31"));

// Window functions
import org.apache.spark.sql.expressions.Window;

Dataset<Row> ranked = data.withColumn(
    "rank",
    row_number().over(
        Window.partitionBy("department_id")
              .orderBy(col("salary").desc())
    )
);

// Pivot operations
Dataset<Row> pivoted = data
    .groupBy("department_id")
    .pivot("hire_year")
    .agg(count("employee_id"));

// UDFs (User Defined Functions)
spark.udf().register("salary_grade", (Double salary) -> {
    if (salary > 100000) return "A";
    if (salary > 70000) return "B";
    return "C";
}, DataTypes.StringType);

Dataset<Row> withGrade = data.selectExpr(
    "*",
    "salary_grade(salary) as grade"
);
```

---

## Migration from POJO-Based Approach

### Step-by-Step Migration Guide

#### 1. Update Database Schema

Make `RESULT_CLASS` column nullable:

```sql
ALTER TABLE REPORT_METADATA
ALTER COLUMN RESULT_CLASS VARCHAR(500) NULL;
```

#### 2. Update Metadata JSON

**Before (POJO-based):**
```json
{
  "reportName": "employee_report",
  "storedProcedure": "dbo.usp_GetEmployees",
  "resultClass": "com.example.EmployeeReport",
  "inputParameters": [...],
  "outputParameters": [...],
  "resultSetMapping": {
    "strategy": "EXPLICIT",
    "columnMappings": [
      {"column": "employee_id", "field": "employeeId", "required": true},
      {"column": "first_name", "field": "firstName", "required": true}
      // ... 20 more mappings
    ]
  }
}
```

**After (Spark-based):**
```json
{
  "reportName": "employee_report",
  "storedProcedure": "dbo.usp_GetEmployees",
  "inputParameters": [...],
  "outputParameters": [...],
  "sparkConfig": {
    "enabled": true
  }
}
```

#### 3. Update Dependencies

Add Apache Spark to `pom.xml`:

```xml
<dependency>
    <groupId>org.apache.spark</groupId>
    <artifactId>spark-sql_2.12</artifactId>
    <version>3.5.0</version>
</dependency>
```

#### 4. Update Java Code

**Before (POJO-based):**
```java
// Define POJO class (20+ lines)
public class EmployeeReport {
    private Integer employeeId;
    private String firstName;
    // ... 20 more fields
    // ... getters/setters
}

// Execute
ReportService service = new ReportService(provider);
ReportResult<EmployeeReport> result = service.execute(
    "employee_report",
    params,
    EmployeeReport.class
);

// Use typed POJOs
List<EmployeeReport> data = result.getResults();
data.forEach(emp -> {
    System.out.println(emp.getFirstName());
});
```

**After (Spark-based):**
```java
// No POJO class needed!

// Execute
SparkConfig config = SparkConfig.builder().build();
ReportService service = new ReportService(config, provider);
SparkReportResult result = service.execute("employee_report", params);

// Option 1: JSON
String json = result.toJSON();

// Option 2: Spark operations
Dataset<Row> data = result.getDataset();
data.show();

// Option 3: Process rows
data.collectAsList().forEach(row -> {
    System.out.println(row.getAs("first_name"));
});
```

**Lines of code reduced:** ~100 → ~10

#### 5. Delete Obsolete Code

After migration, you can delete:
- ✅ All POJO classes (EmployeeReport, SalesReport, etc.)
- ✅ All explicit column mappings from metadata
- ✅ `resultClass` field from metadata

---

## Build and Test

### Maven Commands

```bash
# Build the project
mvn clean compile

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=SparkDatasetConverterTest

# Run a specific test method
mvn test -Dtest=SparkDatasetConverterTest#testAutoSchemaInference

# Package the JAR
mvn package

# Skip tests during build
mvn clean install -DskipTests
```

### Testing Strategy

**Unit Tests:**
- `SparkDatasetConverterTest` - Schema inference, type mapping
- `SparkOperationsTest` - Transformation operations
- `SparkReportResultTest` - Result handling, JSON conversion

**Integration Tests:**
- Require SQL Server for full stored procedure testing
- H2 integration tests disabled due to stored procedure limitations

**Test Coverage:**
- All core mapping and conversion logic
- Spark schema inference from ResultSetMetaData
- Data transformation operations
- Output parameter handling

---

## Benefits and Trade-offs

### Advantages of Spark Approach

✅ **No Manual Mapping**
   - Schema auto-inferred from database
   - No POJO classes to maintain
   - No explicit column-to-field mappings

✅ **Dynamic Transformations**
   - Filter, join, aggregate at runtime
   - No code changes for new transformations
   - Full Spark SQL capabilities

✅ **Scalability**
   - Can scale to Spark cluster mode
   - Built-in optimization (Catalyst optimizer)
   - Lazy evaluation for efficiency

✅ **Flexibility**
   - Output JSON for REST APIs
   - Output Datasets for processing
   - Output to Parquet, CSV, etc.

✅ **Less Code**
   - Eliminate 100+ lines of POJO definitions
   - Eliminate dozens of column mappings
   - Cleaner, more maintainable codebase

### Trade-offs

⚠️ **Type Safety**
   - No compile-time type checking on row fields
   - Must use string column names: `row.getAs("column")`
   - Runtime errors for missing columns

⚠️ **Resource Usage**
   - Higher memory requirements (1GB+ recommended)
   - More dependencies (Spark ecosystem)
   - Slower startup time (SparkSession initialization)

⚠️ **Learning Curve**
   - Requires Spark/DataFrame knowledge
   - Different paradigm from Java streams
   - Understanding Spark execution model

⚠️ **Deployment Complexity**
   - Spark configuration needed
   - Larger application size
   - May require cluster setup for production scale

---

## API Reference

### ReportService

```java
public class ReportService {
    // Constructor
    public ReportService(SparkConfig sparkConfig, ConnectionProvider provider)

    // Execute report and return Spark Dataset
    public SparkReportResult execute(String reportName, Map<String, Object> params)
    public SparkReportResult execute(String reportName, Object inputPojo)

    // Cache management
    public void clearMetadataCache()
    public void invalidateMetadataCache(String reportName)
    public int getMetadataCacheSize()
}
```

### SparkReportResult

```java
public class SparkReportResult {
    // Get Spark Dataset for transformations
    public Dataset<Row> getDataset()

    // Get output parameters from stored procedure
    public Map<String, Object> getOutputParameters()
    public Object getOutputParameter(String name)

    // Convert to JSON
    public String toJSON()
    public String toPrettyJSON()

    // Metadata
    public String getReportName()
    public long getExecutionTime()
    public int getResultCount()
}
```

### SparkOperations

```java
public class SparkOperations {
    // Filtering
    public static Dataset<Row> filter(Dataset<Row> dataset, String condition)

    // Selection
    public static Dataset<Row> select(Dataset<Row> dataset, String... columns)

    // Ordering
    public static Dataset<Row> orderBy(Dataset<Row> dataset, String column)
    public static Dataset<Row> orderByDesc(Dataset<Row> dataset, String column)

    // Limiting
    public static Dataset<Row> limit(Dataset<Row> dataset, int n)

    // Distinct
    public static Dataset<Row> distinct(Dataset<Row> dataset)

    // Joins
    public static Dataset<Row> join(Dataset<Row> left, Dataset<Row> right, String column)
    public static Dataset<Row> leftJoin(Dataset<Row> left, Dataset<Row> right, String column)
    public static Dataset<Row> rightJoin(Dataset<Row> left, Dataset<Row> right, String column)

    // Aggregations
    public static Dataset<Row> groupBy(Dataset<Row> dataset,
                                        String[] groupColumns,
                                        Map<String, String> aggregations)

    // Union
    public static Dataset<Row> union(Dataset<Row> first, Dataset<Row> second)
}
```

### SparkConfig

```java
public class SparkConfig {
    public static Builder builder()

    public static class Builder {
        public Builder appName(String appName)
        public Builder master(String master)
        public Builder set(String key, String value)
        public SparkConfig build()
    }
}
```

---

## Package Structure

```
com.reporting.framework/
├── api/                       - Public API surface
│   ├── ReportService          - Main entry point
│   └── ReportResult           - (Unused in Spark approach)
│
├── spark/                     - Spark-specific components
│   ├── SparkConfig            - Spark session configuration
│   ├── SparkDatasetConverter  - ResultSet → Dataset<Row>
│   ├── SparkReportResult      - Result wrapper with Dataset
│   └── SparkOperations        - Transformation utilities
│
├── executor/                  - Stored procedure execution
│   ├── StoredProcedureExecutor    - CallableStatement management
│   ├── GenericParameterBinder     - Parameter type mapping
│   └── ExecutionResult            - Raw execution results
│
├── metadata/                  - Configuration and metadata
│   ├── MetadataLoader         - Load + cache metadata
│   ├── MetadataRepository     - Database access
│   ├── ReportMetadata         - Report configuration model
│   └── ParameterMetadata      - Parameter definition
│
├── connection/                - Database connection abstraction
│   ├── ConnectionProvider             - Interface
│   ├── DataSourceConnectionProvider   - For connection pools
│   └── SimpleConnectionProvider       - For direct JDBC URLs
│
└── exception/                 - Framework exceptions
    ├── ReportExecutionException
    └── MetadataException
```

---

## Requirements

- **Java 17+**
- **Apache Spark 3.5+**
- **SQL Server** (or H2 with SQL Server mode for testing)
- **SLF4J** for logging
- **Recommended:** 1GB+ memory for Spark executor and driver

### Maven Dependency

```xml
<dependency>
    <groupId>com.reporting</groupId>
    <artifactId>reporting-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Spark Configuration

**Development (Local Mode):**
```java
SparkConfig config = SparkConfig.builder()
    .appName("Reporting")
    .master("local[*]")  // Use all available cores
    .build();
```

**Production (Cluster Mode):**
```java
SparkConfig config = SparkConfig.builder()
    .appName("Reporting Production")
    .master("spark://master:7077")
    .set("spark.executor.memory", "2g")
    .set("spark.driver.memory", "1g")
    .set("spark.sql.shuffle.partitions", "200")
    .build();
```

---

## Performance Considerations

- **Metadata Caching:** Metadata is cached after first load to avoid repeated DB queries
- **Lazy Evaluation:** Spark transformations are lazy - optimize by combining operations
- **Connection Pooling:** Use HikariCP via `DataSourceConnectionProvider`
- **Partitioning:** Configure `spark.sql.shuffle.partitions` based on data volume
- **Caching:** Cache frequently accessed Datasets with `dataset.cache()`
- **Memory:** Allocate sufficient memory for Spark (1GB+ recommended)

---

## Examples and Use Cases

### Use Case 1: REST API with JSON Output

```java
@PostMapping("/reports/{reportName}")
public ResponseEntity<String> executeReport(
        @PathVariable String reportName,
        @RequestBody Map<String, Object> params) {

    SparkReportResult result = reportService.execute(reportName, params);
    return ResponseEntity.ok(result.toJSON());
}
```

### Use Case 2: Complex Analytics

```java
SparkReportResult salesResult = reportService.execute("sales_report", params);
Dataset<Row> sales = salesResult.getDataset();

// Monthly revenue by region
Dataset<Row> monthlyRevenue = sales
    .withColumn("month", date_format(col("sale_date"), "yyyy-MM"))
    .groupBy("region", "month")
    .agg(
        sum("amount").as("total_revenue"),
        count("*").as("transaction_count"),
        avg("amount").as("avg_transaction")
    )
    .orderBy("region", "month");

monthlyRevenue.show();
```

### Use Case 3: Multi-Report Joining

```java
SparkReportResult empResult = reportService.execute("employees", empParams);
SparkReportResult deptResult = reportService.execute("departments", deptParams);
SparkReportResult projResult = reportService.execute("projects", projParams);

Dataset<Row> employees = empResult.getDataset();
Dataset<Row> departments = deptResult.getDataset();
Dataset<Row> projects = projResult.getDataset();

// Join all three
Dataset<Row> complete = employees
    .join(departments, "department_id")
    .join(projects, "project_id")
    .select(
        "employee_name",
        "department_name",
        "project_name",
        "salary",
        "project_budget"
    );

String json = complete.toJSON().collectAsList().toString();
```

---

## License

MIT License

## Contributing

Contributions welcome! Please submit a pull request or open an issue.

## Additional Resources

- [Apache Spark Documentation](https://spark.apache.org/docs/latest/)
- [Spark SQL Guide](https://spark.apache.org/docs/latest/sql-programming-guide.html)
- [Dataset API Reference](https://spark.apache.org/docs/latest/api/java/org/apache/spark/sql/Dataset.html)

---

**Branch Information:**
- **This branch (`spark-impl`):** Apache Spark-based approach with auto-inferred schema
- **Main branch:** POJO-based approach with explicit column mappings
- **Dataset-impl branch:** Dual-mode approach supporting both POJO and dynamic operations

Choose the branch that best fits your use case!
