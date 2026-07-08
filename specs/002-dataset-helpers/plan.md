# Implementation Plan: Dataset Lookup and Multi-Dataset Execution

**Branch**: `dataset-impl` | **Date**: 2025-07-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-dataset-helpers/spec.md`

**Note**: This is a straightforward enhancement adding helper methods to existing classes - no new dependencies or architectural changes required.

## Summary

Add two convenience methods to simplify multi-dataset report execution:
1. **Dataset Lookup**: Add `getDatasetById(String datasourceId)` method to ReportMetadata for direct dataset access without iteration
2. **Multi-Dataset Execution**: Add `executeAllDatasets(String reportId, Map<String, Object> parameters)` method to ReportService that executes all datasets in a report and returns Map<datasourceId, DataSet>

**Approach**: Extend existing classes with helper methods. Reuse current execution infrastructure (StoredProcedureExecutor, parameter binding). No new components needed.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**:
- Existing framework dependencies (no new additions required)
- Jackson 2.17.2 (JSON - already in use)
- SLF4J 2.0.13 + Logback (logging - already in use)

**Storage**: SQL Server (production), H2 1.4.200 (testing for lookup tests)

**Testing**: JUnit Jupiter 5.10.3, AssertJ 3.26.0

**Target Platform**: JVM-based applications (Java 17+ runtime)

**Project Type**: Java library/framework enhancement (small API addition)

**Performance Goals**:
- Dataset lookup: O(n) single pass through datasets list (negligible overhead for typical 1-10 datasets)
- Multi-dataset execution: <5 seconds for 5 datasets with 1000 rows each
- No additional memory overhead (reuse existing execution path)

**Constraints**:
- Must maintain immutability of ReportMetadata (read-only operation)
- Must not modify existing execute() method signature or behavior
- Parameter map is shared reference (no cloning) per FR-017
- Fail-fast error handling (stop on first failure) per FR-011

**Scale/Scope**:
- Typical reports: 1-10 datasets
- Maximum supported: 50 datasets per report
- Target execution time: <5 seconds for typical multi-dataset reports

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Based on the Reporting Framework Constitution (v1.0.0), verify:

- [x] **Metadata-Driven**: Are report definitions configured via database metadata, not hardcoded?
  - ✅ YES - No change to metadata-driven approach. Lookup uses existing metadata, execution reads from database
- [x] **Type Safety**: Does the design provide typed accessors without requiring manual casting?
  - ✅ YES - Return types are `Dataset` and `Map<String, DataSet>` with proper generics, no casting required
- [x] **Immutability**: Do transformations return new instances rather than modifying in place?
  - ✅ YES - Lookup is read-only, execution returns new DataSet instances via existing immutable pipeline
- [x] **Fluent DSL**: Are complex operations expressible through chainable APIs?
  - ⚠️ N/A - These are convenience methods, not transformation operations. DSL applies to DataQuery/DataOperations
- [x] **Test-First**: Are integration tests planned before implementation (FullWorkflowIntegrationTest pattern)?
  - ✅ YES - Integration tests for lookup and multi-dataset execution (see quickstart.md)
- [x] **Transparent Limitations**: Are known constraints documented with workarounds in CLAUDE.md?
  - ✅ YES - H2 limitation for multi-dataset execution documented (SQL Server needed for full testing)
- [x] **Simplicity**: Does the design avoid over-engineering and unnecessary abstraction (YAGNI)?
  - ✅ YES - Simple helper methods, reuse existing execution logic, no new classes or patterns

**Violations Requiring Justification**: None - all gates pass or N/A

## Project Structure

### Documentation (this feature)

```text
specs/002-dataset-helpers/
├── plan.md              # This file
├── research.md          # Phase 0: Design decisions (minimal - straightforward enhancement)
├── data-model.md        # Phase 1: Method signatures and return types
├── quickstart.md        # Phase 1: Test scenarios for validation
└── tasks.md             # Phase 2: (Created by /speckit-tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/reporting/framework/
├── metadata/
│   └── model/
│       └── ReportMetadata.java         # UPDATE - Add getDatasetById() method
├── api/
│   └── ReportService.java              # UPDATE - Add executeAllDatasets() method
└── (other packages unchanged)

src/test/java/com/reporting/framework/
├── metadata/
│   └── model/
│       └── ReportMetadataTest.java     # NEW or UPDATE - Test getDatasetById()
├── api/
│   └── ReportServiceTest.java          # UPDATE - Test executeAllDatasets()
└── integration/
    └── MultiDatasetIntegrationTest.java # NEW - End-to-end multi-dataset execution test
```

**Structure Decision**: Single project structure (existing framework). Only two files modified: ReportMetadata.java and ReportService.java. New integration test for multi-dataset scenarios.

**Files to UPDATE**:
- `src/main/java/com/reporting/framework/metadata/model/ReportMetadata.java` - Add getDatasetById()
- `src/main/java/com/reporting/framework/api/ReportService.java` - Add executeAllDatasets()
- Test files - Add new tests for both methods

**Files to KEEP UNCHANGED**:
- MetadataLoader, MetadataRepository - No changes needed
- StoredProcedureExecutor - Reused as-is
- Dataset, Parameter, Column DTOs - No changes needed
- All other framework components

## Complexity Tracking

> **No violations** - Constitution gates all pass. No complexity justification needed.

**Simplicity Analysis**:
- Only 2 methods added across 2 existing classes
- No new dependencies, packages, or architectural patterns
- Reuses 100% of existing execution infrastructure
- Total new code estimate: ~40 lines production code, ~150 lines test code
- Complexity rating: LOW (straightforward helper methods)

## Phase 0: Research (Minimal)

**Research Topics**: None required - this is a straightforward enhancement using existing patterns

**Decisions Made**:
1. **Lookup Implementation**: Linear search through datasets list - acceptable for typical 1-10 datasets
2. **Execution Pattern**: Reuse existing ReportService.execute(reportId, params) internally via private helper
3. **Error Messages**: Include datasourceId context in all exceptions for debugging
4. **Return Type**: Use LinkedHashMap to preserve dataset order

All patterns and approaches already established in codebase. See research.md for detailed rationale.

## Phase 1: Design Artifacts

### Data Model (data-model.md)
- Method signatures for getDatasetById() and executeAllDatasets()
- Return types and exception specifications
- Parameter extraction logic from shared map

### Contracts (N/A)
- No external API contracts - this is an internal framework enhancement
- Method signatures documented in data-model.md

### Quickstart (quickstart.md)
- Test scenario 1: Dataset lookup with valid ID
- Test scenario 2: Dataset lookup with invalid ID (exception)
- Test scenario 3: Execute all datasets (3 datasets, verify Map result)
- Test scenario 4: Execute all with failure (verify fail-fast)

## Implementation Notes

### Dataset Lookup (getDatasetById)

**Location**: `ReportMetadata.java`

**Logic**:
```java
public Dataset getDatasetById(String datasourceId) {
    return datasets.stream()
        .filter(d -> d.getDatasourceId().equals(datasourceId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "Dataset not found: " + datasourceId +
            ". Available datasets: " + getAvailableDatasetIds()
        ));
}

private List<String> getAvailableDatasetIds() {
    return datasets.stream()
        .map(Dataset::getDatasourceId)
        .collect(Collectors.toList());
}
```

**Key Points**:
- Case-sensitive match per FR-004
- Exception includes available IDs per FR-005
- O(n) complexity acceptable for small n (1-10 typical)

### Multi-Dataset Execution (executeAllDatasets)

**Location**: `ReportService.java`

**Logic**:
```java
public Map<String, DataSet> executeAllDatasets(String reportId,
                                                 Map<String, Object> parameters) {
    ReportMetadata metadata = metadataLoader.load(reportId);
    Map<String, DataSet> results = new LinkedHashMap<>();

    for (Dataset dataset : metadata.getDatasets()) {
        try {
            DataSet result = executeDataset(reportId, dataset, parameters);
            results.put(dataset.getDatasourceId(), result);
        } catch (Exception e) {
            throw new ReportExecutionException(
                "Failed to execute dataset '" + dataset.getDatasourceId() +
                "' in report '" + reportId + "': " + e.getMessage(), e
            );
        }
    }

    return results;
}
```

**Key Points**:
- Reuses existing executeDataset() private method
- LinkedHashMap preserves insertion order per FR-010
- Fail-fast: exception thrown immediately per FR-011
- Empty Map returned for zero datasets per FR-013
- Shared parameter map per FR-008, FR-017

### Parameter Extraction

**Current Behavior** (already implemented in StoredProcedureExecutor):
- Each dataset's parameters are defined in metadata
- Executor extracts only the parameters it needs from the input map
- Validation ensures required parameters are present
- Extra parameters in map are ignored

**No changes needed** - existing parameter binding logic handles this per FR-009, FR-015, FR-016

## Testing Strategy

### Unit Tests

**ReportMetadataTest** (new or update):
- Test getDatasetById() with valid IDs (multiple datasets)
- Test getDatasetById() with invalid ID (exception thrown)
- Test error message includes available IDs

**ReportServiceTest** (update):
- Mock multi-dataset execution scenarios
- Test empty dataset list returns empty Map
- Test parameter passing to underlying execute method

### Integration Tests

**MultiDatasetIntegrationTest** (new):
- Setup: Create report with 3 datasets in H2 (or SQL Server)
- Test 1: Execute all datasets, verify Map<String, DataSet> structure
- Test 2: Verify each dataset executed with correct parameters
- Test 3: Simulate failure in second dataset, verify fail-fast
- Test 4: Verify execution order matches metadata order
- Test 5: Edge case - report with zero datasets

**Test Data**:
- Report with 3 datasets: ds-employees, ds-departments, ds-metrics
- Shared parameters: departmentId, startDate, endDate
- ds-employees uses: departmentId
- ds-departments uses: none (all parameters optional)
- ds-metrics uses: startDate, endDate

### Performance Tests

- Benchmark multi-dataset execution (5 datasets, 1000 rows each)
- Target: <5 seconds total execution time
- Measure per-dataset execution time vs. total time overhead

## Known Limitations

### H2 Testing Limitation (Documented in CLAUDE.md)

**Issue**: H2 doesn't support callable stored procedures (uses CREATE ALIAS for functions)

**Impact**:
- Dataset lookup can be fully tested with H2 (only reads metadata)
- Multi-dataset execution requires SQL Server for full integration testing
- Unit tests with mocks will cover multi-dataset logic

**Workaround**:
- Use H2 for lookup tests and mocked multi-dataset tests
- Require SQL Server for full multi-dataset integration tests
- Document in CLAUDE.md and quickstart.md

### No Partial Results (v1)

**Limitation**: Fail-fast means no partial results returned on error

**Rationale**: Simpler implementation for v1, clearer error semantics

**Future Enhancement**: Add optional "collect errors" mode that returns both successful results and error details

## Next Steps

After this plan is approved:

1. **Run /speckit-tasks** to generate implementation task list
2. **Implement in phases**:
   - Phase 1: Add getDatasetById() with tests
   - Phase 2: Add executeAllDatasets() with tests
   - Phase 3: Integration tests and documentation
3. **Update CLAUDE.md** with new helper methods and examples
4. **Create PR** with before/after code examples

**Estimated Effort**: Small (2-3 hours implementation + testing)

**Risk**: Low - no architectural changes, reuses existing infrastructure
