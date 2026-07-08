# Data Model: Report Metadata JSON Structure

**Feature**: Report Metadata JSON Structure
**Date**: 2026-07-07
**Status**: Complete

## Overview

This document defines the immutable data model for report metadata. All entities follow Constitution Principle III (Immutability) and Principle II (Type Safety).

## Core Entities

### 1. ReportMetadata

**Purpose**: Top-level container for a single report's complete metadata including all datasets.

**Java Class**: `com.reporting.framework.metadata.model.ReportMetadata`

**Attributes**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| reportId | String | Yes | Not null, not empty, max 100 chars | Unique identifier for the report |
| reportName | String | Yes | Not null, not empty, max 255 chars | Display name of the report |
| reportDescription | String | No | Max 1000 chars | Human-readable description |
| datasets | List\<Dataset\> | Yes | Not null, can be empty list | Collection of datasets for this report |

**Immutability**:
- All fields are `private final`
- No setter methods
- `datasets` list wrapped in `Collections.unmodifiableList()`
- Constructor performs defensive copy of input list

**Validation Rules**:
- reportId must not be null or blank
- reportName must not be null or blank
- datasets must not be null (use empty list if no datasets)
- If datasets is empty, report cannot execute (validation at business logic layer)

**Relationships**:
- Contains 0 to N Dataset objects (1 to N recommended for executable reports)

**JSON Example**:
```json
{
  "reportId": "employee-analysis-001",
  "reportName": "Employee Analysis Report",
  "reportDescription": "Comprehensive analysis of employee data",
  "datasets": [...]
}
```

**Java Implementation Pattern**:
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportMetadata {
    private final String reportId;
    private final String reportName;
    private final String reportDescription;
    private final List<Dataset> datasets;

    @JsonCreator
    public ReportMetadata(
        @JsonProperty("reportId") String reportId,
        @JsonProperty("reportName") String reportName,
        @JsonProperty("reportDescription") String reportDescription,
        @JsonProperty("datasets") List<Dataset> datasets
    ) {
        this.reportId = Objects.requireNonNull(reportId, "reportId required");
        this.reportName = Objects.requireNonNull(reportName, "reportName required");
        this.reportDescription = reportDescription;
        this.datasets = Collections.unmodifiableList(
            datasets != null ? new ArrayList<>(datasets) : Collections.emptyList()
        );
    }

    public String getReportId() { return reportId; }
    public String getReportName() { return reportName; }
    public String getReportDescription() { return reportDescription; }
    public List<Dataset> getDatasets() { return datasets; }
}
```

---

### 2. Dataset

**Purpose**: Represents a single data source within a report (e.g., a stored procedure) with its parameters and column definitions.

**Java Class**: `com.reporting.framework.metadata.model.Dataset`

**Attributes**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| datasource | String | Yes | Not null, not empty | Name of the data source (e.g., "usp_GetEmployees") |
| datasourceId | String | Yes | Not null, not empty, unique within report | Unique ID for this dataset within the report |
| datasourceType | String | Yes | Not null, currently only "StoredProc" | Type of data source |
| datasourceDescription | String | No | Max 1000 chars | Description of what this dataset provides |
| parameters | List\<Parameter\> | Yes | Not null, can be empty | Input/output parameters for the dataset |
| columns | List\<Column\> | Yes | Not null, can be empty | Column definitions with UI metadata |

**Immutability**:
- All fields are `private final`
- No setters
- Both `parameters` and `columns` lists wrapped in `Collections.unmodifiableList()`

**Validation Rules**:
- datasource must match a valid stored procedure name pattern
- datasourceId must be unique within the report's datasets list
- datasourceType currently must be "StoredProc" (validate in loader)
- parameters list must not be null
- columns list must not be null

**Relationships**:
- Owned by ReportMetadata (composition)
- Contains 0 to N Parameter objects
- Contains 0 to N Column objects

**JSON Example**:
```json
{
  "datasource": "usp_GetEmployees",
  "datasourceId": "ds-001",
  "datasourceType": "StoredProc",
  "datasourceDescription": "Retrieves employee data with filters",
  "parameters": [...],
  "columns": [...]
}
```

---

### 3. Parameter

**Purpose**: Defines a single input or output parameter for a dataset's stored procedure.

**Java Class**: `com.reporting.framework.metadata.model.Parameter`

**Attributes**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| parameterName | String | Yes | Not null, not empty, camelCase | Name of the parameter (e.g., "departmentId") - **automatically converted to camelCase from database** |
| dataType | String | Yes | Not null, valid SQL type | SQL data type (e.g., "INTEGER", "VARCHAR") |
| parameterDirection | ParameterDirection | Yes | Not null, "Input" or "Output" | Direction enum |
| nullable | Boolean | Yes | Not null | Whether the parameter accepts null values |

**Naming Transformation**:
The `parameterName` field is **automatically converted to camelCase** when loading metadata from the database:
- Database: "Department_Id" or "DEPARTMENT_ID" → JSON: "departmentId"
- Database: "StartDate" → JSON: "startDate"
- Database: "employee_count" → JSON: "employeeCount"

This ensures consistent naming conventions in the JSON API regardless of database naming style.

**Enum: ParameterDirection**:
```java
public enum ParameterDirection {
    INPUT,   // Maps to "Input" in JSON
    OUTPUT;  // Maps to "Output" in JSON

    @JsonCreator
    public static ParameterDirection fromString(String value) {
        return "Input".equals(value) ? INPUT : OUTPUT;
    }

    @JsonValue
    public String toJson() {
        return this == INPUT ? "Input" : "Output";
    }
}
```

**Immutability**:
- All fields are `private final`
- No setters
- Enum provides type-safe direction

**Validation Rules**:
- parameterName must not be null or empty
- dataType must be a supported SQL type (see SqlTypeMapper)
- parameterDirection must be Input or Output
- nullable must be explicitly set (true or false)

**Supported SQL Types**:
- INTEGER, BIGINT, VARCHAR, NVARCHAR, DECIMAL, NUMERIC, DATE, DATETIME, DATETIME2, BIT

**Relationships**:
- Owned by Dataset (composition)
- No relationships to other entities

**JSON Example (Input Parameter)**:
```json
{
  "parameterName": "DepartmentId",
  "dataType": "INTEGER",
  "parameterDirection": "Input",
  "nullable": true
}
```

**JSON Example (Output Parameter)**:
```json
{
  "parameterName": "TotalRecords",
  "dataType": "INTEGER",
  "parameterDirection": "Output",
  "nullable": false
}
```

---

### 4. Column

**Purpose**: Defines a single column in the dataset result with UI metadata for dynamic report rendering.

**Java Class**: `com.reporting.framework.metadata.model.Column`

**Attributes**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| sourceColumn | String | Yes | Not null, not empty | Original column name from data source - **preserved exactly as-is** |
| displayName | String | Yes | Not null, not empty, camelCase | User-friendly name for UI display - **automatically converted to camelCase from database** |
| dataType | String | Yes | Not null, valid SQL type | SQL data type of the column |
| sortable | Boolean | Yes | Not null | Whether UI should allow sorting this column |
| groupable | Boolean | Yes | Not null | Whether UI should allow grouping by this column |
| filterable | Boolean | Yes | Not null | Whether UI should allow filtering this column |

**Naming Transformation**:
- **sourceColumn**: Preserved exactly as stored in database (e.g., "employee_id", "TOTAL_SALES") - used for SQL references
- **displayName**: Automatically converted to camelCase when loading from database:
  - Database: "Employee_Name" or "EMPLOYEE_NAME" → JSON: "employeeName"
  - Database: "TotalSales" → JSON: "totalSales"
  - Database: "hire_date" → JSON: "hireDate"

This dual approach ensures SQL compatibility (sourceColumn) while providing consistent naming for UI consumers (displayName).

**Immutability**:
- All fields are `private final`
- No setters
- All boolean flags must be explicitly set

**Validation Rules**:
- sourceColumn must not be null or empty
- displayName must not be null or empty
- dataType must be a supported SQL type
- All boolean flags must be explicitly true or false (no null)

**UI Flag Semantics**:
- `sortable`: true = column supports ORDER BY operations
- `groupable`: true = column suitable for GROUP BY operations (typically categorical data)
- `filterable`: true = column supports WHERE clause filtering

**Relationships**:
- Owned by Dataset (composition)
- No relationships to other entities

**JSON Example**:
```json
{
  "sourceColumn": "employee_id",
  "displayName": "Employee ID",
  "dataType": "INTEGER",
  "sortable": true,
  "groupable": false,
  "filterable": true
}
```

```json
{
  "sourceColumn": "department_name",
  "displayName": "Department",
  "dataType": "VARCHAR",
  "sortable": true,
  "groupable": true,
  "filterable": true
}
```

---

### 5. ReportCatalog

**Purpose**: Wrapper for a collection of all report metadata (used for catalog retrieval).

**Java Class**: `com.reporting.framework.metadata.model.ReportCatalog`

**Attributes**:
| Field | Type | Required | Constraints | Description |
|-------|------|----------|-------------|-------------|
| reports | List\<ReportMetadata\> | Yes | Not null, can be empty | List of all available report metadata |
| count | Integer | Yes | Not null, must equal reports.size() | Total number of reports |

**Immutability**:
- All fields are `private final`
- `reports` list wrapped in `Collections.unmodifiableList()`

**Validation Rules**:
- reports must not be null
- count must exactly match reports.size()

**JSON Example**:
```json
{
  "reports": [
    { "reportId": "report-001", ... },
    { "reportId": "report-002", ... }
  ],
  "count": 2
}
```

---

## Entity Lifecycle

### Creation Flow

```
Database REPORT_METADATA table
    ↓ (SQL SELECT)
Raw JSON string
    ↓ (Jackson ObjectMapper.readValue)
ReportMetadata object (immutable)
    ↓ (Cached in MetadataRepository)
In-memory cache
    ↓ (Retrieved by reportId)
Returned to caller
```

**Key Points**:
- Entities created via Jackson deserialization (no public constructors needed beyond @JsonCreator)
- Once created, entities are immutable (defensive copying in constructor)
- Cached instances shared across requests (safe due to immutability)

---

## Validation Strategy

### Construction-Time Validation

Performed in constructor via `Objects.requireNonNull()`:
```java
this.reportId = Objects.requireNonNull(reportId, "reportId required");
```

### Load-Time Validation

Performed by MetadataLoader:
1. **JSON Schema Validation**: Validate raw JSON against schema before deserialization
2. **Type Validation**: Jackson deserialization performs type checking
3. **Business Rule Validation**: Check datasourceType, SQL type support

### Example Validation Flow

```java
// 1. Load JSON from database
String json = loadFromDatabase(reportId);

// 2. Validate against JSON Schema
jsonSchemaValidator.validate(json);

// 3. Deserialize (Jackson validates types)
ReportMetadata metadata = objectMapper.readValue(json, ReportMetadata.class);

// 4. Business rules validation
validateBusinessRules(metadata);
```

---

## Error Conditions

### MetadataNotFoundException

**Triggered When**: Report ID does not exist in database

**Attributes**:
- `reportId`: The requested report ID that was not found

**Example**:
```java
throw new MetadataNotFoundException("employee-999");
```

### MetadataValidationException

**Triggered When**: Metadata fails JSON Schema validation or business rules

**Attributes**:
- `reportId`: The report with invalid metadata
- `validationErrors`: List of specific validation failures

**Example**:
```java
throw new MetadataValidationException(
    "employee-001",
    Arrays.asList(
        "Dataset 'ds-001' has invalid datasourceType: 'InvalidType'",
        "Parameter 'Dept' has unsupported dataType: 'UNSUPPORTED_TYPE'"
    )
);
```

---

## Testing Data Scenarios

### Minimal Report (Single Dataset, No Parameters, Few Columns)

```json
{
  "reportId": "simple-001",
  "reportName": "Simple Report",
  "reportDescription": "Basic report with no parameters",
  "datasets": [{
    "datasource": "usp_GetDepartments",
    "datasourceId": "ds-001",
    "datasourceType": "StoredProc",
    "datasourceDescription": "All departments",
    "parameters": [],
    "columns": [
      { "sourceColumn": "dept_id", "displayName": "ID", "dataType": "INTEGER", "sortable": true, "groupable": false, "filterable": true },
      { "sourceColumn": "dept_name", "displayName": "Department", "dataType": "VARCHAR", "sortable": true, "groupable": true, "filterable": true }
    ]
  }]
}
```

### Complex Report (Multiple Datasets, Mixed Parameters)

```json
{
  "reportId": "complex-001",
  "reportName": "Complex Multi-Dataset Report",
  "reportDescription": "Combines employee and metrics data",
  "datasets": [
    {
      "datasource": "usp_GetEmployees",
      "datasourceId": "ds-001",
      "datasourceType": "StoredProc",
      "datasourceDescription": "Employee master data",
      "parameters": [
        { "parameterName": "DepartmentId", "dataType": "INTEGER", "parameterDirection": "Input", "nullable": true },
        { "parameterName": "RecordCount", "dataType": "INTEGER", "parameterDirection": "Output", "nullable": false }
      ],
      "columns": [
        { "sourceColumn": "emp_id", "displayName": "Employee ID", "dataType": "INTEGER", "sortable": true, "groupable": false, "filterable": true },
        { "sourceColumn": "full_name", "displayName": "Name", "dataType": "VARCHAR", "sortable": true, "groupable": false, "filterable": true },
        { "sourceColumn": "salary", "displayName": "Salary", "dataType": "DECIMAL", "sortable": true, "groupable": true, "filterable": true }
      ]
    },
    {
      "datasource": "usp_GetEmployeeMetrics",
      "datasourceId": "ds-002",
      "datasourceType": "StoredProc",
      "datasourceDescription": "Performance metrics",
      "parameters": [
        { "parameterName": "EmployeeId", "dataType": "INTEGER", "parameterDirection": "Input", "nullable": false }
      ],
      "columns": [
        { "sourceColumn": "metric_name", "displayName": "Metric", "dataType": "VARCHAR", "sortable": false, "groupable": true, "filterable": false },
        { "sourceColumn": "metric_value", "displayName": "Value", "dataType": "DECIMAL", "sortable": true, "groupable": false, "filterable": true }
      ]
    }
  ]
}
```

### Edge Case: Empty Catalog

```json
{
  "reports": [],
  "count": 0
}
```

---

## Implementation Notes

- All DTOs use Jackson `@JsonCreator` and `@JsonProperty` annotations
- Lists are defensively copied in constructors and wrapped with `Collections.unmodifiableList()`
- No Lombok or Records - explicit code for clarity
- Test builders provided for easy test data construction (separate from production classes)
