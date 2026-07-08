# Quickstart: Dataset Lookup and Multi-Dataset Execution

**Feature**: Dataset Helpers (002)

**Date**: 2025-07-07

**Purpose**: Runnable validation scenarios that prove the feature works end-to-end

## Prerequisites

### Required Software
- Java 17+
- Maven 3.6+
- Git

### Required Setup
```bash
# Clone repository (if not already)
git clone <repository-url>
cd reporting-framework

# Checkout feature branch
git checkout dataset-impl

# Build project
mvn clean compile

# Run tests
mvn test
```

### Test Data Requirements

For full integration tests, you'll need:
- **H2 Database**: For unit and lookup integration tests (included in test scope)
- **SQL Server** (optional): For full multi-dataset execution integration tests (H2 has stored procedure limitations)

Test data will be created automatically by integration tests (see scenarios below).

---

## Validation Scenarios

### Scenario 1: Dataset Lookup - Valid ID

**Purpose**: Verify getDatasetById() returns correct dataset for valid datasourceId

**Setup**:
```java
// Test creates report metadata with 3 datasets
ReportMetadata metadata = createTestMetadata(
    Dataset("ds-employees", ...),
    Dataset("ds-departments", ...),
    Dataset("ds-metrics", ...)
);
```

**Execute**:
```java
Dataset result = metadata.getDatasetById("ds-employees");
```

**Expected Outcome**:
```java
assertThat(result).isNotNull();
assertThat(result.getDatasourceId()).isEqualTo("ds-employees");
assertThat(result.getDatasource()).isEqualTo("usp_GetEmployees");
```

**Test Class**: `ReportMetadataTest.testGetDatasetById_ValidId()`

**Run Command**:
```bash
mvn test -Dtest=ReportMetadataTest#testGetDatasetById_ValidId
```

**Success Criteria**: Test passes, dataset returned matches expected datasourceId

---

### Scenario 2: Dataset Lookup - Invalid ID

**Purpose**: Verify getDatasetById() throws exception with helpful message for non-existent datasourceId

**Setup**:
```java
// Same test metadata as Scenario 1
ReportMetadata metadata = createTestMetadata(...);
```

**Execute**:
```java
assertThatThrownBy(() -> metadata.getDatasetById("ds-invalid"))
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessageContaining("Dataset not found: ds-invalid")
    .hasMessageContaining("Available datasets: [ds-employees, ds-departments, ds-metrics]");
```

**Expected Outcome**:
- `IllegalArgumentException` thrown
- Message includes requested ID: "ds-invalid"
- Message includes available IDs: "[ds-employees, ds-departments, ds-metrics]"

**Test Class**: `ReportMetadataTest.testGetDatasetById_InvalidId()`

**Run Command**:
```bash
mvn test -Dtest=ReportMetadataTest#testGetDatasetById_InvalidId
```

**Success Criteria**: Exception thrown with both requested and available IDs in message

---

### Scenario 3: Multi-Dataset Execution - Happy Path

**Purpose**: Verify executeAllDatasets() executes all datasets and returns organized results

**Setup**:
```sql
-- Test data inserted into H2 or SQL Server
INSERT INTO REPORT_METADATA (REPORT_ID, REPORT_NAME, METADATA_JSON)
VALUES ('multi-dataset-test', 'Multi-Dataset Test Report', '{
  "reportId": "multi-dataset-test",
  "reportName": "Multi-Dataset Test Report",
  "datasets": [
    {
      "datasourceId": "ds-001",
      "datasource": "usp_Dataset1",
      "parameters": [{"parameterName": "param1", "dataType": "INTEGER", ...}],
      "columns": [...]
    },
    {
      "datasourceId": "ds-002",
      "datasource": "usp_Dataset2",
      "parameters": [],
      "columns": [...]
    },
    {
      "datasourceId": "ds-003",
      "datasource": "usp_Dataset3",
      "parameters": [{"parameterName": "param2", "dataType": "VARCHAR", ...}],
      "columns": [...]
    }
  ]
}');
```

**Execute**:
```java
ReportService service = new ReportService(connectionProvider);

Map<String, Object> params = Map.of(
    "param1", 123,
    "param2", "test-value"
);

Map<String, DataSet> results = service.executeAllDatasets("multi-dataset-test", params);
```

**Expected Outcome**:
```java
// Verify map structure
assertThat(results).hasSize(3);
assertThat(results).containsKeys("ds-001", "ds-002", "ds-003");

// Verify each dataset executed
DataSet dataset1 = results.get("ds-001");
assertThat(dataset1).isNotNull();
assertThat(dataset1.size()).isGreaterThan(0); // Has rows

DataSet dataset2 = results.get("ds-002");
assertThat(dataset2).isNotNull();

DataSet dataset3 = results.get("ds-003");
assertThat(dataset3).isNotNull();

// Verify execution order preserved
List<String> keys = new ArrayList<>(results.keySet());
assertThat(keys).containsExactly("ds-001", "ds-002", "ds-003");
```

**Test Class**: `MultiDatasetIntegrationTest.testExecuteAllDatasets_HappyPath()`

**Run Command**:
```bash
mvn test -Dtest=MultiDatasetIntegrationTest#testExecuteAllDatasets_HappyPath
```

**Success Criteria**:
- All 3 datasets executed successfully
- Map contains 3 entries
- Keys match datasourceIds
- Execution order preserved

---

### Scenario 4: Multi-Dataset Execution - Fail Fast

**Purpose**: Verify executeAllDatasets() stops immediately on first failure and throws exception with context

**Setup**:
```java
// Create report with 3 datasets where second one will fail
// ds-002 requires parameter "missingParam" that is not provided
```

**Execute**:
```java
Map<String, Object> params = Map.of(
    "param1", 123
    // Intentionally missing "missingParam" for ds-002
);

assertThatThrownBy(() ->
    service.executeAllDatasets("multi-dataset-test", params)
)
    .isInstanceOf(ReportExecutionException.class)
    .hasMessageContaining("Failed to execute dataset 'ds-002'")
    .hasMessageContaining("multi-dataset-test")
    .hasCauseInstanceOf(ReportExecutionException.class);
```

**Expected Outcome**:
- `ReportExecutionException` thrown after ds-001 succeeds but ds-002 fails
- Exception message includes:
  - Failing dataset ID: "ds-002"
  - Report ID: "multi-dataset-test"
  - Root cause details
- ds-003 is NOT executed (fail-fast behavior)
- No results returned (exception thrown instead)

**Test Class**: `MultiDatasetIntegrationTest.testExecuteAllDatasets_FailFast()`

**Run Command**:
```bash
mvn test -Dtest=MultiDatasetIntegrationTest#testExecuteAllDatasets_FailFast
```

**Success Criteria**:
- Exception thrown after first failure
- Exception includes dataset ID and report ID
- Remaining datasets not executed

---

### Scenario 5: Multi-Dataset Execution - Empty Datasets

**Purpose**: Verify executeAllDatasets() returns empty Map (not error) for report with zero datasets

**Setup**:
```sql
-- Report with zero datasets
INSERT INTO REPORT_METADATA (REPORT_ID, REPORT_NAME, METADATA_JSON)
VALUES ('empty-report', 'Empty Report', '{
  "reportId": "empty-report",
  "reportName": "Empty Report",
  "datasets": []
}');
```

**Execute**:
```java
Map<String, DataSet> results = service.executeAllDatasets("empty-report", Map.of());
```

**Expected Outcome**:
```java
assertThat(results).isNotNull();
assertThat(results).isEmpty();
assertThat(results).hasSize(0);
```

**Test Class**: `MultiDatasetIntegrationTest.testExecuteAllDatasets_EmptyDatasets()`

**Run Command**:
```bash
mvn test -Dtest=MultiDatasetIntegrationTest#testExecuteAllDatasets_EmptyDatasets
```

**Success Criteria**:
- Empty Map returned (not null, not exception)
- No errors or warnings

---

### Scenario 6: Parameter Extraction Verification

**Purpose**: Verify each dataset extracts only the parameters it needs from shared map

**Setup**:
```java
// Create report with 3 datasets with different parameter needs:
// ds-001 needs: param1
// ds-002 needs: none
// ds-003 needs: param2, param3

Map<String, Object> sharedParams = Map.of(
    "param1", 100,
    "param2", "value2",
    "param3", LocalDate.of(2024, 1, 1),
    "extraParam", "ignored" // Not used by any dataset
);
```

**Execute**:
```java
Map<String, DataSet> results = service.executeAllDatasets("param-test-report", sharedParams);
```

**Expected Outcome**:
```java
// All datasets execute successfully
assertThat(results).hasSize(3);

// Verify ds-001 got param1
DataSet ds1 = results.get("ds-001");
assertThat(ds1).isNotNull();
// (Dataset executed means param1 was extracted correctly)

// Verify ds-002 executed with no parameters
DataSet ds2 = results.get("ds-002");
assertThat(ds2).isNotNull();

// Verify ds-003 got param2 and param3
DataSet ds3 = results.get("ds-003");
assertThat(ds3).isNotNull();

// Verify extraParam was ignored (no error)
```

**Test Class**: `MultiDatasetIntegrationTest.testParameterExtraction()`

**Run Command**:
```bash
mvn test -Dtest=MultiDatasetIntegrationTest#testParameterExtraction
```

**Success Criteria**:
- All datasets execute with their specific parameters
- Extra parameters ignored (no error)
- Missing required parameters cause failure

---

## Running All Validation Scenarios

### Run All Dataset Helper Tests

```bash
# Run all tests for this feature
mvn test -Dtest=ReportMetadataTest,MultiDatasetIntegrationTest
```

### Run Full Test Suite

```bash
# Run all tests including existing framework tests
mvn test
```

**Expected Results**:
- All dataset helper tests pass
- No regressions in existing tests
- Total test count increases by ~8 tests (6 scenarios + 2 edge cases)

---

## Verification Checklist

After running validation scenarios, verify:

- [ ] **Scenario 1**: getDatasetById() returns correct dataset for valid ID
- [ ] **Scenario 2**: getDatasetById() throws exception with helpful message for invalid ID
- [ ] **Scenario 3**: executeAllDatasets() returns Map with all dataset results
- [ ] **Scenario 4**: executeAllDatasets() stops on first failure (fail-fast)
- [ ] **Scenario 5**: executeAllDatasets() returns empty Map for zero datasets
- [ ] **Scenario 6**: Parameter extraction works correctly (each dataset gets what it needs)
- [ ] **No regressions**: Existing framework tests still pass
- [ ] **Code quality**: No new warnings or code quality issues

---

## Troubleshooting

### Issue: Tests fail with "H2 stored procedure limitation"

**Solution**: This is expected for full multi-dataset execution tests. Either:
1. Use SQL Server for full integration testing
2. Run unit tests with mocked execution (no database)

**Commands**:
```bash
# Run only unit tests (no database)
mvn test -Dtest=ReportMetadataTest

# Run with SQL Server (update connection properties first)
mvn test -Dspring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=test
```

### Issue: "Dataset not found" exception in test

**Cause**: Test metadata not created correctly

**Solution**: Check test setup - ensure datasets are created with correct IDs

### Issue: Performance test fails (<5 second target)

**Cause**: Machine too slow or database connection issues

**Solution**:
- Run on faster machine
- Check network latency to database
- Verify database is not under load

---

## Next Steps

After all validation scenarios pass:

1. **Update CLAUDE.md**: Add examples of using the new helper methods
2. **Create PR**: Include before/after code examples showing code reduction
3. **Update integration tests**: Add these scenarios to FullWorkflowIntegrationTest if appropriate

---

## Code Examples

### Before This Feature (Manual Iteration)

```java
// Old approach: Manual iteration and collection
ReportMetadata metadata = reportService.getMetadata("employee-analysis");
Map<String, DataSet> results = new LinkedHashMap<>();

for (Dataset dataset : metadata.getDatasets()) {
    try {
        // Execute each dataset manually
        DataSet result = executeSingleDataset(metadata.getReportId(), dataset, params);
        results.put(dataset.getDatasourceId(), result);
    } catch (Exception e) {
        throw new RuntimeException("Failed: " + dataset.getDatasourceId(), e);
    }
}

// Use results
DataSet employees = results.get("ds-employees");
```

**Lines of code**: ~15 lines

**Complexity**: High (error handling, collection management, iteration)

### After This Feature (Helper Methods)

```java
// New approach: Single method call
Map<String, DataSet> results = reportService.executeAllDatasets("employee-analysis", params);

// Use results
DataSet employees = results.get("ds-employees");
```

**Lines of code**: 2 lines

**Complexity**: Low (framework handles everything)

**Code Reduction**: 87% (15 lines → 2 lines)

---

## Performance Benchmarks

### Expected Performance

| Scenario | Datasets | Rows/Dataset | Expected Time | Target |
|----------|----------|--------------|---------------|--------|
| Small report | 1 | 100 | <1 second | <5 seconds |
| Medium report | 5 | 1000 | 2-3 seconds | <5 seconds |
| Large report | 10 | 1000 | 4-5 seconds | <5 seconds |
| Extreme | 50 | 100 | 5-10 seconds | N/A (edge case) |

### Lookup Performance

- Single lookup: <1 microsecond (unmeasurable overhead)
- 10 lookups: <10 microseconds
- Dominated by database execution time (500ms-2s per dataset)

---

## Documentation Updates Required

After feature is complete:

1. **CLAUDE.md**: Add section "Multi-Dataset Reports" with examples
2. **README.md**: Update examples to show executeAllDatasets()
3. **Javadocs**: Add comprehensive javadoc to both new methods

**Example CLAUDE.md Addition**:

```markdown
## Multi-Dataset Reports

### Execute Specific Dataset

\`\`\`java
ReportMetadata metadata = reportService.getMetadata("complex-report");
Dataset metricsDataset = metadata.getDatasetById("ds-metrics");
// Execute only this dataset
DataSet result = reportService.execute("complex-report", metricsDataset, params);
\`\`\`

### Execute All Datasets

\`\`\`java
Map<String, DataSet> results = reportService.executeAllDatasets("complex-report", params);
results.forEach((id, dataSet) -> {
    System.out.println("Dataset " + id + ": " + dataSet.size() + " rows");
});
\`\`\`
```

---

## Summary

**Total Scenarios**: 6 validation scenarios

**Test Classes**: 2 (ReportMetadataTest, MultiDatasetIntegrationTest)

**Expected Test Count**: +8 tests

**Run Time**: ~5-10 seconds for all validation scenarios

**Prerequisites**: Java 17+, Maven 3.6+, H2 (included) or SQL Server (optional)

**Success Criteria**: All 6 scenarios pass, no regressions, performance targets met

**Ready to implement**: All validation scenarios defined and testable
