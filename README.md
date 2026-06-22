# Metadata-Driven Java Reporting Framework

A reusable, metadata-driven framework for executing SQL Server stored procedures and mapping results to Java POJOs using Jackson ObjectMapper with explicit column mappings. Add new reports via metadata only - no framework code changes needed!

## 🌟 Features

- **Zero-Code Reports**: Add new reports by inserting metadata only
- **Metadata-Driven**: All configuration stored in database, loaded at runtime
- **Explicit Column Mapping**: Self-documenting column-to-field mappings
- **Jackson-Based Mapping**: Efficient ResultSet → Map → POJO conversion
- **Generic Parameter Binding**: Dynamic SQL parameter mapping with type safety
- **Flexible Input**: Map-based or POJO-based input parameters
- **Output Parameters**: Full support for stored procedure output parameters
- **Metadata Caching**: In-memory caching for performance
- **Type-Safe**: Java 17 with LocalDate, LocalDateTime, BigDecimal support

## 🏗️ Architecture

```
Client
  ↓
ReportService.execute(reportName, params)
  ↓
MetadataLoader → Database (REPORT_METADATA table)
  ↓
StoredProcedureExecutor → CallableStatement
  ↓
ResultSetToMapConverter → List<Map<String, Object>>
  (uses explicit column mappings)
  ↓
JacksonPojoMapper → List<POJO>
  (ObjectMapper.convertValue())
  ↓
ReportResult<T>(results, outputParameters)
```

### Key Components

- **ReportService**: Main API entry point
- **MetadataLoader**: Loads and caches metadata from database
- **StoredProcedureExecutor**: Executes stored procedures with dynamic parameter binding
- **GenericParameterBinder**: Maps Java types to SQL types
- **ResultSetToMapConverter**: Converts ResultSet to List<Map> using explicit column mappings
- **JacksonPojoMapper**: Converts Map to POJO using ObjectMapper
- **PojoToParameterConverter**: Converts input POJO to Map

## 🚀 Quick Start

### 1. Database Setup

```sql
-- Create metadata table
CREATE TABLE REPORT_METADATA (
    REPORT_NAME VARCHAR(100) PRIMARY KEY,
    STORED_PROCEDURE VARCHAR(200) NOT NULL,
    RESULT_CLASS VARCHAR(500) NOT NULL,
    METADATA_JSON NVARCHAR(MAX) NOT NULL,
    CREATED_DATE DATETIME DEFAULT GETDATE(),
    UPDATED_DATE DATETIME DEFAULT GETDATE()
);
```

### 2. Create Business Table and Stored Procedure

```sql
-- Business table
CREATE TABLE employees (
    employee_id INT PRIMARY KEY IDENTITY(1,1),
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    salary DECIMAL(18,2) NOT NULL,
    hire_date DATE NOT NULL,
    department_id INT NOT NULL
);

-- Stored procedure
CREATE PROCEDURE dbo.usp_GetEmployees
    @DepartmentId INT,
    @StartDate DATE = NULL,
    @TotalCount INT OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT employee_id, first_name, last_name, email, salary, hire_date
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

### 3. Create POJO

```java
package com.example.reporting;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EmployeeReport {
    private Integer employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private BigDecimal salary;
    private LocalDate hireDate;

    // Getters and setters
    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public BigDecimal getSalary() { return salary; }
    public void setSalary(BigDecimal salary) { this.salary = salary; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
}
```

### 4. Insert Metadata with Explicit Column Mappings

```sql
INSERT INTO REPORT_METADATA (REPORT_NAME, STORED_PROCEDURE, RESULT_CLASS, METADATA_JSON)
VALUES (
    'employee_report',
    'dbo.usp_GetEmployees',
    'com.example.reporting.EmployeeReport',
    '{
        "reportName": "employee_report",
        "storedProcedure": "dbo.usp_GetEmployees",
        "resultClass": "com.example.reporting.EmployeeReport",
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
        "resultSetMapping": {
            "strategy": "EXPLICIT",
            "columnMappings": [
                {"column": "employee_id", "field": "employeeId", "required": true},
                {"column": "first_name", "field": "firstName", "required": true},
                {"column": "last_name", "field": "lastName", "required": true},
                {"column": "email", "field": "email", "required": true},
                {"column": "salary", "field": "salary", "required": true},
                {"column": "hire_date", "field": "hireDate", "required": true}
            ],
            "unmappedColumnsStrategy": "IGNORE"
        }
    }'
);
```

### 5. Execute the Report

#### Option 1: Map-Based Parameters

```java
import com.reporting.framework.api.ReportResult;
import com.reporting.framework.api.ReportService;
import com.reporting.framework.connection.ConnectionProvider;
import com.reporting.framework.connection.DataSourceConnectionProvider;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Setup
DataSource dataSource = ... // HikariCP, Spring DataSource, etc.
ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
ReportService reportService = new ReportService(provider);

// Prepare parameters
Map<String, Object> params = new HashMap<>();
params.put("DepartmentId", 10);
params.put("StartDate", LocalDate.of(2020, 1, 1));

// Execute
ReportResult<EmployeeReport> result = reportService.execute("employee_report", params);

// Access results
List<EmployeeReport> employees = result.getResults();
Integer totalCount = (Integer) result.getOutputParameter("TotalCount");

System.out.println("Found " + totalCount + " employees:");
for (EmployeeReport emp : employees) {
    System.out.printf("%s %s - %s - $%,.2f (hired %s)%n",
        emp.getFirstName(),
        emp.getLastName(),
        emp.getEmail(),
        emp.getSalary(),
        emp.getHireDate());
}
```

#### Option 2: POJO-Based Parameters (Type-Safe)

```java
// Create input POJO
public class EmployeeReportRequest {
    private Integer departmentId;
    private LocalDate startDate;

    // Getters and setters
    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
}

// Execute with POJO
EmployeeReportRequest request = new EmployeeReportRequest();
request.setDepartmentId(10);
request.setStartDate(LocalDate.of(2020, 1, 1));

ReportResult<EmployeeReport> result = reportService.execute("employee_report", request);
List<EmployeeReport> employees = result.getResults();
```

## 📖 Adding a New Report

The framework follows the **Open/Closed Principle** - add new reports without modifying framework code!

### 3-Step Process:

1. **Create Stored Procedure** in SQL Server
2. **Create POJO Class** with matching fields
3. **Insert Metadata** into REPORT_METADATA table

That's it! No framework code changes needed.

### Example: Sales Report

```sql
-- 1. Create stored procedure
CREATE PROCEDURE dbo.usp_GetSalesReport
    @StartDate DATE,
    @EndDate DATE,
    @TotalRevenue DECIMAL(18,2) OUTPUT
AS
BEGIN
    SELECT
        sale_id,
        product_name,
        sale_amount,
        sale_date,
        customer_name
    FROM sales
    WHERE sale_date BETWEEN @StartDate AND @EndDate
    ORDER BY sale_date DESC;

    SELECT @TotalRevenue = SUM(sale_amount)
    FROM sales
    WHERE sale_date BETWEEN @StartDate AND @EndDate;
END;
```

```java
// 2. Create POJO
public class SalesReport {
    private Integer saleId;
    private String productName;
    private BigDecimal saleAmount;
    private LocalDate saleDate;
    private String customerName;
    // getters/setters
}
```

```sql
-- 3. Insert metadata
INSERT INTO REPORT_METADATA (REPORT_NAME, STORED_PROCEDURE, RESULT_CLASS, METADATA_JSON)
VALUES (
    'sales_report',
    'dbo.usp_GetSalesReport',
    'com.example.SalesReport',
    '{
        "reportName": "sales_report",
        "storedProcedure": "dbo.usp_GetSalesReport",
        "resultClass": "com.example.SalesReport",
        "inputParameters": [
            {"name": "StartDate", "sqlType": "DATE", "javaType": "java.time.LocalDate", "required": true},
            {"name": "EndDate", "sqlType": "DATE", "javaType": "java.time.LocalDate", "required": true}
        ],
        "outputParameters": [
            {"name": "TotalRevenue", "sqlType": "DECIMAL", "javaType": "java.math.BigDecimal"}
        ],
        "resultSetMapping": {
            "strategy": "EXPLICIT",
            "columnMappings": [
                {"column": "sale_id", "field": "saleId", "required": true},
                {"column": "product_name", "field": "productName", "required": true},
                {"column": "sale_amount", "field": "saleAmount", "required": true},
                {"column": "sale_date", "field": "saleDate", "required": true},
                {"column": "customer_name", "field": "customerName", "required": true}
            ]
        }
    }'
);
```

## 📋 Metadata JSON Structure

### Complete Structure

```json
{
  "reportName": "report_name",
  "storedProcedure": "dbo.usp_ProcedureName",
  "resultClass": "com.example.YourPojo",
  "inputParameters": [
    {
      "name": "ParameterName",
      "sqlType": "INTEGER|VARCHAR|DATE|DECIMAL|TIMESTAMP|...",
      "javaType": "java.lang.Integer|java.lang.String|java.time.LocalDate|...",
      "required": true
    }
  ],
  "outputParameters": [
    {
      "name": "OutputParamName",
      "sqlType": "INTEGER|DECIMAL|VARCHAR|...",
      "javaType": "java.lang.Integer|java.math.BigDecimal|..."
    }
  ],
  "resultSetMapping": {
    "strategy": "EXPLICIT",
    "columnMappings": [
      {
        "column": "database_column_name",
        "field": "javaFieldName",
        "required": true
      }
    ],
    "unmappedColumnsStrategy": "IGNORE"
  }
}
```

### Field Descriptions

- **reportName**: Unique identifier for the report
- **storedProcedure**: Fully qualified stored procedure name (e.g., `dbo.usp_MyProc`)
- **resultClass**: Fully qualified Java class name (e.g., `com.example.MyReport`)
- **inputParameters**: Array of input parameter definitions
  - `name`: Parameter name (matches stored procedure parameter)
  - `sqlType`: SQL data type (INTEGER, VARCHAR, DATE, DECIMAL, etc.)
  - `javaType`: Fully qualified Java type
  - `required`: Whether parameter is required
- **outputParameters**: Array of output parameter definitions
- **resultSetMapping**: Column-to-field mapping configuration
  - `strategy`: Must be "EXPLICIT"
  - `columnMappings`: Array of column mappings
    - `column`: Database column name
    - `field`: Java field name
    - `required`: Whether column must be present
  - `unmappedColumnsStrategy`: "IGNORE" (ignore unmapped columns)

## 🔧 Explicit Column Mapping

The framework uses **explicit column-to-field mappings** for clarity and self-documentation.

### Benefits

✅ **Self-Documenting**: Clear which database column maps to which Java field
✅ **No Assumptions**: Doesn't rely on naming conventions
✅ **Better Validation**: Framework validates required columns exist in ResultSet
✅ **Flexible**: Handles any column naming scheme (snake_case, camelCase, PascalCase, etc.)
✅ **Selective Mapping**: Only map the columns you need

### Example

```json
"resultSetMapping": {
  "strategy": "EXPLICIT",
  "columnMappings": [
    {"column": "employee_id", "field": "employeeId", "required": true},
    {"column": "first_name", "field": "firstName", "required": true},
    {"column": "hire_date", "field": "hireDate", "required": true}
  ],
  "unmappedColumnsStrategy": "IGNORE"
}
```

If a required column is missing from the ResultSet, the framework throws a clear error:
```
Required columns not found in ResultSet: [first_name].
Available columns: [employee_id, hire_date, salary]
```

## 📊 Supported SQL Types

| SQL Type | Java Type | Example |
|----------|-----------|---------|
| INTEGER, INT, SMALLINT, TINYINT | java.lang.Integer | 42 |
| BIGINT | java.lang.Long | 9223372036854775807L |
| DECIMAL, NUMERIC | java.math.BigDecimal | new BigDecimal("123.45") |
| VARCHAR, NVARCHAR, CHAR, NCHAR | java.lang.String | "Hello" |
| DATE | java.time.LocalDate | LocalDate.of(2024, 1, 15) |
| DATETIME, TIMESTAMP | java.time.LocalDateTime | LocalDateTime.now() |
| TIME | java.time.LocalTime | LocalTime.of(14, 30) |
| BIT, BOOLEAN | java.lang.Boolean | true |
| FLOAT, REAL | java.lang.Float | 3.14f |
| DOUBLE | java.lang.Double | 3.14159 |

## ⚙️ Metadata Caching

Metadata is cached in memory for performance. The cache is thread-safe.

```java
// Clear entire cache (useful after metadata updates)
reportService.clearMetadataCache();

// Invalidate specific report cache
reportService.invalidateMetadataCache("employee_report");

// Check cache size
int size = reportService.getMetadataCacheSize();
System.out.println("Cached reports: " + size);
```

**When to clear cache:**
- After updating metadata in the database
- After adding new reports
- After schema changes

## 🔍 Testing

### Run Tests

```bash
mvn test
```

### Test Infrastructure

- **H2 In-Memory Database**: Tests use H2 with SQL Server compatibility mode
- **Integration Tests**: Full workflow tests from metadata to result mapping
- **Unit Tests**: Individual component testing

### Example Test

```java
@Test
void testEmployeeReport() {
    Map<String, Object> params = Map.of("DepartmentId", 10);
    ReportResult<EmployeeReport> result = reportService.execute("employee_report", params);

    assertThat(result.getResults()).isNotEmpty();
    assertThat(result.getResults().get(0).getDepartmentId()).isEqualTo(10);
}
```

## 🎯 Design Patterns

- **Open/Closed Principle**: Closed for modification, open for extension via metadata
- **Strategy Pattern**: Pluggable mapping strategies (currently EXPLICIT)
- **Factory Pattern**: ObjectMapperFactory for configured Jackson instances
- **Template Method**: Common execution flow with extensible steps
- **Repository Pattern**: MetadataRepository for metadata access

## 📦 Maven Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.reporting</groupId>
    <artifactId>reporting-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## 📋 Requirements

- **Java**: 17 or higher
- **Database**: SQL Server (or H2 with SQL Server mode for testing)
- **Dependencies**:
  - Jackson 2.17+ (JSON processing and object mapping)
  - SLF4J (logging interface)
  - JDBC driver for your database (e.g., mssql-jdbc for SQL Server)

## 🌐 Spring Boot Integration

```java
@Configuration
public class ReportingConfig {

    @Bean
    public ConnectionProvider connectionProvider(DataSource dataSource) {
        return new DataSourceConnectionProvider(dataSource);
    }

    @Bean
    public ReportService reportService(ConnectionProvider connectionProvider) {
        return new ReportService(connectionProvider);
    }
}

@Service
public class EmployeeService {

    @Autowired
    private ReportService reportService;

    public List<EmployeeReport> getEmployeesByDepartment(Integer deptId) {
        ReportResult<EmployeeReport> result = reportService.execute(
            "employee_report",
            Map.of("DepartmentId", deptId)
        );
        return result.getResults();
    }
}
```

## 🚀 Production Considerations

### Connection Pooling

Use a connection pool like HikariCP:

```java
HikariConfig config = new HikariConfig();
config.setJdbcUrl("jdbc:sqlserver://localhost:1433;databaseName=mydb");
config.setUsername("user");
config.setPassword("password");
config.setMaximumPoolSize(10);

DataSource dataSource = new HikariDataSource(config);
ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
ReportService reportService = new ReportService(provider);
```

### Logging

Configure SLF4J with your preferred logging implementation (Logback, Log4j2):

```xml
<!-- logback.xml -->
<logger name="com.reporting.framework" level="INFO"/>
```

### Error Handling

```java
try {
    ReportResult<EmployeeReport> result = reportService.execute("employee_report", params);
    // Process results
} catch (ReportExecutionException e) {
    logger.error("Report execution failed: {}", e.getMessage(), e);
    // Handle error (return error response, retry, etc.)
}
```

## 📖 Alternative Branches

This repository contains multiple implementation approaches:

| Branch | Approach | Best For |
|--------|----------|----------|
| **main** (current) | POJO-based with explicit mappings | Stable schemas, compile-time type safety |
| **dataset-impl** | Dual-mode: POJO + Dynamic (DataRow/DataSet) | Flexible schemas, runtime transformations |
| **spark-impl** | Apache Spark integration | Big data, distributed processing |

## 📝 License

MIT License

## 🤝 Contributing

Contributions welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Submit a pull request with tests

For bugs or feature requests, please open an issue.
