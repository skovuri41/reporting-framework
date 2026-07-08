# Research: Dataset Lookup and Multi-Dataset Execution

**Feature**: Dataset Helpers (002)

**Date**: 2025-07-07

**Status**: Complete (Minimal research needed - straightforward enhancement)

## Overview

This feature adds two convenience methods to existing classes. All patterns and infrastructure already exist in the codebase. This document records design decisions and rationale.

## Research Topics

### 1. Dataset Lookup Implementation Approach

**Question**: What's the most efficient way to look up a dataset by ID?

**Options Considered**:

| Option | Approach | Pros | Cons | Decision |
|--------|----------|------|------|----------|
| A | Linear search through List<Dataset> | Simple, no additional data structures, preserves immutability | O(n) complexity | ✅ **CHOSEN** |
| B | Build internal Map<ID, Dataset> cache | O(1) lookup | Breaks immutability, adds memory overhead, complexity for small n | Rejected |
| C | Create separate index in MetadataLoader | Fast lookups across reports | Over-engineering for 1-10 datasets | Rejected |

**Decision**: **Option A** - Linear search

**Rationale**:
- Typical reports have 1-10 datasets (O(n) negligible)
- Maximum 50 datasets per spec (still acceptable)
- Maintains immutability principle (Constitution III)
- YAGNI principle (Constitution VII) - no premature optimization
- Consistent with existing patterns (ReportCatalog uses lists)

**Performance Analysis**:
- 10 datasets: ~10 object comparisons (~1 microsecond)
- 50 datasets: ~50 object comparisons (~5 microseconds)
- Negligible compared to database/execution overhead (milliseconds to seconds)

### 2. Multi-Dataset Execution Order

**Question**: Should datasets execute in parallel or sequentially?

**Options Considered**:

| Option | Approach | Pros | Cons | Decision |
|--------|----------|------|------|----------|
| A | Sequential (for-loop) | Simple, predictable order, easier error handling | Slower for independent datasets | ✅ **CHOSEN** |
| B | Parallel (ExecutorService) | Faster for independent datasets | Complex error handling, unpredictable order, thread management | Rejected for v1 |
| C | Smart detection (parallel if no deps) | Best of both worlds | Too complex, YAGNI violation | Rejected |

**Decision**: **Option A** - Sequential execution in list order

**Rationale**:
- Simplicity (Constitution VII)
- Predictable execution order matches metadata order (FR-010)
- Easier debugging and error messages
- Dataset execution time dominates (parallel won't help much)
- Fail-fast is simpler with sequential execution
- Parallel can be added later if needed without breaking API

**Performance Impact**:
- 5 datasets × 1 second each = 5 seconds total
- Target: <5 seconds per SC-003 (met with sequential)
- Parallel would save time only if datasets are truly independent and network-bound

### 3. Parameter Map Sharing Strategy

**Question**: Should parameter map be cloned per dataset or shared?

**Options Considered**:

| Option | Approach | Pros | Cons | Decision |
|--------|----------|------|------|----------|
| A | Share reference (pass same map) | Zero overhead, simple | Datasets could theoretically modify | ✅ **CHOSEN** |
| B | Clone per dataset | Defensive programming | Memory overhead, unnecessary copying | Rejected |
| C | Immutable wrapper | Safety without cloning | Added complexity | Rejected |

**Decision**: **Option A** - Share reference per FR-017

**Rationale**:
- Maps are read-only in StoredProcedureExecutor (existing pattern)
- No dataset code modifies parameter maps in practice
- Zero memory overhead
- Consistent with existing execute(reportId, params) behavior
- Simplicity over defensive programming

**Note**: If future code violates this assumption, wrap in Collections.unmodifiableMap() at call site

### 4. Return Type for Multi-Dataset Results

**Question**: What collection type should executeAllDatasets() return?

**Options Considered**:

| Option | Type | Pros | Cons | Decision |
|--------|------|------|------|----------|
| A | LinkedHashMap<String, DataSet> | Preserves insertion order, standard Map interface | Slight overhead vs HashMap | ✅ **CHOSEN** |
| B | HashMap<String, DataSet> | Fastest | Unpredictable iteration order | Rejected |
| C | Custom Result class with Map + metadata | Most flexible | Over-engineering, YAGNI violation | Rejected |
| D | List<Pair<String, DataSet>> | Preserves order, explicit pairing | Non-standard, harder to use | Rejected |

**Decision**: **Option A** - LinkedHashMap<String, DataSet>

**Rationale**:
- Preserves execution order per FR-010
- Standard Map interface (familiar to developers)
- Performance difference negligible (1-10 entries)
- Easy to iterate in order: `results.forEach((id, dataset) -> ...)`
- Consistent with spec requirement for "organized results"

### 5. Error Handling Strategy

**Question**: How should errors during multi-dataset execution be handled?

**Options Considered**:

| Option | Approach | Pros | Cons | Decision |
|--------|----------|------|------|----------|
| A | Fail-fast (stop on first error) | Simple, clear error semantics | No partial results | ✅ **CHOSEN** |
| B | Collect errors (try all, return results + errors) | More resilient, partial results | Complex return type, harder to use | Rejected for v1 |
| C | Best-effort (skip failures silently) | Maximum results | Hidden failures, dangerous | Rejected |

**Decision**: **Option A** - Fail-fast per FR-011

**Rationale**:
- Simpler implementation and error handling
- Clear failure semantics (user knows exactly what failed)
- Consistent with database transaction semantics (all or nothing)
- Easier debugging (first error is usually the root cause)
- User explicitly chose this option during specification (Q2: Option A)

**Future Enhancement**: Add optional parameter `boolean collectErrors` for resilient mode

### 6. Exception Types

**Question**: What exception should be thrown for different error scenarios?

**Decision Matrix**:

| Scenario | Exception Type | Rationale |
|----------|----------------|-----------|
| Dataset ID not found in ReportMetadata | IllegalArgumentException | Programming error, bad argument |
| Dataset execution fails | ReportExecutionException | Runtime execution error |
| Report ID not found | MetadataNotFoundException | Already thrown by metadataLoader.load() |
| Null parameter map | NullPointerException (implicit) | Follows Java convention for null |

**Rationale**:
- IllegalArgumentException: Indicates caller passed bad datasourceId (checked exception semantics)
- ReportExecutionException: Wraps execution failures with context (which dataset failed)
- Consistent with existing framework exception hierarchy
- Error messages include context (datasourceId, reportId) per FR-005, FR-012, FR-018

## Design Patterns Used

### 1. Reuse Existing Infrastructure

**Pattern**: Leverage existing execute(reportId, params) method

**Application**:
```java
// Don't create new execution logic
// Reuse existing tested path
DataSet result = executeDataset(reportId, dataset, parameters);
```

**Benefits**:
- Zero duplication
- Same validation, error handling, parameter binding
- Consistent behavior across single and multi-dataset execution

### 2. Fail-Fast Iterator Pattern

**Pattern**: Stop iteration on first exception

**Application**:
```java
for (Dataset dataset : metadata.getDatasets()) {
    try {
        DataSet result = executeDataset(reportId, dataset, parameters);
        results.put(dataset.getDatasourceId(), result);
    } catch (Exception e) {
        // Re-throw with context, stop iteration
        throw new ReportExecutionException("...", e);
    }
}
```

**Benefits**:
- Clear error semantics
- No resource cleanup issues (each dataset execution is self-contained)
- Predictable behavior

### 3. Stream-based Lookup with Exception

**Pattern**: Use Stream API for clean lookup with custom exception

**Application**:
```java
return datasets.stream()
    .filter(d -> d.getDatasourceId().equals(datasourceId))
    .findFirst()
    .orElseThrow(() -> new IllegalArgumentException("..."));
```

**Benefits**:
- Readable, declarative code
- Standard Java 8+ pattern
- Clear intent (find first match or throw)

## Alternatives Rejected

### 1. Fluent API for Multi-Dataset Execution

**Considered**:
```java
reportService.executeAll("report-id")
    .withParameters(params)
    .parallelExecution()
    .onError(COLLECT_ERRORS)
    .execute();
```

**Rejected**: Over-engineering for v1. Current requirements don't justify builder pattern. Can add later if needed.

### 2. Async/Future-based Execution

**Considered**: Return `CompletableFuture<Map<String, DataSet>>`

**Rejected**: Adds complexity without clear benefit. Most use cases need results immediately. Async can be added via wrapper if needed.

### 3. Dataset Result Wrapper Class

**Considered**: Custom class with results + timing + output params separated

**Rejected**: Map<String, DataSet> already provides everything needed. DataSet includes output params. Don't add abstraction without clear requirement.

## Performance Considerations

### Lookup Performance
- O(n) where n = number of datasets in report
- Typical n = 1-10 (negligible: <10 microseconds)
- Maximum n = 50 (acceptable: <50 microseconds)
- Dominated by other factors (database, network)

### Multi-Dataset Execution Performance
- Sequential execution: sum of individual dataset times
- Typical: 5 datasets × 0.5s = 2.5 seconds (well under 5s target)
- No additional overhead beyond method call stack
- Memory: one Map with n entries (negligible for n ≤ 50)

### Memory Usage
- No new data structures (reuses existing List<Dataset>)
- LinkedHashMap overhead: ~32 bytes per entry + entries
- For 10 datasets: ~320 bytes (negligible)
- DataSet objects already created (no additional allocation)

## Testing Approach

### Unit Tests (Isolated)
- ReportMetadata.getDatasetById() with mocked datasets
- ReportService.executeAllDatasets() with mocked loader/executor
- Exception scenarios without database

### Integration Tests (End-to-End)
- Full flow with H2 (lookup only) or SQL Server (full execution)
- Multiple datasets with different parameter requirements
- Fail-fast behavior verification
- Order preservation verification

### Performance Tests
- Benchmark multi-dataset execution (5 datasets, 1000 rows each)
- Verify <5 second target
- Measure lookup overhead (should be unmeasurable)

## Dependencies

**None** - This feature uses only existing framework code:
- ReportMetadata (add method)
- ReportService (add method)
- StoredProcedureExecutor (reuse as-is)
- MetadataLoader (reuse as-is)
- DataSet (returned from execution)

## Risks

**Risk Level**: **LOW**

**Identified Risks**:
1. ~~H2 testing limitation~~ - Mitigated: Use SQL Server for full tests
2. ~~Performance with 50 datasets~~ - Acceptable per analysis
3. ~~Breaking immutability~~ - Prevented: Read-only operations only

**No blockers identified**

## Summary

All research questions answered with simple, proven approaches:
- Linear search for lookup (acceptable performance)
- Sequential execution (simpler, meets performance targets)
- Shared parameter map (zero overhead)
- LinkedHashMap return type (preserves order)
- Fail-fast error handling (clearer semantics)

**Ready for implementation** - No open questions remain.
