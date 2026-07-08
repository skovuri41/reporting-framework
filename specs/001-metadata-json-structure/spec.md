# Feature Specification: Report Metadata JSON Structure

**Feature Branch**: `001-metadata-json-structure`

**Created**: 2026-07-07

**Status**: Draft

**Input**: User description: "Create a desired Report Metadata json that will be returned from metadata stored proc in d/b when requested by a given report id, when no parameters are passed, stored proc will return list of all reports in json returned."

**Context**: This is a **clean replacement** of the metadata structure in the existing reporting framework. The new structure supports multiple datasets per report, column metadata for UI generation, and comprehensive parameter definitions.

**Approach**: No backward compatibility required - this replaces the existing metadata implementation entirely.

## Current State & Replacement Scope

### Existing Components to be **REPLACED**

The following existing components will be removed/rewritten:

**Database Schema**:
- Current `REPORT_METADATA` table structure → Will be replaced with new schema
- Old columns: `REPORT_NAME` (PK), `STORED_PROCEDURE`, `RESULT_CLASS`, `METADATA_JSON`

**Java Classes to Remove/Replace**:
- `MetadataLoader.java` → **REPLACE** with new implementation for new JSON structure
- Any metadata DTO classes for old format → **DELETE** (if they exist)
- Old metadata parsing logic → **DELETE**

**Components to Keep (Framework Core)**:
- `ReportService.java` → Keep but update to use new metadata structure
- `StoredProcedureExecutor.java` → Keep (may need minor updates for dataset concept)
- `DataSet`, `DataRow`, `DataQuery`, `DataOperations` → **Keep unchanged** (core framework)
- `ResultSetToMapConverter.java` → **Keep unchanged**
- `ConnectionProvider` implementations → **Keep unchanged**

### New Metadata Structure

**New Database Schema** (to be created):
```sql
CREATE TABLE REPORT_METADATA (
    REPORT_ID VARCHAR(100) PRIMARY KEY,
    REPORT_NAME VARCHAR(255) NOT NULL,
    REPORT_DESCRIPTION VARCHAR(1000),
    METADATA_JSON NVARCHAR(MAX) NOT NULL
);
```

**New JSON Format** (stored in METADATA_JSON):
```json
{
  "reportId": "employee-analysis-001",
  "reportName": "Employee Analysis Report",
  "reportDescription": "Comprehensive employee analysis",
  "datasets": [
    {
      "datasource": "usp_GetEmployees",
      "datasourceId": "ds-001",
      "datasourceType": "StoredProc",
      "datasourceDescription": "Employee data",
      "parameters": [
        {
          "parameterName": "DepartmentId",
          "dataType": "INTEGER",
          "parameterDirection": "Input",
          "nullable": true
        }
      ],
      "columns": [
        {
          "sourceColumn": "employee_id",
          "displayName": "Employee ID",
          "dataType": "INTEGER",
          "sortable": true,
          "groupable": false,
          "filterable": true
        }
      ]
    }
  ]
}
```

### Cleanup Requirements

**Database Cleanup**:
- **FR-028**: Old REPORT_METADATA table schema MUST be dropped and recreated with new structure
- **FR-029**: Old metadata JSON format is completely removed (no dual-format support)

**Code Cleanup**:
- **FR-030**: Remove any old metadata DTO classes that mapped to old JSON structure
- **FR-031**: Remove old metadata parsing/loading logic
- **FR-032**: Update ReportService to use new metadata structure exclusively
- **FR-033**: Clean up any unused imports, utilities, or helpers related to old metadata format

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Retrieve Single Report Metadata (Priority: P1)

A developer or business analyst needs to understand the complete configuration for a specific report. They provide a report identifier and receive comprehensive metadata including all datasets, parameters, and column definitions with UI metadata (sortable, groupable, filterable flags).

**Why this priority**: This is the primary use case - understanding what datasets, parameters, and columns a report contains. This enables self-service report consumption and dynamic UI generation.

**Independent Test**: Can be fully tested by requesting metadata for a known report identifier and validating the returned structure contains report details, all datasets, parameters with directions, and columns with display metadata.

**Acceptance Scenarios**:

1. **Given** a valid report identifier exists in the system, **When** user requests metadata for that report, **Then** system returns complete metadata including reportId, reportName, reportDescription, and all datasets with their configurations
2. **Given** a report with multiple datasets, **When** user requests metadata, **Then** system returns each dataset with datasource details, parameters array, and columns array
3. **Given** a dataset with parameters, **When** metadata is retrieved, **Then** each parameter includes parameterName, dataType, parameterDirection, and nullable flag
4. **Given** a dataset with columns, **When** metadata is retrieved, **Then** each column includes sourceColumn, displayName, dataType, and UI flags (sortable, groupable, filterable)

---

### User Story 2 - Retrieve All Reports Catalog (Priority: P2)

A developer or administrator needs to discover what reports are available in the system. They request the full catalog without specifying a particular report and receive a list of all available reports with their complete metadata.

**Why this priority**: This enables report discovery and catalog browsing. While important for exploration and administration, it's secondary to being able to use a known report.

**Independent Test**: Can be fully tested by requesting the catalog without parameters and validating that all configured reports are returned with complete metadata structure.

**Acceptance Scenarios**:

1. **Given** multiple reports exist in the system, **When** user requests the catalog without specifying a report identifier, **Then** system returns a list containing complete metadata for all available reports
2. **Given** the catalog is requested, **When** no reports exist in the system, **Then** system returns an empty list
3. **Given** the catalog is requested, **When** reports have varying complexity (different dataset/parameter/column counts), **Then** all reports are included with their complete metadata regardless of complexity

---

### Edge Cases

- What happens when an invalid or non-existent report identifier is requested?
- How does the system handle reports with no datasets?
- How does the system handle datasets with no parameters versus datasets with many parameters?
- How does the system handle datasets with no columns (metadata-only datasets)?
- What happens when parameterDirection contains values other than "Input" or "Output"?
- How are different data types represented across parameters and columns (INTEGER, VARCHAR, DATE, DECIMAL, etc.)?
- What happens when a report's metadata is incomplete or malformed in the database?
- How does the system handle multiple datasets per report with different datasource types?
- What happens when metadata fails JSON Schema validation?
- Should the JSON Schema be retrievable via an API endpoint or provided as a static file?
- How should malformed JSON in METADATA_JSON column be handled?
- What happens when datasourceType is not "StoredProc" (future extensibility)?
- What happens when a dataset has both Input and Output parameters with the same name?
- How are null values handled in nullable parameters during execution?

## Requirements *(mandatory)*

### Functional Requirements

**Report Level**:
- **FR-001**: System MUST return metadata for a specific report when provided with a valid report identifier
- **FR-002**: System MUST return a list of all available reports when no report identifier is provided
- **FR-003**: Each report metadata MUST include reportId (unique identifier)
- **FR-004**: Each report metadata MUST include reportName
- **FR-005**: Each report metadata MUST include reportDescription
- **FR-006**: Each report metadata MUST include a datasets array (empty array if no datasets)

**Dataset Level**:
- **FR-007**: Each dataset MUST include datasource (name or identifier of the data source)
- **FR-008**: Each dataset MUST include datasourceId (unique identifier for the datasource)
- **FR-009**: Each dataset MUST include datasourceType (type/category of datasource)
- **FR-010**: Each dataset MUST include datasourceDescription
- **FR-011**: Each dataset MUST include a parameters array (empty array if no parameters)
- **FR-012**: Each dataset MUST include a columns array (empty array if no columns)

**Parameter Level**:
- **FR-013**: Each parameter MUST include parameterName
- **FR-014**: Each parameter MUST include dataType (SQL data type specification)
- **FR-015**: Each parameter MUST include parameterDirection (Input or Output)
- **FR-016**: Each parameter MUST include nullable flag (true/false)

**Column Level**:
- **FR-017**: Each column MUST include sourceColumn (original column name from datasource)
- **FR-018**: Each column MUST include displayName (user-friendly name for UI display)
- **FR-019**: Each column MUST include dataType (data type of the column)
- **FR-020**: Each column MUST include sortable flag (true/false indicating if column supports sorting)
- **FR-021**: Each column MUST include groupable flag (true/false indicating if column supports grouping)
- **FR-022**: Each column MUST include filterable flag (true/false indicating if column supports filtering)

**General**:
- **FR-023**: The metadata structure MUST be returned in JSON format
- **FR-024**: System MUST provide a JSON Schema definition that formally describes the metadata structure
- **FR-025**: The JSON Schema MUST validate all required fields, data types, and structural constraints for Report, Dataset, Parameter, and Column entities
- **FR-026**: System MUST handle requests for non-existent report identifiers with clear error indication
- **FR-027**: The catalog response MUST include consistent metadata structure for each report in the list

**Naming Transformation**:
- **FR-028**: System MUST convert parameterName to camelCase when loading metadata from database (e.g., "Department_Id" → "departmentId", "START_DATE" → "startDate")
- **FR-029**: System MUST convert column displayName to camelCase when loading metadata from database (e.g., "Employee_Name" → "employeeName", "TOTAL_SALES" → "totalSales")
- **FR-030**: System MUST preserve original sourceColumn name exactly as stored in database (no transformation)
- **FR-031**: Naming transformation MUST handle common database naming conventions (snake_case, UPPER_CASE, PascalCase)

### Key Entities *(include if feature involves data)*

- **Report Metadata**: Represents the complete configuration for a single report, including reportId, reportName, reportDescription, and a collection of datasets. This is the top-level metadata structure.

- **Dataset**: Represents a single data source within a report, including datasource information, parameters for execution, and columns for result presentation. A report can have multiple datasets from different sources.

- **Parameter**: Represents a single input/output parameter for a dataset, including its name, data type, direction (IN/OUT/INOUT), and nullability. Used for parameterized queries and stored procedure calls.

- **Column**: Represents a single column in the dataset result, including source column name, display name for UI, data type, and UI capability flags (sortable, groupable, filterable). Used for building dynamic report UIs.

- **Report Catalog**: Represents a collection of all report metadata entries, returned when no specific report is requested.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can determine all datasets, parameters, and columns for any report by examining the metadata response without consulting external documentation
- **SC-002**: Users can discover all available reports in the system through a single catalog request
- **SC-003**: The metadata structure is consistent across all reports, enabling automated UI generation and tooling
- **SC-004**: 100% of report configuration (datasets, parameters, columns, UI flags) is accurately represented in the metadata response
- **SC-005**: Report metadata can be retrieved in under 500 milliseconds for typical reports (up to 5 datasets, 20 parameters total, 50 columns total)
- **SC-006**: Catalog retrieval completes in under 1 second for typical catalog sizes (up to 100 reports)
- **SC-007**: Column metadata enables automated generation of sortable, groupable, and filterable UI components without hardcoding
- **SC-008**: All metadata responses validate successfully against the JSON Schema definition, ensuring 100% structural compliance
- **SC-009**: JSON Schema can be used by client applications for automatic validation and code generation
- **SC-010**: New metadata structure supports at least 10 datasets per report without performance degradation
- **SC-011**: Metadata retrieval and parsing completes in <100ms for typical single-dataset reports

## Assumptions

- **Clean Break**: No backward compatibility with old metadata format is required - complete replacement
- **Fresh Start**: All metadata will be created/loaded using the new structure
- Report metadata is stored in REPORT_METADATA table that can be queried by reportId (primary key)
- The system already has a mechanism to store and manage report definitions with multiple datasets
- Report identifiers (reportId) are unique within the system
- Dataset identifiers (datasourceId) are unique within a report
- Parameter data types follow standard SQL type conventions (INTEGER, VARCHAR, DATE, DECIMAL, etc.)
- Column data types follow standard SQL type conventions
- Parameter direction values are limited to: "Input" or "Output"
- The JSON structure will be consumed by programmatic clients for building dynamic report UIs
- Error responses for invalid report identifiers will follow the framework's existing error handling patterns
- The metadata represents the "contract" that clients must follow to successfully execute a report and render its results
- Sortable, groupable, and filterable flags indicate UI capabilities, not data constraints
- A report can have zero or more datasets (empty datasets array is valid)
- A dataset can have zero or more parameters and columns (empty arrays are valid)
- Datasource type uses value "StoredProc" for stored procedures
- JSON Schema will follow the JSON Schema Draft 7 specification (https://json-schema.org/)
- The schema will be versioned and maintained alongside the metadata structure
- Schema validation can be performed at both generation time (server-side) and consumption time (client-side)
- **Test Suite**: Existing tests will be updated to use new metadata format (no old format support)
- **Core Framework**: DataSet, DataQuery, DataOperations, and execution logic remain unchanged - only metadata layer is replaced
- **Database**: SQL Server is primary database; H2 for testing (with known stored procedure limitations)

## Metadata Structure Example

### Single Report Response

```json
{
  "reportId": "employee-analysis-001",
  "reportName": "Employee Analysis Report",
  "reportDescription": "Comprehensive analysis of employee data including salary, department, and performance metrics",
  "datasets": [
    {
      "datasource": "usp_GetEmployees",
      "datasourceId": "ds-001",
      "datasourceType": "StoredProc",
      "datasourceDescription": "Retrieves employee data with optional department filter",
      "parameters": [
        {
          "parameterName": "DepartmentId",
          "dataType": "INTEGER",
          "parameterDirection": "Input",
          "nullable": true
        },
        {
          "parameterName": "TotalRecords",
          "dataType": "INTEGER",
          "parameterDirection": "Output",
          "nullable": false
        }
      ],
      "columns": [
        {
          "sourceColumn": "employee_id",
          "displayName": "Employee ID",
          "dataType": "INTEGER",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "full_name",
          "displayName": "Full Name",
          "dataType": "VARCHAR",
          "sortable": true,
          "groupable": false,
          "filterable": true
        },
        {
          "sourceColumn": "department_name",
          "displayName": "Department",
          "dataType": "VARCHAR",
          "sortable": true,
          "groupable": true,
          "filterable": true
        },
        {
          "sourceColumn": "salary",
          "displayName": "Salary",
          "dataType": "DECIMAL",
          "sortable": true,
          "groupable": true,
          "filterable": true
        },
        {
          "sourceColumn": "hire_date",
          "displayName": "Hire Date",
          "dataType": "DATE",
          "sortable": true,
          "groupable": false,
          "filterable": true
        }
      ]
    },
    {
      "datasource": "usp_GetEmployeeMetrics",
      "datasourceId": "ds-002",
      "datasourceType": "StoredProc",
      "datasourceDescription": "Retrieves aggregated performance metrics",
      "parameters": [
        {
          "parameterName": "EmployeeId",
          "dataType": "INTEGER",
          "parameterDirection": "Input",
          "nullable": false
        }
      ],
      "columns": [
        {
          "sourceColumn": "metric_name",
          "displayName": "Metric",
          "dataType": "VARCHAR",
          "sortable": true,
          "groupable": true,
          "filterable": false
        },
        {
          "sourceColumn": "metric_value",
          "displayName": "Value",
          "dataType": "DECIMAL",
          "sortable": true,
          "groupable": false,
          "filterable": true
        }
      ]
    }
  ]
}
```

### Catalog Response

```json
{
  "reports": [
    {
      "reportId": "employee-analysis-001",
      "reportName": "Employee Analysis Report",
      "reportDescription": "Comprehensive analysis of employee data",
      "datasets": [...]
    },
    {
      "reportId": "sales-summary-002",
      "reportName": "Sales Summary Report",
      "reportDescription": "Monthly sales performance summary",
      "datasets": [...]
    }
  ],
  "count": 2
}
```
