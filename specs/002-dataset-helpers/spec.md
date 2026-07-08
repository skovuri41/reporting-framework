# Feature Specification: Dataset Lookup and Multi-Dataset Execution

**Feature Branch**: `002-dataset-helpers`

**Created**: 2025-07-07

**Status**: Draft

**Input**: User description: "new feature to add methods to ReportMetadata.java, to get the dataset by id, also another helper method to ReportService to execute all stored procs for datasets and get results as a map of id and dataset"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lookup Specific Dataset by ID (Priority: P1)

A developer building a multi-dataset report needs to execute only a specific dataset (e.g., "ds-employees") without iterating through the entire datasets collection. They can call a helper method on ReportMetadata to retrieve the dataset by its datasourceId and execute it directly.

**Why this priority**: This is the foundation for targeted dataset execution. Without the ability to lookup datasets by ID, developers must manually iterate and search, leading to error-prone code. This enables the multi-dataset execution feature (US2).

**Independent Test**: Can be fully tested by loading report metadata, calling getDatasetById() with valid and invalid IDs, and verifying correct dataset is returned or exception is thrown.

**Acceptance Scenarios**:

1. **Given** a report with multiple datasets, **When** developer calls getDatasetById() with a valid datasourceId, **Then** system returns the matching Dataset object
2. **Given** a report with multiple datasets, **When** developer calls getDatasetById() with a non-existent datasourceId, **Then** system throws IllegalArgumentException with clear error message
3. **Given** a report with datasets having IDs "ds-001", "ds-002", **When** developer calls getDatasetById("ds-002"), **Then** system returns the second dataset without requiring iteration

---

### User Story 2 - Execute All Datasets in Report (Priority: P2)

A developer needs to execute all datasets in a multi-dataset report (e.g., "employee-analysis" with datasets for employee data, department data, and metrics) and receive all results organized by dataset ID. They can call a single method on ReportService that executes all datasets and returns a Map<datasourceId, DataSet> for easy access to each result.

**Why this priority**: This provides a convenient way to execute complex reports with multiple data sources. Without this, developers must manually loop through datasets, execute each one, and collect results. This reduces boilerplate code and ensures consistent execution patterns.

**Independent Test**: Can be fully tested by creating a report with 3 datasets, providing parameters, calling executeAllDatasets(), and verifying that all three datasets are executed and results are returned in a map keyed by datasourceId.

**Acceptance Scenarios**:

1. **Given** a report with 3 datasets (ds-001, ds-002, ds-003), **When** developer calls executeAllDatasets() with parameters, **Then** system executes all three datasets and returns Map with 3 entries
2. **Given** a report with datasets requiring parameters, **When** developer provides a parameter map, **Then** each dataset extracts only the parameters it needs based on its metadata configuration
3. **Given** a report where the second dataset execution fails, **When** developer calls executeAllDatasets(), **Then** system throws exception immediately without executing remaining datasets (fail-fast behavior)
4. **Given** a report with zero datasets, **When** developer calls executeAllDatasets(), **Then** system returns empty Map without error
5. **Given** datasets with overlapping parameter names, **When** developer provides parameters, **Then** each dataset receives the correct parameter values from the shared map

---

### Edge Cases

- What happens when a report has duplicate datasourceIds (two datasets with same ID)?
- How does the system handle datasets with no parameters when executeAllDatasets() is called?
- What happens when the parameter map is null or empty for executeAllDatasets()?
- How are output parameters handled when executing all datasets - are they collected per-dataset or globally?
- What happens when a dataset's stored procedure returns no rows (empty ResultSet)?
- How does the system handle very large numbers of datasets (e.g., 50+ datasets in one report)?
- What happens when dataset execution order matters due to data dependencies between datasets?
- How are database connections managed when executing multiple datasets sequentially?

## Requirements *(mandatory)*

### Functional Requirements

**Dataset Lookup (US1)**:
- **FR-001**: ReportMetadata MUST provide a method to retrieve a Dataset by its datasourceId
- **FR-002**: The lookup method MUST throw IllegalArgumentException when datasourceId is not found
- **FR-003**: The lookup method MUST return the exact Dataset object that matches the provided datasourceId
- **FR-004**: The lookup method MUST perform case-sensitive matching on datasourceId
- **FR-005**: The error message for dataset-not-found MUST include the requested datasourceId and the available datasourceIds

**Multi-Dataset Execution (US2)**:
- **FR-006**: ReportService MUST provide a method to execute all datasets in a report
- **FR-007**: The method MUST return results as Map<String, DataSet> where key is datasourceId and value is execution result
- **FR-008**: The method MUST accept a single parameter map that is shared across all datasets
- **FR-009**: Each dataset MUST extract only the parameters it requires based on its metadata configuration
- **FR-010**: The method MUST execute datasets sequentially in the order they appear in the ReportMetadata.datasets list
- **FR-011**: The method MUST use fail-fast behavior - stop execution and throw exception on first dataset failure
- **FR-012**: The method MUST include the failing dataset's datasourceId in the exception message
- **FR-013**: The method MUST return an empty Map when report has zero datasets (not throw exception)
- **FR-014**: The method MUST use the existing execute(reportId, params) logic internally for each dataset

**Parameter Handling**:
- **FR-015**: System MUST validate that required (non-nullable) parameters are present in the shared parameter map for each dataset
- **FR-016**: System MUST ignore parameters in the shared map that a dataset does not use
- **FR-017**: System MUST pass the same parameter map reference to all datasets (no cloning/copying between executions)

**Error Handling**:
- **FR-018**: When a dataset execution fails, the exception MUST indicate which dataset failed (by datasourceId)
- **FR-019**: When a dataset execution fails, already-executed dataset results MUST NOT be returned
- **FR-020**: When a dataset is not found during execution, system MUST throw ReportExecutionException (not IllegalArgumentException)

### Key Entities

- **Dataset Lookup Result**: A single Dataset object retrieved from ReportMetadata by datasourceId, enabling targeted execution without iteration

- **Multi-Dataset Execution Result**: A Map<String, DataSet> containing execution results for all datasets in a report, keyed by datasourceId for easy access to specific results

- **Shared Parameter Map**: A single Map<String, Object> containing all parameters for all datasets, where each dataset extracts only the parameters it needs based on its metadata

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers can retrieve a specific dataset from a multi-dataset report in a single method call without writing iteration logic
- **SC-002**: Developers can execute all datasets in a report with a single method call, receiving organized results keyed by dataset ID
- **SC-003**: Multi-dataset execution completes in under 5 seconds for typical reports (up to 5 datasets, each returning under 1000 rows)
- **SC-004**: Parameter extraction is automatic - developers provide one parameter map and each dataset receives only its required parameters
- **SC-005**: Error messages clearly identify which dataset failed when multi-dataset execution encounters errors
- **SC-006**: Code complexity for executing multi-dataset reports is reduced by at least 70% compared to manual iteration approach

## Assumptions

- Reports may contain 1-10 datasets typically, with maximum of 50 datasets in extreme cases
- Datasets in a report execute independently and do not have data dependencies on each other's results
- The current execute(reportId, params) method will be reused internally for single dataset execution
- Dataset execution order follows the order in ReportMetadata.datasets list (array order)
- Database connections are managed by existing ConnectionProvider and closed properly after each dataset execution
- Output parameters are returned per-dataset (each DataSet includes its own output parameters)
- Duplicate datasourceIds within a report are considered configuration errors and handled at metadata validation time (not at runtime)
- Parameter names are consistent across datasets (e.g., if two datasets need "departmentId", they use the same parameter name)
- The fail-fast behavior is acceptable for v1 - collecting partial results is a future enhancement
- Integration tests will use H2 for dataset lookup testing; full multi-dataset execution will require SQL Server (due to H2 stored procedure limitations)
