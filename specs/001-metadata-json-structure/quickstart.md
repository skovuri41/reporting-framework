# Quickstart: Report Metadata JSON Structure

**Feature**: Report Metadata JSON Structure
**Date**: 2026-07-07
**Purpose**: Validate that the metadata JSON structure works end-to-end

## Overview

This guide provides runnable validation scenarios to verify the report metadata JSON structure feature. Follow these scenarios in order to validate the complete implementation.

## Prerequisites

- Java 17+ installed
- Maven 3.6+ installed
- Project compiled: `mvn clean compile`
- H2 test database initialized automatically by tests

## Validation Scenarios

### Scenario 1: Validate JSON Schema

**Objective**: Verify that the JSON Schema is valid and example files conform to it.

**Setup**:
```bash
# Install JSON Schema validator (one-time setup)
npm install -g ajv-cli

# Navigate to contracts directory
cd specs/001-metadata-json-structure/contracts/
```

**Execute**:
```bash
# Validate single report example
ajv validate -s metadata-schema.json -d report-example.json

# Validate catalog example
ajv validate -s metadata-schema.json -d catalog-example.json
```

**Expected Outcome**:
```
report-example.json valid
catalog-example.json valid
```

**Validation Checklist**:
- [ ] Schema file is valid JSON Schema Draft 7
- [ ] Report example validates successfully
- [ ] Catalog example validates successfully
- [ ] Schema enforces required fields (reportId, reportName, datasets, etc.)
- [ ] Schema validates enum values (parameterDirection: "Input"/"Output")

---

### Scenario 2: Test JSON Serialization/Deserialization

**Objective**: Verify Jackson can serialize and deserialize metadata DTOs correctly.

**Setup**:
```bash
# Ensure code is compiled
mvn clean compile
```

**Execute**:
```bash
# Run unit tests for JSON serialization
mvn test -Dtest=MetadataJsonTest
```

**Expected Outcome**:
- All tests pass
- Console shows successful serialization of ReportMetadata, Dataset, Parameter, Column
- Deserialization produces identical objects (round-trip test passes)

**Validation Checklist**:
- [ ] ReportMetadata serializes to correct JSON structure
- [ ] Dataset, Parameter, Column nested correctly
- [ ] ParameterDirection enum serializes to "Input"/"Output" strings
- [ ] Null fields handled correctly (reportDescription nullable)
- [ ] Empty lists serialize to [] not null
- [ ] Round-trip serialization preserves all data

---

### Scenario 3: Test Metadata Loading from Database

**Objective**: Verify MetadataLoader can load metadata from H2 database and parse JSON.

**Setup**:
```bash
# Ensure test schema is up-to-date
cat src/test/resources/test-schema.sql
```

**Execute**:
```bash
# Run metadata loader integration test
mvn test -Dtest=MetadataIntegrationTest#testMetadataLoading
```

**Expected Outcome**:
- Test passes
- Console shows metadata loaded from database
- JSON parsed into ReportMetadata object
- All datasets, parameters, and columns accessible via getters

**Validation Checklist**:
- [ ] Test data inserted into REPORT_METADATA table
- [ ] MetadataLoader.load(reportId) returns ReportMetadata
- [ ] Datasets list populated correctly
- [ ] Parameters and columns accessible
- [ ] No manual casting required (type-safe getters work)

---

### Scenario 4: Test Catalog Retrieval

**Objective**: Verify catalog retrieval returns all reports efficiently.

**Setup**:
```bash
# Run catalog integration test
mvn test -Dtest=MetadataIntegrationTest#testCatalogRetrieval
```

**Expected Outcome**:
- Test passes
- Catalog contains all test reports
- count field matches reports.size()
- Retrieval completes in <1 second for 3 test reports

**Validation Checklist**:
- [ ] getCatalog() returns ReportCatalog
- [ ] All reports included in catalog
- [ ] count field accurate
- [ ] No duplicate reports
- [ ] Performance acceptable (<1s)

---

### Scenario 5: Test JSON Schema Validation

**Objective**: Verify that invalid metadata is rejected with clear error messages.

**Setup**:
```bash
# Run schema validation tests
mvn test -Dtest=JsonSchemaValidationTest
```

**Expected Outcome**:
- Tests pass
- Invalid JSON rejected with specific error messages
- Missing required fields detected
- Invalid enum values caught
- Invalid data types rejected

**Test Cases**:
```java
// Missing required field
{ "reportId": "001", "reportName": "Test" }  // Missing datasets
→ Error: "datasets is required"

// Invalid parameter direction
{ "parameterDirection": "INVALID" }
→ Error: "parameterDirection must be 'Input' or 'Output'"

// Invalid data type
{ "dataType": "UNSUPPORTED" }
→ Error: "dataType must be one of: INTEGER, VARCHAR, ..."
```

**Validation Checklist**:
- [ ] Missing reportId detected
- [ ] Missing datasets detected
- [ ] Invalid parameterDirection rejected
- [ ] Invalid dataType rejected
- [ ] Empty datasets array allowed
- [ ] Validation errors are descriptive

---

### Scenario 6: Test MetadataNotFoundException

**Objective**: Verify proper error handling for non-existent report IDs.

**Setup**:
```bash
# Run error handling test
mvn test -Dtest=MetadataLoaderTest#testReportNotFound
```

**Expected Outcome**:
- Test passes by expecting MetadataNotFoundException
- Exception message includes the requested reportId
- No NullPointerException or generic errors

**Validation Checklist**:
- [ ] MetadataNotFoundException thrown for invalid reportId
- [ ] Exception message includes reportId
- [ ] No stack traces for expected errors
- [ ] Proper exception hierarchy (extends ReportingException)

---

### Scenario 7: End-to-End Integration Test

**Objective**: Validate complete flow from database to cached metadata to retrieval.

**Setup**:
```bash
# Run full integration test suite
mvn test -Dtest=MetadataIntegrationTest
```

**Expected Outcome**:
- All integration tests pass
- Metadata loaded once and cached
- Subsequent retrievals use cache (no database queries)
- Performance meets requirements (<100ms single report, <1s catalog)

**Flow Validated**:
```
1. INSERT test metadata into REPORT_METADATA table
2. MetadataLoader.load(reportId)
   → SELECT from database
   → Validate against JSON Schema
   → Deserialize with Jackson
   → Cache in memory
3. Second call to load(reportId)
   → Retrieved from cache (no database call)
4. getCatalog()
   → Returns all cached reports
```

**Validation Checklist**:
- [ ] Database schema created correctly
- [ ] Test data inserted successfully
- [ ] First load hits database
- [ ] Second load uses cache
- [ ] Catalog aggregates all reports
- [ ] Performance within targets

---

## Manual Validation (Optional)

### Inspect Generated JSON

If you want to manually inspect the JSON output:

```bash
# Run a test that outputs JSON to console
mvn test -Dtest=MetadataJsonTest#testSerializeToJson

# Or use Java code in a scratch file
public class MetadataExample {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // Read example file
        File file = new File("specs/001-metadata-json-structure/contracts/report-example.json");
        ReportMetadata metadata = mapper.readValue(file, ReportMetadata.class);

        // Print pretty JSON
        String json = mapper.writerWithDefaultPrettyPrinter()
                           .writeValueAsString(metadata);
        System.out.println(json);

        // Access data
        System.out.println("Report ID: " + metadata.getReportId());
        System.out.println("Datasets: " + metadata.getDatasets().size());
    }
}
```

---

## Performance Validation

### Measure Metadata Loading Performance

```bash
# Run performance test
mvn test -Dtest=MetadataPerformanceTest
```

**Expected Results**:
- Single report load: <100ms
- Catalog load (100 reports): <1 second
- JSON parsing overhead: <50ms per report
- Cache retrieval: <1ms

**Performance Test Code** (example):
```java
@Test
public void testSingleReportLoadPerformance() {
    long start = System.currentTimeMillis();
    ReportMetadata metadata = metadataLoader.load("employee-001");
    long duration = System.currentTimeMillis() - start;

    assertThat(duration).isLessThan(100); // <100ms
}

@Test
public void testCatalogLoadPerformance() {
    // Insert 100 test reports
    insertTestReports(100);

    long start = System.currentTimeMillis();
    ReportCatalog catalog = metadataRepository.getCatalog();
    long duration = System.currentTimeMillis() - start;

    assertThat(catalog.getCount()).isEqualTo(100);
    assertThat(duration).isLessThan(1000); // <1 second
}
```

---

## Troubleshooting

### Issue: JSON Schema validation fails

**Solution**: Check that example JSON files match the schema exactly. Common issues:
- Missing required fields
- Typos in field names (camelCase sensitive)
- Wrong enum values ("Input" not "input")

### Issue: Jackson deserialization fails

**Solution**: Ensure DTOs have:
- `@JsonCreator` on constructor
- `@JsonProperty` on all constructor parameters
- Matching field names in JSON and Java

### Issue: H2 test data not loading

**Solution**: Check `src/test/resources/test-schema.sql`:
- Table created with correct schema
- Sample INSERT statements valid
- JSON strings properly escaped

### Issue: Performance tests fail

**Solution**:
- Check H2 is running in-memory (fast mode)
- Verify caching is enabled
- Ensure JSON files aren't too large

---

## Success Indicators

✅ All 7 validation scenarios pass
✅ JSON Schema validates all examples
✅ Jackson serialization round-trips correctly
✅ Metadata loads from database successfully
✅ Catalog retrieval works
✅ Schema validation catches errors
✅ Performance meets targets (<100ms, <1s)

---

## Next Steps After Validation

Once all scenarios pass:
1. Review generated JSON in `target/` directory
2. Verify CLAUDE.md updated with new metadata structure documentation
3. Update existing reports to new format (if applicable)
4. Create tasks for implementation using `/speckit-tasks`

---

## References

- Data Model: [data-model.md](data-model.md)
- JSON Schema: [contracts/metadata-schema.json](contracts/metadata-schema.json)
- Example JSONs: [contracts/](contracts/)
- Research: [research.md](research.md)
- Plan: [plan.md](plan.md)
