# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A metadata-driven Java framework for executing SQL Server stored procedures and mapping results to POJOs. The core principle: **add new reports without modifying framework code** (Open/Closed Principle).

Reports are defined entirely through database metadata (JSON), eliminating the need for report-specific execution code.

## Build and Test Commands

```bash
# Build the project
mvn clean compile

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ResultSetToMapConverterTest

# Run a specific test method
mvn test -Dtest=ResultSetToMapConverterTest#testExplicitMappingWithValidColumns

# Package the JAR
mvn package

# Skip tests during build
mvn clean install -DskipTests
```

**Testing Notes:**
- Integration tests (`src/test/java/com/reporting/framework/integration/`) are disabled by default due to H2 stored procedure limitations
- Full integration testing requires SQL Server
- Unit tests cover all core mapping, conversion, and validation logic

## High-Level Architecture

The framework follows a **layered architecture** with data flowing through five distinct layers:

### Execution Flow

```
Client Code
    ↓
┌───────────────────────────────────────────────────────────────┐
│ API Layer: ReportService                                       │
│ - Entry point for report execution                             │
│ - Handles metadata caching                                     │
│ - Manages overall execution lifecycle                          │
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
│ Mapping Layer (Input): PojoToParameterConverter               │
│ - Converts input POJO → Map<String, Object>                   │
│ - Enables type-safe parameter input                            │
└───────────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────────┐
│ Execution Layer: StoredProcedureExecutor                       │
│ - Builds dynamic CallableStatement: {call proc(?, ?)}         │
│ - GenericParameterBinder: Java types → SQL types              │
│ - Handles input parameters and output parameter registration   │
│ - Manages database resources                                   │
└───────────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────────┐
│ Mapping Layer (Output): Two-Stage Conversion                  │
│                                                                 │
│ Stage 1: ResultSetToMapConverter                               │
│ - ResultSet → List<Map<String, Object>>                       │
│ - Uses EXPLICIT column mappings from metadata                  │
│ - Validates required columns exist                             │
│ - Case-insensitive column matching                             │
│                                                                 │
│ Stage 2: JacksonPojoMapper                                     │
│ - List<Map> → List<POJO>                                       │
│ - Uses ObjectMapper.convertValue() for efficiency             │
│ - Handles Java 8 date/time types via JavaTimeModule           │
└───────────────────────────────────────────────────────────────┘
    ↓
ReportResult<T> (typed result list + output parameters)
```

### Key Architectural Patterns

**Two-Stage Mapping Strategy:**
The framework uses ResultSet → Map → POJO instead of direct ResultSet → POJO. This separation allows:
- ResultSetToMapConverter handles SQL-specific logic (column mapping, SQL types)
- JacksonPojoMapper handles POJO serialization (naming strategies, type conversion)
- Clear separation of concerns
- Easier testing and maintenance

**Explicit Column Mapping:**
All column-to-field mappings are defined explicitly in metadata JSON, not derived from naming conventions. This provides:
- Self-documenting mappings
- Upfront validation (missing columns detected before processing)
- Better error messages listing actual column names
- Support for any database naming scheme

**Connection Provider Abstraction:**
`ConnectionProvider` interface allows pluggable connection strategies:
- `DataSourceConnectionProvider` - for connection pools (HikariCP)
- `SimpleConnectionProvider` - for direct JDBC URLs
- Enables testing with different connection sources

## Adding a New Report (Three Steps)

No framework code changes needed! Define everything through metadata:

### 1. Create Stored Procedure (SQL Server)
```sql
CREATE PROCEDURE dbo.usp_YourReport
    @Param1 INT,
    @Param2 DATE = NULL,
    @OutputParam INT OUTPUT
AS
BEGIN
    SELECT column1, column2, column3
    FROM your_table
    WHERE condition = @Param1;

    SELECT @OutputParam = COUNT(*) FROM your_table;
END;
```

### 2. Create POJO Class
```java
public class YourReport {
    private Integer field1;
    private String field2;
    private LocalDate field3;
    // getters and setters
}
```

### 3. Insert Metadata
```sql
INSERT INTO REPORT_METADATA (REPORT_NAME, STORED_PROCEDURE, RESULT_CLASS, METADATA_JSON)
VALUES ('your_report', 'dbo.usp_YourReport', 'com.example.YourReport',
'{
  "reportName": "your_report",
  "storedProcedure": "dbo.usp_YourReport",
  "resultClass": "com.example.YourReport",
  "inputParameters": [
    {"name": "Param1", "sqlType": "INTEGER", "javaType": "java.lang.Integer", "required": true},
    {"name": "Param2", "sqlType": "DATE", "javaType": "java.time.LocalDate", "required": false}
  ],
  "outputParameters": [
    {"name": "OutputParam", "sqlType": "INTEGER", "javaType": "java.lang.Integer"}
  ],
  "resultSetMapping": {
    "strategy": "EXPLICIT",
    "columnMappings": [
      {"column": "column1", "field": "field1", "required": true},
      {"column": "column2", "field": "field2", "required": true},
      {"column": "column3", "field": "field3", "required": false}
    ],
    "unmappedColumnsStrategy": "IGNORE"
  }
}');
```

Then execute: `reportService.execute("your_report", parameters)`

## Important Implementation Details

### Metadata Caching
Metadata is loaded once and cached in ConcurrentHashMap. Methods available:
```java
reportService.clearMetadataCache();              // Clear all
reportService.invalidateMetadataCache("report");  // Clear one
int size = reportService.getMetadataCacheSize(); // Check size
```

### SQL Type Mapping (GenericParameterBinder)
Core type mappings in `GenericParameterBinder.java`:
- `java.lang.Integer` → `Types.INTEGER`
- `java.lang.Long` → `Types.BIGINT`
- `java.math.BigDecimal` → `Types.DECIMAL`
- `java.lang.String` → `Types.VARCHAR`
- `java.time.LocalDate` → `Types.DATE`
- `java.time.LocalDateTime` → `Types.TIMESTAMP`
- `java.lang.Boolean` → `Types.BIT`

### ObjectMapper Configuration
`ObjectMapperFactory` provides a pre-configured Jackson ObjectMapper:
- `JavaTimeModule` registered for Java 8 date/time types
- `PropertyNamingStrategies.SNAKE_CASE` for database compatibility
- `FAIL_ON_UNKNOWN_PROPERTIES = false` for flexible mapping

### Error Handling Strategy
- `ReportExecutionException` - runtime exception wrapping execution errors
- `MetadataException` - metadata loading/parsing errors
- Both provide detailed context (report name, parameters, stack traces)

### Resource Management
`StoredProcedureExecutor` uses try-with-resources for:
- Connection (provided by ConnectionProvider)
- CallableStatement
- ResultSet
All resources are automatically closed, even on exceptions.

## Package Structure and Responsibilities

```
com.reporting.framework/
├── api/                  - Public API surface
│   ├── ReportService     - Main entry point
│   └── ReportResult      - Typed result wrapper
│
├── executor/             - Stored procedure execution
│   ├── StoredProcedureExecutor      - CallableStatement management
│   ├── GenericParameterBinder       - Parameter type mapping
│   └── ExecutionResult              - Raw execution results
│
├── mapper/               - Data transformation
│   ├── ResultSetToMapConverter      - ResultSet → Map (explicit mappings)
│   ├── JacksonPojoMapper            - Map → POJO (Jackson)
│   ├── PojoToParameterConverter     - POJO → Map (input params)
│   └── ObjectMapperFactory          - Configured Jackson instance
│
├── metadata/             - Configuration and metadata
│   ├── MetadataLoader               - Load + cache metadata
│   ├── MetadataRepository           - Database access
│   ├── ReportMetadata               - Report configuration model
│   ├── ParameterMetadata            - Parameter definition
│   ├── ResultSetMapping             - Mapping strategy
│   └── ColumnMapping                - Column-to-field mapping
│
├── connection/           - Database connection abstraction
│   ├── ConnectionProvider           - Interface
│   ├── DataSourceConnectionProvider - For connection pools
│   └── SimpleConnectionProvider     - For direct JDBC URLs
│
└── exception/            - Framework exceptions
    ├── ReportExecutionException
    └── MetadataException
```

## Testing Strategy

### Unit Tests (`src/test/java/com/reporting/framework/unit/`)
Each mapper component has comprehensive unit tests:
- **ResultSetToMapConverterTest** (7 tests) - Explicit mapping, validation, null handling
- **JacksonPojoMapperTest** (5 tests) - POJO conversion, empty lists, unknown properties
- **PojoToParameterConverterTest** (4 tests) - Input POJO conversion, null handling

### Integration Tests (`src/test/java/com/reporting/framework/integration/`)
Currently disabled - require SQL Server for full stored procedure testing.

### Example POJOs (`src/test/java/com/reporting/framework/example/`)
Demonstrate usage patterns:
- `EmployeeReport` - snake_case columns, BigDecimal, LocalDate
- `SalesReport` - output parameters
- `EmployeeReportRequest` - type-safe input parameters

## Common Development Scenarios

### Modifying Metadata Schema
If changing `REPORT_METADATA` table structure or JSON schema:
1. Update `src/main/resources/schema/metadata_schema.sql`
2. Update `ReportMetadata`, `ParameterMetadata`, `ResultSetMapping` model classes
3. Update `MetadataLoader.loadMetadata()` JSON parsing
4. Update README.md examples

### Adding SQL Type Support
To support a new SQL type:
1. Add mapping in `GenericParameterBinder.getSqlType(Class<?>)`
2. Add reverse mapping in `GenericParameterBinder.convertOutputParameter()`
3. Add to supported types table in README.md
4. Add test case in unit tests

### Modifying Column Mapping Logic
All column mapping logic is in `ResultSetToMapConverter.convert()`:
- Uses explicit mappings from `ResultSetMapping.getColumnMappings()`
- Validates required columns exist (throws exception if missing)
- Case-insensitive column matching via `ResultSetMetaData.findColumn()`
- Builds Map with POJO field names as keys

### Spring Boot Integration
Framework is designed to work with Spring Boot:
```java
@Bean
public ReportService reportService(DataSource dataSource) {
    ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
    return new ReportService(provider);
}
```
See `USAGE_EXAMPLE.md` for complete Spring integration examples.

## Performance Considerations

- Metadata is cached after first load (avoid repeated DB queries)
- Jackson's `convertValue()` is more efficient than serialize/deserialize
- Connection pooling recommended (HikariCP) via `DataSourceConnectionProvider`
- ResultSet → Map conversion is single-pass (no multiple iterations)
- Explicit column mapping avoids reflection overhead for column discovery
