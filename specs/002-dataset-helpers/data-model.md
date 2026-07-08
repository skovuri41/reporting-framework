# Data Model: Dataset Lookup and Multi-Dataset Execution

**Feature**: Dataset Helpers (002)

**Date**: 2025-07-07

**Purpose**: Define method signatures, return types, and behavior contracts for the two helper methods

## Overview

This feature adds two helper methods to existing classes without introducing new entities or data structures. This document specifies the exact method signatures and behavior.

## Method Specifications

### 1. Dataset Lookup Method

**Location**: `ReportMetadata.java`

**Signature**:
```java
public Dataset getDatasetById(String datasourceId)
```

**Purpose**: Retrieve a specific dataset from a report's dataset collection by its datasourceId

**Parameters**:
- `datasourceId` (String, non-null): The unique identifier of the dataset to retrieve (e.g., "ds-employees")

**Returns**:
- `Dataset`: The matching dataset object

**Throws**:
- `IllegalArgumentException`: When datasourceId is not found in the report's datasets
- `NullPointerException`: When datasourceId is null (standard Java behavior)

**Behavior**:
- Performs case-sensitive string comparison on datasourceId (FR-004)
- Searches through the datasets list in order until match is found
- Returns first matching dataset (duplicate IDs are configuration errors, not runtime concern)
- Exception message includes both requested ID and available IDs (FR-005)

**Example Usage**:
```java
ReportMetadata metadata = reportService.getMetadata("employee-report");
Dataset employeeDataset = metadata.getDatasetById("ds-employees");
// Now execute just this dataset
```

**Example Exception Message**:
```
IllegalArgumentException: Dataset not found: ds-invalid. Available datasets: [ds-employees, ds-departments, ds-metrics]
```

**Complexity**: O(n) where n = number of datasets (acceptable for typical 1-10 datasets)

**Thread Safety**: Thread-safe (read-only operation on immutable collection)

---

### 2. Multi-Dataset Execution Method

**Location**: `ReportService.java`

**Signature**:
```java
public Map<String, DataSet> executeAllDatasets(String reportId,
                                                 Map<String, Object> parameters)
```

**Purpose**: Execute all datasets in a report and return organized results keyed by datasourceId

**Parameters**:
- `reportId` (String, non-null): The unique identifier of the report to execute (e.g., "employee-analysis")
- `parameters` (Map<String, Object>, non-null): Shared parameter map containing all parameters for all datasets

**Returns**:
- `Map<String, DataSet>`: LinkedHashMap where:
  - Key: datasourceId (String) - e.g., "ds-employees"
  - Value: DataSet execution result for that dataset
  - Order: Preserved in execution order (matches datasets list order)

**Throws**:
- `ReportExecutionException`: When any dataset execution fails (includes datasourceId context)
- `MetadataNotFoundException`: When reportId is not found (thrown by metadataLoader.load())
- `NullPointerException`: When reportId or parameters is null

**Behavior**:
- Loads report metadata using MetadataLoader (FR-006)
- Executes datasets sequentially in the order they appear in ReportMetadata.datasets (FR-010)
- For each dataset:
  - Calls internal executeDataset(reportId, dataset, parameters) method
  - Each dataset extracts only the parameters it needs from the shared map (FR-009)
  - Validates required parameters are present (FR-015)
  - Ignores extra parameters (FR-016)
- Collects results in LinkedHashMap to preserve execution order
- Stops immediately on first failure and throws exception (FR-011, fail-fast)
- Returns empty Map if report has zero datasets (FR-013)
- Exception messages include datasourceId of failing dataset (FR-012)

**Example Usage**:
```java
ReportService service = new ReportService(connectionProvider);

Map<String, Object> params = Map.of(
    "departmentId", 10,
    "startDate", LocalDate.of(2024, 1, 1),
    "endDate", LocalDate.of(2024, 12, 31)
);

Map<String, DataSet> results = service.executeAllDatasets("employee-analysis", params);

// Access individual results
DataSet employees = results.get("ds-employees");
DataSet departments = results.get("ds-departments");
DataSet metrics = results.get("ds-metrics");

// Iterate in execution order
results.forEach((datasourceId, dataSet) -> {
    System.out.println("Dataset: " + datasourceId +
                       ", Rows: " + dataSet.size());
});
```

**Example Exception Message**:
```
ReportExecutionException: Failed to execute dataset 'ds-employees' in report 'employee-analysis':
Required parameter 'departmentId' is missing for dataset 'ds-employees'
```

**Complexity**: O(n × m) where:
- n = number of datasets (1-50 typical)
- m = execution time per dataset (0.5-2 seconds typical)

**Thread Safety**: Thread-safe (uses thread-safe MetadataLoader and creates new Map)

---

## Helper Method (Private)

### 3. Get Available Dataset IDs

**Location**: `ReportMetadata.java`

**Signature**:
```java
private List<String> getAvailableDatasetIds()
```

**Purpose**: Extract list of all datasourceIds for error message generation

**Returns**:
- `List<String>`: List of all datasourceIds in the report

**Behavior**:
- Streams through datasets collection
- Maps each Dataset to its datasourceId
- Collects into List
- Used only for error message formatting

**Example Output**:
```java
["ds-employees", "ds-departments", "ds-metrics"]
```

---

## Data Flow Diagrams

### Dataset Lookup Flow

```
User Code
  ↓
reportMetadata.getDatasetById("ds-employees")
  ↓
Stream datasets → filter by datasourceId → findFirst()
  ↓
Found? → Return Dataset
  ↓
Not Found? → Throw IllegalArgumentException with available IDs
```

### Multi-Dataset Execution Flow

```
User Code
  ↓
reportService.executeAllDatasets("report-id", params)
  ↓
Load ReportMetadata via MetadataLoader
  ↓
Create LinkedHashMap<String, DataSet> results
  ↓
For each dataset in metadata.getDatasets():
  ↓
  Try: executeDataset(reportId, dataset, params)
    ↓
    StoredProcedureExecutor extracts needed params
    ↓
    Execute stored procedure
    ↓
    Convert ResultSet → DataSet
    ↓
    Put (datasourceId, DataSet) into results map
  ↓
  Catch Exception: Wrap with context and re-throw (fail-fast)
  ↓
Return results map
```

### Parameter Extraction (Existing Behavior)

```
Shared Parameter Map:
{
  "departmentId": 10,
  "startDate": "2024-01-01",
  "endDate": "2024-12-31"
}
  ↓
Dataset 1 Metadata (ds-employees):
  parameters: ["departmentId"]
  ↓
  Extract: {"departmentId": 10}
  ↓
Dataset 2 Metadata (ds-departments):
  parameters: []
  ↓
  Extract: {} (no parameters needed)
  ↓
Dataset 3 Metadata (ds-metrics):
  parameters: ["startDate", "endDate"]
  ↓
  Extract: {"startDate": "2024-01-01", "endDate": "2024-12-31"}
```

**Note**: Parameter extraction is handled by existing StoredProcedureExecutor - no changes needed

---

## Return Type Specifications

### Map<String, DataSet> Structure

**Type**: `java.util.LinkedHashMap<String, DataSet>`

**Key Constraints**:
- Non-null strings
- Unique per dataset (datasourceIds must be unique in report)
- Case-sensitive

**Value Constraints**:
- Non-null DataSet objects
- Each DataSet contains:
  - List<DataRow> rows (may be empty for no-result stored procedures)
  - Map<String, Object> outputParameters (per-dataset output params)
  - String datasetName (reportId in current implementation)

**Order Guarantee**:
- Iteration order matches execution order
- Execution order matches ReportMetadata.datasets list order
- Preserved by LinkedHashMap

**Empty Map Scenario**:
- Returned when report has zero datasets
- Valid result (not an error condition)

---

## Exception Hierarchy

```
java.lang.Exception
  ↓
com.reporting.framework.exception.ReportExecutionException
  ↓
  Used for: Dataset execution failures during multi-dataset execution
  Context: datasourceId, reportId, root cause

java.lang.RuntimeException
  ↓
java.lang.IllegalArgumentException
  ↓
  Used for: Dataset not found in getDatasetById()
  Context: requested datasourceId, available datasourceIds

com.reporting.framework.exception.MetadataNotFoundException
  ↓
  Used for: Report not found (thrown by MetadataLoader)
  Context: reportId
```

---

## Validation Rules

### getDatasetById() Validation

1. **datasourceId non-null**: Implicit via NullPointerException
2. **datasourceId exists**: Validated by stream filter → orElseThrow
3. **Case-sensitive match**: equals() comparison (not equalsIgnoreCase)

### executeAllDatasets() Validation

1. **reportId non-null**: Implicit via NullPointerException
2. **parameters non-null**: Implicit via NullPointerException
3. **reportId exists**: Validated by metadataLoader.load() → MetadataNotFoundException
4. **Required parameters present**: Validated per-dataset by StoredProcedureExecutor
5. **Parameter types compatible**: Validated by GenericParameterBinder

---

## Integration Points

### Existing Components Used

**ReportMetadata** (modified):
- `List<Dataset> getDatasets()` - Read to search for matching dataset
- `getDatasetById()` - NEW method added

**ReportService** (modified):
- `ReportMetadata getMetadata(String reportId)` - Called to load metadata
- `private DataSet executeDataset(reportId, dataset, params)` - Reused for each dataset
- `executeAllDatasets()` - NEW method added

**MetadataLoader** (unchanged):
- `ReportMetadata load(String reportId)` - Used to load metadata

**StoredProcedureExecutor** (unchanged):
- `ExecutionResult execute(Dataset dataset, Map<String, Object> inputParameters)` - Used internally by executeDataset()

**GenericParameterBinder** (unchanged):
- Handles parameter extraction and type conversion

---

## Summary

**New Methods**: 2 public + 1 private helper

**New Classes**: 0

**New Exceptions**: 0 (reuses existing exception types)

**Modified Classes**: 2 (ReportMetadata, ReportService)

**Breaking Changes**: None (additive-only changes)

**Backward Compatibility**: 100% (existing code unaffected)
