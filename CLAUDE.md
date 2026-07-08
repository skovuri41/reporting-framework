# CLAUDE.md

> **⚠️ INTERNAL USE ONLY**
> This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
> This is NOT user-facing documentation. See README.md for public documentation.

## Project Overview

A metadata-driven Java reporting framework for executing SQL Server stored procedures with two execution modes:
- **POJO Mode**: Compile-time type safety with explicit class mappings
- **Dynamic Mode**: Runtime flexibility with fluent transformations (the focus of current development)

**Current Branch:** `dataset-impl` - Implements the DataSet abstraction layer with fluent DSL for transformations.

## Build & Test Commands

### Maven Build
```bash
# Full build
mvn clean install

# Compile only
mvn compile

# Run all tests (expect 108+ tests)
mvn test

# Run specific test class
mvn test -Dtest=FullWorkflowIntegrationTest

# Run single test method
mvn test -Dtest=FullWorkflowIntegrationTest#testEmployeeSalaryAnalysis

# View test results
cat target/surefire-reports/*.txt
```

### Requirements
- **Java 17+** (set in pom.xml)
- **Maven 3.6+**
- **H2 1.4.200** for testing (not 2.x - see Known Limitations)

## Architecture Overview

### Execution Flow (Dynamic Mode)

```
Client
  ↓
ReportService.execute(reportName, params)
  ↓
MetadataLoader → Database REPORT_METADATA table
  ↓
StoredProcedureExecutor → CallableStatement → Stored Procedure
  ↓
ResultSetToMapConverter → List<Map<String, Object>>
  ↓
DataRow.of() → List<DataRow>
  ↓
DataSet(rows, outputParams, reportName)
  ↓
DataQuery/DataOperations → Transformations
  ↓
DataSet → JSON output
```

### Core Components

**Entry Point:**
- `ReportService` - Main API, executes reports and returns DataSet

**Data Layer (Dynamic Mode - Current Focus):**
- `DataSet` - Immutable collection of DataRows with metadata
- `DataRow` - Type-safe wrapper over Map<String, Object> with getters (getString, getInt, getBigDecimal, etc.)
- `DataQuery` - Fluent DSL builder for transformations (filter, select, orderBy, groupBy, withColumn, etc.)
- `DataOperations` - Static methods for complex operations (joins, unions, pivots, aggregations)

**Metadata Model:**
- `ReportMetadata` - Immutable report definition (reportId, reportName, description, datasets)
- `Dataset` - Immutable dataset definition (datasource, parameters, columns)
- `Parameter` - Input/Output parameter metadata with automatic camelCase transformation
- `Column` - Column metadata (sourceColumn preserved, displayName transformed to camelCase)
- `ReportCatalog` - Immutable catalog of all available reports

**Metadata & Execution:**
- `MetadataLoader` - Loads report metadata from REPORT_METADATA table (with in-memory caching)
- `MetadataRepository` - Fetches and validates JSON metadata, applies naming transformations
- `JsonSchemaValidator` - Validates metadata JSON against JSON Schema Draft 7
- `NamingConverter` - Transforms snake_case/PascalCase/UPPER_SNAKE_CASE to camelCase
- `StoredProcedureExecutor` - Executes stored procedures via CallableStatement with Dataset metadata
- `ResultSetToMapConverter` - Converts ResultSet to List<Map<String, Object>>

**Connection Management:**
- `ConnectionProvider` - Interface for database connections
- `DataSourceConnectionProvider` - Production implementation (HikariCP, etc.)
- `SimpleConnectionProvider` - Test implementation

### Package Structure

```
com.reporting.framework/
├── api/                     # Public API (ReportService)
├── data/                    # DataSet, DataRow, DataQuery, DataOperations
├── metadata/                # Metadata loading and caching
├── executor/                # Stored procedure execution
├── mapper/                  # ResultSet to Map conversion
├── connection/              # Connection providers
└── exception/               # Framework exceptions
```

## Metadata System Architecture (Phase 2)

### Metadata Loading Flow

```
Client calls service.execute(reportId, params)
  ↓
MetadataLoader.load(reportId)
  ↓
Check in-memory cache (ConcurrentHashMap)
  ↓ (cache miss)
MetadataRepository.getReportMetadata(reportId)
  ↓
Load METADATA_JSON from database
  ↓
JsonSchemaValidator.validate(reportId, json)
  ↓
Jackson deserializes JSON → ReportMetadata
  ↓
Apply naming transformations:
  - Parameters: parameterName → camelCase
  - Columns: displayName → camelCase (sourceColumn preserved)
  ↓
Create immutable DTOs (defensive copy of lists)
  ↓
Store in cache
  ↓
Return to caller
```

### Naming Transformation Rules

**NamingConverter.toCamelCase()** automatically transforms:

| Input Format | Example Input | Output | Notes |
|--------------|---------------|--------|-------|
| snake_case | `department_id` | `departmentId` | Common in SQL |
| UPPER_SNAKE_CASE | `EMPLOYEE_COUNT` | `employeeCount` | SQL constants |
| PascalCase | `StartDate` | `startDate` | C# conventions |
| Single uppercase | `X` | `x` | Edge case |
| Already camelCase | `employeeId` | `employeeId` | No change |

**Important:**
- Transformation happens AFTER Jackson deserialization
- `Column.sourceColumn` is **NOT transformed** (preserves exact SQL column name)
- `Column.displayName` IS transformed (client-facing name)
- `Parameter.parameterName` IS transformed (used in parameter maps)

### Immutability Guarantees

All metadata DTOs are immutable:
- Constructor-only initialization with `@JsonCreator`
- Lists wrapped with `Collections.unmodifiableList()` (defensive copy)
- No setters
- All fields `final`

Example:
```java
ReportMetadata metadata = metadataLoader.load("report-001");
metadata.getDatasets().clear();  // Throws UnsupportedOperationException
```

### Performance Characteristics

**Caching Strategy:**
- In-memory cache using `ConcurrentHashMap<String, ReportMetadata>`
- Thread-safe for concurrent access
- No TTL/expiration (manual refresh via `refreshCache()`)
- Catalog loads ALL reports in single SQL query

**Benchmarks (H2 in-memory):**
- Single report first load: ~10-20ms (includes validation + transformation)
- Single report cached load: <1ms
- Catalog load (100 reports): ~82ms (target: <1000ms)
- Cache lookup: O(1) hash map access

## Key Design Patterns

### Immutability
All transformations return **new instances**:
```java
DataSet filtered = DataQuery.from(original).filter(...).execute();  // original unchanged
DataRow modified = row.with("newCol", value);  // row unchanged
```

### Type-Safe Access (No Casting)
```java
DataRow row = dataSet.first();
String name = row.getString("NAME");           // Not: (String) row.get("NAME")
Integer id = row.getInt("EMPLOYEE_ID");        // Not: (Integer) row.get("EMPLOYEE_ID")
BigDecimal salary = row.getBigDecimal("SALARY");
LocalDate hired = row.getLocalDate("HIRE_DATE");
```

### Fluent DSL
```java
DataSet result = DataQuery.from(employees)
    .filter(row -> row.getInt("SALARY") > 80000)
    .select("NAME", "SALARY", "DEPARTMENT")
    .orderBy("SALARY").desc()
    .limit(10)
    .execute();
```

### Execute Order (DataQuery)
Transformations execute in this order regardless of chaining:
1. Filters
2. Computed columns (withColumn)
3. Select (column projection)
4. OrderBy
5. Distinct
6. Limit
7. GroupBy with aggregations

## Known Limitations (IMPORTANT)

### 1. H2 Stored Procedure Limitation
**Issue:** H2 doesn't support callable stored procedures.
- H2's `CREATE ALIAS` creates functions (SELECT syntax), not procedures (CALL syntax)
- Framework uses `CallableStatement` with `{call procedure_name}` which is incompatible

**Impact:**
- Tests use helper method `createDataSetFromQuery(sql, datasetName)` to bypass stored procedures
- Integration tests create DataSets directly from SQL queries
- Metadata layer + stored procedure integration is **not tested with H2**

**To test fully:** Use SQL Server (not H2)

### 2. Multiple Aggregations on Same Column
**Issue:** GroupByBuilder uses `Map<String, AggregationFunction>` - multiple aggregations on same column overwrite.

```java
// ❌ BROKEN - sum overwrites avg
.groupBy("DEPT").avg("SALARY").sum("SALARY")

// ✅ WORKAROUND - use different columns
.groupBy("DEPT").sum("SALARY").count("EMPLOYEE_ID")
```

**Root Cause:** Map key is column name, so duplicate keys overwrite.

**Future Fix:** Change to `List<Pair<String, AggregationFunction>>` in:
- `DataQuery.GroupByBuilder`
- `DataOperations.groupByWithAggregations()`

### 3. Computed Column Dependencies
**Issue:** Computed columns can't reference other computed columns in same `execute()`.

```java
// ❌ BROKEN - TOTAL_COMP can't see BONUS
DataQuery.from(ds)
    .withColumn("BONUS", row -> row.getBigDecimal("SALARY").multiply(0.1))
    .withColumn("TOTAL_COMP", row -> row.getBigDecimal("BONUS"))  // BONUS is null!
    .execute()

// ✅ WORKAROUND - split into multiple execute() calls
DataSet step1 = DataQuery.from(ds).withColumn("BONUS", ...).execute();
DataSet step2 = DataQuery.from(step1).withColumn("TOTAL_COMP", ...).execute();
```

**Root Cause:** In `DataQuery.execute()`, all computed columns apply to **original row**, not intermediate results.

**Future Fix:** Change `apply(row)` to `apply(newRow)` in DataQuery.execute() around line 154.

## Test Suite Structure

### Integration Tests (Primary)
- `FullWorkflowIntegrationTest` - **Main test** (4 comprehensive scenarios, all passing)
  - Uses test data: 7 employees, 3 departments, 9 sales records
  - Tests: joins, filters, aggregations, computed columns, complex workflows

### Unit Tests
- `DataSetTest` - DataSet and DataQuery functionality
- `DataOperationsTest` - Joins, unions, pivots
- `ResultSetToMapConverterTest` - ResultSet conversion

### Test Data (FullWorkflowIntegrationTest)
```sql
employees (employee_id, name, department_id, salary, hire_date)
  - 7 employees across 3 departments

departments (department_id, department_name, budget)
  - Engineering (10), Sales (20), Marketing (30)

sales (sale_id, employee_id, sale_amount, sale_month)
  - 9 Q1 2024 sales records for sales employees
```

## Common Patterns

### Aggregation Column Naming
After aggregation, columns follow pattern: `{COLUMN}_{function}`
```java
.groupBy("DEPT").sum("SALARY").count("ID")
// Creates columns: DEPT, SALARY_sum, ID_count
```

### Debugging DataSet Contents
```java
DataRow row = dataSet.first();
System.out.println("Columns: " + row.keys());
System.out.println("Row: " + row);
```

### Join Pattern
```java
DataSet employees = reportService.execute("employees", params);
DataSet departments = reportService.execute("departments", params);

DataSet joined = DataOperations.innerJoin(
    employees, departments,
    "DEPARTMENT_ID", "DEPARTMENT_ID"
);
```

### Complex Multi-Step Workflow
```java
// 1. Fetch and filter
DataSet highEarners = DataQuery.from(employees)
    .filter(row -> row.getBigDecimal("SALARY").compareTo(BigDecimal.valueOf(85000)) >= 0)
    .execute();

// 2. Join
DataSet enriched = DataOperations.innerJoin(highEarners, departments, "DEPT_ID", "DEPT_ID");

// 3. Transform
DataSet result = DataQuery.from(enriched)
    .select("NAME", "SALARY", "DEPARTMENT_NAME")
    .withColumn("BONUS", row -> row.getBigDecimal("SALARY").multiply(BigDecimal.valueOf(0.1)))
    .orderBy("SALARY").desc()
    .execute();

// 4. Export
String json = result.toPrettyJSON();
```

## Metadata-Driven Configuration

### New JSON Metadata Structure (Phase 2)

Reports are configured via JSON metadata stored in the `REPORT_METADATA` table. The JSON structure is validated against JSON Schema Draft 7 and automatically transformed to camelCase naming conventions.

#### Database Schema

```sql
CREATE TABLE REPORT_METADATA (
    REPORT_ID VARCHAR(100) PRIMARY KEY,
    REPORT_NAME VARCHAR(255) NOT NULL,
    REPORT_DESCRIPTION VARCHAR(1000),
    METADATA_JSON NVARCHAR(MAX) NOT NULL,  -- JSON structure below
    CREATED_DATE DATETIME DEFAULT CURRENT_TIMESTAMP,
    MODIFIED_DATE DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

#### JSON Metadata Structure

```json
{
  "reportId": "employee-report-001",
  "reportName": "Employee Report",
  "reportDescription": "Lists employees with filters",
  "datasets": [
    {
      "datasource": "dbo.usp_GetEmployees",
      "datasourceId": "ds-employees",
      "datasourceType": "StoredProc",
      "datasourceDescription": "Fetches employee data",
      "parameters": [
        {
          "parameterName": "department_id",
          "dataType": "INTEGER",
          "parameterDirection": "Input",
          "nullable": true
        },
        {
          "parameterName": "EMPLOYEE_COUNT",
          "dataType": "INTEGER",
          "parameterDirection": "Output",
          "nullable": false
        }
      ],
      "columns": [
        {
          "sourceColumn": "employee_id",
          "displayName": "employee_id",
          "dataType": "INTEGER",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "full_name",
          "displayName": "FULL_NAME",
          "dataType": "VARCHAR",
          "sortable": true,
          "groupable": false,
          "filterable": true
        }
      ]
    }
  ]
}
```

#### Automatic Naming Transformation

The framework **automatically transforms** parameter and column names to camelCase after loading:

```
Input JSON:                     After Transformation:
─────────────────────────────  ──────────────────────────────
department_id        (snake)  → departmentId      (camelCase)
EMPLOYEE_COUNT       (UPPER)  → employeeCount     (camelCase)
StartDate          (Pascal)  → startDate         (camelCase)
employee_id        (snake)  → employeeId        (camelCase)
FULL_NAME          (UPPER)  → fullName          (camelCase)
DepartmentName     (Pascal)  → departmentName    (camelCase)
```

**Important:**
- `sourceColumn` is **preserved exactly** (NOT transformed) - used for SQL column mapping
- `displayName` is **transformed to camelCase** - used for client-facing display
- `parameterName` is **transformed to camelCase** - used in parameter maps

#### Loading Metadata

```java
// Single report
ReportService service = new ReportService(connectionProvider);
ReportMetadata metadata = service.getMetadata("employee-report-001");

// Report catalog (all reports)
ReportCatalog catalog = service.getCatalog();
List<ReportSummary> reports = catalog.getReports();

// Execute with transformed parameter names
Map<String, Object> params = Map.of("departmentId", 10);  // Use camelCase!
DataSet result = service.execute("employee-report-001", params);
```

#### JSON Schema Validation

All metadata JSON is validated against JSON Schema Draft 7 on load. Validation failures throw `MetadataValidationException` with detailed error messages.

Schema location: `src/main/resources/schemas/report-metadata-schema.json`

#### Performance Targets

- Single report load: < 100ms (actual: ~4ms with caching)
- Catalog load (100 reports): < 1000ms (actual: ~82ms)
- In-memory caching with `ConcurrentHashMap` (thread-safe)

## Dependencies

### Core Runtime
- Jackson 2.17.2 (JSON serialization/deserialization)
- JSON Schema Validator 1.5.1 (networknt/json-schema-validator - JSON Schema Draft 7 validation)
- SLF4J 2.0.13 + Logback (logging)
- SQL Server JDBC Driver 12.8.1
- Google Guava 33.0.0 (utilities, used for CaseFormat in NamingConverter)

### Testing
- JUnit Jupiter 5.10.3
- Mockito 5.12.0
- H2 1.4.200 (in-memory database - **must be 1.4.x, not 2.x**)
- AssertJ 3.26.0

## Future Work (See PHASE3_PLANNING.md)

Planning documents exist for hierarchical JSON mapping (Phase 3):
- Transform flat DataSets into nested JSON structures (departments → employees arrays)
- Three-phase approach: field mapping (Phase 1) → nested objects (Phase 2) → hierarchical arrays (Phase 3)
- **Recommendation:** Start with Phase 1 (simple field renaming) for quick value

## Additional Documentation

- `DEVELOPER_GUIDE.md` - Comprehensive guide with quick start, technical context, known issues, session history
- `PHASE3_PLANNING.md` - Future hierarchical JSON mapping architecture
- `README.md` - Project overview and usage examples
- `TRANSFORMATION_ARCHITECTURE_RESEARCH.md` - Research on transformation architectures (future work)

## Git Workflow

**Main Branch:** `main`
**Current Development Branch:** `dataset-impl`

When making commits, follow existing patterns seen in recent commits:
- Descriptive messages focused on what changed
- Include co-author attribution when working with Claude

## Important File Locations

### Configuration
- `src/test/resources/logback-test.xml` - Test logging config
- `src/test/resources/test-schema.sql` - H2 test schema

### Key Implementation Files

**API & Entry Points:**
- `src/main/java/com/reporting/framework/api/ReportService.java` - Main entry point

**Data Layer:**
- `src/main/java/com/reporting/framework/data/DataSet.java` - Core data container
- `src/main/java/com/reporting/framework/data/DataQuery.java` - Fluent DSL builder
- `src/main/java/com/reporting/framework/data/DataOperations.java` - Complex operations

**Metadata Model (Phase 2):**
- `src/main/java/com/reporting/framework/metadata/model/ReportMetadata.java` - Report definition
- `src/main/java/com/reporting/framework/metadata/model/Dataset.java` - Dataset definition
- `src/main/java/com/reporting/framework/metadata/model/Parameter.java` - Parameter metadata
- `src/main/java/com/reporting/framework/metadata/model/Column.java` - Column metadata
- `src/main/java/com/reporting/framework/metadata/model/ReportCatalog.java` - Report catalog

**Metadata Loading & Validation:**
- `src/main/java/com/reporting/framework/metadata/MetadataLoader.java` - Metadata caching and loading
- `src/main/java/com/reporting/framework/metadata/MetadataRepository.java` - Database access and transformation
- `src/main/java/com/reporting/framework/metadata/JsonSchemaValidator.java` - JSON Schema validation
- `src/main/java/com/reporting/framework/metadata/util/NamingConverter.java` - Naming transformation
- `src/main/resources/schemas/report-metadata-schema.json` - JSON Schema definition

**Execution:**
- `src/main/java/com/reporting/framework/executor/StoredProcedureExecutor.java` - Stored procedure execution
- `src/main/java/com/reporting/framework/mapper/ResultSetToMapConverter.java` - ResultSet conversion

### Tests to Run When Validating Changes

**Primary Integration Tests:**
- `FullWorkflowIntegrationTest` - End-to-end workflow tests (run this first)
- `EndToEndMetadataTest` - Complete metadata flow validation (database → cache → retrieve)

**Metadata Tests:**
- `MetadataIntegrationTest` - Metadata loading and transformation
- `MetadataPerformanceTest` - Performance benchmarks (<100ms single, <1s catalog)
- `JsonSchemaValidatorTest` - JSON Schema validation
- `NamingConverterTest` - Naming transformation (snake_case, PascalCase, UPPER_SNAKE_CASE)

**Unit Tests:**
- `DataSetTest` - DataSet/DataQuery unit tests
- `DataOperationsTest` - Joins/aggregations tests
