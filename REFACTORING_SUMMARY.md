# Refactoring Summary: Report Metadata → Stored Procedure Metadata

## Overview

This refactoring transforms the framework from a "report-centric" to a "stored procedure-centric" architecture, with support for:
- Multiple stored procedures per business report
- Metadata-driven parameter filtering
- Automatic column name mapping (SQL → camelCase)
- Cache warming on startup
- Improved error handling

## Key Changes

### 1. Renamed Core Classes

| Old Name | New Name | Purpose |
|----------|----------|---------|
| `ReportMetadata` | `StoredProcedureMetadata` | Clearer semantics |
| `reportName` | `procedureId` | Identifier for stored proc |
| `storedProcedure` | `procedureName` | Actual SQL procedure name |

### 2. New Metadata Structure

**ParameterMetadata** (Enhanced):
```json
{
  "name": "DepartmentId",
  "fieldName": "departmentId",
  "type": "INTEGER",
  "nullable": false
}
```

**ColumnMetadata** (New):
```json
{
  "columnName": "EMPLOYEE_ID",
  "fieldName": "employeeId",
  "columnType": "INTEGER"
}
```

**StoredProcedureMetadata**:
```json
{
  "procedureId": "emp_001",
  "procedureName": "usp_GetEmployees",
  "description": "Fetch employee data",
  "parameters": [...],
  "columns": [...],
  "outputParameters": [...]
}
```

### 3. Metadata Stored Procedure

Old approach: SELECT from REPORT_METADATA table
New approach: Call `usp_GetProcedureMetadata` with OUT parameter

```sql
CREATE PROCEDURE usp_GetProcedureMetadata
    @ProcedureId VARCHAR(100) = NULL,
    @MetadataJson NVARCHAR(MAX) OUTPUT
AS
BEGIN
    -- If @ProcedureId is NULL, return all procedures (for cache warming)
    -- Otherwise return specific procedure metadata
END;
```

### 4. Parameter Filtering

New `ParameterFilter` class validates and filters parameters:

```java
// Service receives superset
Map<String, Object> allParams = Map.of(
    "departmentId", 10,
    "minSalary", 50000,
    "region", "US",      // Filtered out
    "unused", "value"    // Filtered out
);

// Framework automatically filters
Map<String, Object> filtered = parameterFilter.filterParameters(allParams, metadata);
// Result: {departmentId=10, minSalary=50000}
```

### 5. camelCase Field Names

**Old (SQL column names):**
```java
DataRow row = dataSet.first();
Integer id = row.getInt("EMPLOYEE_ID");
String name = row.getString("FIRST_NAME");
```

**New (camelCase throughout):**
```java
DataRow row = dataSet.first();
Integer id = row.getInt("employeeId");
String name = row.getString("firstName");
```

### 6. Cache Warming

Metadata cache now warms on startup:

```java
public MetadataLoader(ConnectionProvider provider, boolean cacheEnabled) {
    // ...
    if (cacheEnabled) {
        warmCache();  // Loads all procedures on startup
    }
}
```

Fallback: If cache warming fails or metadata not found, lazy loads on-demand.

### 7. New Exception Classes

- `MetadataParseException` - JSON parsing failures
- `MetadataNotFoundException` - Procedure metadata not found
- `MissingParameterException` - Required parameter missing/null

## Files Modified

### Core Framework
- `StoredProcedureMetadata.java` (new)
- `ParameterMetadata.java` (enhanced)
- `ColumnMetadata.java` (new)
- `ParameterFilter.java` (new)
- `MetadataLoader.java` (refactored)
- `MetadataRepository.java` (refactored - stored proc call)
- `ReportService.java` (updated to use new classes)
- `StoredProcedureExecutor.java` (updated)
- `ResultSetToMapConverter.java` (added metadata-based conversion)

### Exception Classes (New)
- `MetadataParseException.java`
- `MetadataNotFoundException.java`
- `MissingParameterException.java`

### Test Infrastructure
- `TestMetadataLoader.java` (new - for H2 testing)
- `FullWorkflowIntegrationTestRefactored.java` (complete refactor)

## Breaking Changes

### API Changes

**Before:**
```java
DataSet employees = reportService.execute("employee_report", params);
DataRow row = employees.first();
String name = row.getString("FIRST_NAME");  // SQL column name
```

**After:**
```java
DataSet employees = reportService.execute("emp_001", params);
DataRow row = employees.first();
String name = row.getString("firstName");   // camelCase field name
```

### Metadata Structure

Old metadata JSON (REPORT_METADATA table):
```json
{
  "reportName": "employee_report",
  "storedProcedure": "usp_GetEmployees",
  "resultClass": "java.util.Map",
  "inputParameters": [...]
}
```

New metadata JSON (from stored proc):
```json
{
  "procedureId": "emp_001",
  "procedureName": "usp_GetEmployees",
  "parameters": [...],
  "columns": [
    {
      "columnName": "EMPLOYEE_ID",
      "fieldName": "employeeId",
      "columnType": "INTEGER"
    }
  ]
}
```

## Migration Guide

### 1. Database Changes

Create metadata stored procedure:
```sql
CREATE PROCEDURE usp_GetProcedureMetadata
    @ProcedureId VARCHAR(100) = NULL,
    @MetadataJson NVARCHAR(MAX) OUTPUT
AS ...
```

### 2. Update Metadata JSON

Add column definitions with field names:
```json
{
  "columns": [
    {"columnName": "EMPLOYEE_ID", "fieldName": "employeeId", "columnType": "INTEGER"},
    {"columnName": "FIRST_NAME", "fieldName": "firstName", "columnType": "VARCHAR"}
  ]
}
```

### 3. Update Application Code

Change all DataRow access from SQL names to camelCase:
```java
// OLD
row.getInt("EMPLOYEE_ID")

// NEW
row.getInt("employeeId")
```

### 4. Update Procedure Identifiers

Change from report names to procedure IDs:
```java
// OLD
reportService.execute("employee_report", params)

// NEW
reportService.execute("emp_001", params)
```

## Testing Strategy

### H2 Limitation

H2 doesn't support callable stored procedures, so tests use:
- `TestMetadataLoader` - In-memory metadata storage
- Direct SQL queries to create DataSets
- Manual column mapping to camelCase

### Integration Test

See `FullWorkflowIntegrationTestRefactored.java` for complete example:
- 4 stored procedures (employees, departments, sales, regions)
- 4-way joins
- Aggregations
- Computed columns
- Nested JSON generation (regions → departments → employees)

## Benefits

1. **Clearer Architecture** - Stored procedure-centric naming
2. **Flexibility** - Multiple procedures can be composed for complex reports
3. **Type Safety** - camelCase field names throughout (no SQL column names leaking)
4. **Performance** - Cache warming reduces metadata fetch overhead
5. **Maintainability** - Parameter filtering is automatic
6. **Self-Documenting** - Metadata includes column type information

## Future Work

- **Report Metadata Layer** - Add report-level metadata that references multiple procedures
- **Declarative Transformations** - Define joins/transformations in metadata
- **Hierarchical JSON Mapping** - See PHASE3_PLANNING.md for nested array support

## Rollback Plan

If needed, the old `ReportMetadata.java` class is still in the repository (but not used).
To rollback:
1. Revert MetadataRepository to SELECT-based approach
2. Restore ReportMetadata class
3. Update MetadataLoader to use ReportMetadata
4. Revert column name mapping in ResultSetToMapConverter
