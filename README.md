# Metadata-Driven Java Reporting Framework

A reusable, metadata-driven framework for executing SQL Server stored procedures and mapping results to Java POJOs or dynamic maps using Jackson ObjectMapper. No report-specific execution code needed!

## 📚 Documentation

- **[APPROACH_COMPARISON.md](APPROACH_COMPARISON.md)** - Compare POJO-based, Lightweight Dynamic, and Spark approaches
- **[DYNAMIC_REPORTING_GUIDE.md](DYNAMIC_REPORTING_GUIDE.md)** - Complete guide to lightweight dynamic reporting
- **[CLAUDE.md](CLAUDE.md)** - Architecture and development guide for Claude Code

## 🌟 Three Approaches Available

This repository provides three distinct approaches to meet different needs:

| Approach | Branch | JAR Size | Best For |
|----------|--------|----------|----------|
| **POJO-Based** | `main` | ~9MB | Type safety, stable schemas |
| **Lightweight Dynamic** ⭐ | `lightweight-dynamic` | ~10MB | Flexible, no POJOs, Tomcat/Spring Boot |
| **Spark-Based** | `spark-impl` | ~300MB | Big data, distributed computing |

**⭐ Recommended:** For most Spring Boot/Tomcat use cases, use `lightweight-dynamic` branch.

See [APPROACH_COMPARISON.md](APPROACH_COMPARISON.md) for detailed comparison and decision guide.

## Features

- **Open/Closed Principle**: Add new reports without modifying framework code
- **Metadata-Driven**: Report configuration stored in database, fetched at runtime
- **Jackson-Based Mapping**: Efficient ResultSet → POJO conversion using ObjectMapper
- **Generic Parameter Binding**: Dynamic parameter mapping with type safety
- **Flexible Input**: Map-based or POJO-based input parameters
- **Naming Strategy Support**: Automatic snake_case ↔ camelCase conversion
- **Output Parameters**: Full support for stored procedure output parameters
- **Metadata Caching**: In-memory caching for performance
- **Type-Safe**: Java 8 date/time types, BigDecimal, and more

## Architecture

```
Client → ReportService → MetadataLoader → Database (Metadata)
                      ↓
         StoredProcedureExecutor → CallableStatement
                      ↓
         ResultSetToMapConverter → List<Map>
                      ↓
         JacksonPojoMapper → List<POJO>
```

## Quick Start

### 1. Setup Database Schema

Execute the metadata schema:

```sql
CREATE TABLE REPORT_METADATA (
    REPORT_NAME VARCHAR(100) PRIMARY KEY,
    STORED_PROCEDURE VARCHAR(200) NOT NULL,
    RESULT_CLASS VARCHAR(500) NOT NULL,
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
    SELECT employee_id, first_name, last_name, salary, hire_date
    FROM employees
    WHERE department_id = @DepartmentId
        AND (@StartDate IS NULL OR hire_date >= @StartDate);

    SELECT @TotalCount = COUNT(*) FROM employees WHERE department_id = @DepartmentId;
END;
```

### 3. Create a POJO

```java
public class EmployeeReport {
    private Integer employeeId;
    private String firstName;
    private String lastName;
    private BigDecimal salary;
    private LocalDate hireDate;
    // getters and setters
}
```

### 4. Insert Metadata

```sql
INSERT INTO REPORT_METADATA (REPORT_NAME, STORED_PROCEDURE, RESULT_CLASS, METADATA_JSON)
VALUES ('employee_report', 'dbo.usp_GetEmployees',
        'com.example.EmployeeReport',
        '{
          "reportName": "employee_report",
          "storedProcedure": "dbo.usp_GetEmployees",
          "resultClass": "com.example.EmployeeReport",
          "inputParameters": [
            {"name": "DepartmentId", "sqlType": "INTEGER", "javaType": "java.lang.Integer", "required": true},
            {"name": "StartDate", "sqlType": "DATE", "javaType": "java.time.LocalDate", "required": false}
          ],
          "outputParameters": [
            {"name": "TotalCount", "sqlType": "INTEGER", "javaType": "java.lang.Integer"}
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
        }');
```

### 5. Execute the Report

**Option 1: Map-based parameters**
```java
// Setup
DataSource dataSource = ... // HikariCP or other
ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
ReportService reportService = new ReportService(provider);

// Execute
Map<String, Object> params = new HashMap<>();
params.put("DepartmentId", 10);
params.put("StartDate", LocalDate.of(2020, 1, 1));

ReportResult<EmployeeReport> result = reportService.execute("employee_report", params);

// Access results
List<EmployeeReport> employees = result.getResults();
Integer totalCount = (Integer) result.getOutputParameter("TotalCount");
```

**Option 2: POJO-based parameters** (type-safe)
```java
// Create input POJO
public class EmployeeReportRequest {
    private Integer departmentId;
    private LocalDate startDate;
    // getters/setters
}

// Execute with POJO
EmployeeReportRequest request = new EmployeeReportRequest();
request.setDepartmentId(10);
request.setStartDate(LocalDate.of(2020, 1, 1));

ReportResult<EmployeeReport> result = reportService.execute("employee_report", request);
```

## Adding a New Report

Follow these 3 steps (no framework code changes!):

1. **Create stored procedure** in SQL Server
2. **Create POJO** class with matching fields
3. **Insert metadata** into REPORT_METADATA table

That's it! The framework handles everything else.

## Metadata JSON Structure

```json
{
  "reportName": "report_name",
  "storedProcedure": "dbo.usp_ProcedureName",
  "resultClass": "com.example.ResultPojo",
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
  "resultSetMapping": {
    "strategy": "EXPLICIT",
    "columnMappings": [
      {"column": "database_column", "field": "javaField", "required": true},
      {"column": "employee_id", "field": "employeeId", "required": true},
      {"column": "first_name", "field": "firstName", "required": true}
    ],
    "unmappedColumnsStrategy": "IGNORE"
  }
}
```

## Supported SQL Types

| SQL Type | Java Type |
|----------|-----------|
| INTEGER, INT | java.lang.Integer |
| BIGINT | java.lang.Long |
| DECIMAL, NUMERIC | java.math.BigDecimal |
| VARCHAR, NVARCHAR | java.lang.String |
| DATE | java.time.LocalDate |
| DATETIME, TIMESTAMP | java.time.LocalDateTime |
| BIT, BOOLEAN | java.lang.Boolean |
| FLOAT, REAL | java.lang.Float |
| DOUBLE | java.lang.Double |

## Column Mapping

The framework uses **explicit column-to-field mappings** for clarity and self-documentation:

```json
"resultSetMapping": {
  "strategy": "EXPLICIT",
  "columnMappings": [
    {"column": "employee_id", "field": "employeeId", "required": true},
    {"column": "first_name", "field": "firstName", "required": true},
    {"column": "hire_date", "field": "hireDate", "required": true}
  ]
}
```

**Benefits:**
- Self-documenting - Clear which DB column maps to which field
- No assumptions - Doesn't rely on naming conventions
- Better validation - Framework validates required columns exist
- Flexible - Handles any column naming scheme
- Selective mapping - Only map the columns you need

## Metadata Caching

Metadata is cached in memory for performance:

```java
// Clear entire cache
reportService.clearMetadataCache();

// Invalidate specific report
reportService.invalidateMetadataCache("employee_report");

// Check cache size
int size = reportService.getMetadataCacheSize();
```

## Maven Dependency

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.reporting</groupId>
    <artifactId>reporting-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Requirements

- Java 17+
- SQL Server (or H2 with SQL Server mode for testing)
- Jackson 2.17+
- SLF4J for logging

## Testing

Run integration tests:

```bash
mvn test
```

Integration tests use H2 in-memory database with SQL Server compatibility mode.

## Architecture Highlights

### Layered Design
- **API Layer**: `ReportService` - main entry point
- **Execution Layer**: `StoredProcedureExecutor` - handles CallableStatement
- **Mapping Layer**: Jackson-based converters (ResultSet → Map → POJO)
- **Configuration Layer**: `MetadataLoader` with caching
- **Connection Layer**: `ConnectionProvider` abstraction

### Key Components

- **ReportService**: Main API for executing reports
- **MetadataLoader**: Loads and caches metadata from database
- **StoredProcedureExecutor**: Executes stored procedures with dynamic parameter binding
- **GenericParameterBinder**: Maps Java types to SQL types
- **ResultSetToMapConverter**: Converts ResultSet to List<Map>
- **JacksonPojoMapper**: Converts Map to POJO using `ObjectMapper.convertValue()`
- **PojoToParameterConverter**: Converts input POJO to Map

### Design Patterns

- **Open/Closed Principle**: Closed for modification, open for extension
- **Strategy Pattern**: Pluggable mapping strategies
- **Factory Pattern**: ObjectMapperFactory for configured instances
- **Template Method**: Common execution flow with extensible steps

## License

MIT License

## Contributing

Contributions welcome! Please submit a pull request or open an issue.
