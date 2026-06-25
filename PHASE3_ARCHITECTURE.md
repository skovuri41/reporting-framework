# Phase 3: Hierarchical Mapper - Complete Architecture

**Commitment:** Full Phase 3 implementation with hierarchical grouping, nested arrays, and complete schema mapping.

**Timeline:** 3-4 weeks
**Outcome:** Transform flat DataSets into any JSON structure via declarative mapping

---

## Executive Summary

### What We're Building

A **declarative schema mapping engine** that transforms flat DataSet rows into complex hierarchical JSON structures.

**Input:** Flat DataSet
```java
EMPLOYEE_ID | NAME    | DEPT_ID | DEPT_NAME    | SALARY | SALES
1           | Alice   | 10      | Engineering  | 80000  | null
2           | Bob     | 20      | Sales        | 90000  | 55000
3           | Charlie | 10      | Engineering  | 85000  | null
```

**Output:** Hierarchical JSON
```json
{
  "company": {
    "name": "Acme Corp",
    "departments": [
      {
        "id": 10,
        "name": "Engineering",
        "headcount": 2,
        "totalSalary": 165000,
        "employees": [
          {"id": 1, "name": "Alice", "salary": 80000},
          {"id": 3, "name": "Charlie", "salary": 85000}
        ]
      },
      {
        "id": 20,
        "name": "Sales",
        "headcount": 1,
        "totalSalary": 90000,
        "employees": [
          {
            "id": 2,
            "name": "Bob",
            "salary": 90000,
            "performance": {"sales": 55000}
          }
        ]
      }
    ]
  }
}
```

---

## Architecture Overview

### Component Hierarchy

```
HierarchicalMapper (Orchestrator)
├── PathEngine (Path parsing & resolution)
├── MappingSpec (Declarative mapping definition)
├── GroupingEngine (Groups rows by keys)
├── NestedObjectBuilder (Builds nested structures)
├── ArrayNestingEngine (Creates nested arrays)
├── AggregationEngine (Performs aggregations)
└── TypeConverter (Type conversions & validation)
```

### Data Flow

```
DataSet (flat rows)
    ↓
GroupingEngine → Groups by keys
    ↓
For each group:
    NestedObjectBuilder → Build nested objects
    ArrayNestingEngine → Nest child arrays
    AggregationEngine → Calculate aggregates
    ↓
Merge groups into hierarchy
    ↓
JSON output
```

---

## Core Components

### 1. PathEngine

**Responsibility:** Parse and resolve all path types

**Capabilities:**
```java
// Simple field
"id" → ["id"]

// Nested object
"employee.name" → ["employee", "name"]

// Array notation
"departments[]" → ["departments", ARRAY_MARKER]

// Nested arrays
"departments[].employees[]" → ["departments", ARRAY_MARKER, "employees", ARRAY_MARKER]

// Complex
"company.departments[].employees[].projects[].name"
```

**API:**
```java
public class PathEngine {
    // Parse path into segments
    public List<PathSegment> parse(String path);

    // Resolve value at path in nested structure
    public Object resolve(Map<String, Object> root, String path);

    // Set value at path, creating nested structure as needed
    public void set(Map<String, Object> root, String path, Object value);

    // Check if path contains array notation
    public boolean isArrayPath(String path);

    // Get parent path
    public String getParent(String path);
}

public class PathSegment {
    private String name;
    private boolean isArray;
    private boolean isRoot;
}
```

**Implementation Notes:**
- Use recursive descent parser for path syntax
- Handle escaped dots (if field names contain dots)
- Cache parsed paths for performance
- Validate path syntax during mapping definition

---

### 2. MappingSpec

**Responsibility:** Declarative specification of how to map data

**Structure:**
```java
public class MappingSpec {
    private String rootPath;                              // "company"
    private Map<String, Object> constants;                // Fixed values
    private Map<String, String> fieldMappings;            // Column → path
    private Map<String, Function<DataRow, Object>> computed; // Computed fields
    private List<GroupingSpec> groupings;                 // Grouping definitions
    private List<AggregationSpec> aggregations;          // Aggregations
}

public class GroupingSpec {
    private String groupByColumn;        // DataSet column to group by
    private String targetPath;           // Where to put grouped data
    private List<FieldMapping> mappings; // Field mappings within group
    private List<NestedArraySpec> nestedArrays; // Child arrays
}

public class NestedArraySpec {
    private String targetPath;           // Where to nest array
    private List<FieldMapping> mappings; // Field mappings for array items
    private List<NestedArraySpec> children; // Recursive nesting
}

public class AggregationSpec {
    private String sourceColumn;
    private String targetPath;
    private AggregationFunction function;
}
```

---

### 3. GroupingEngine

**Responsibility:** Group DataSet rows by one or more keys

**API:**
```java
public class GroupingEngine {
    // Group by single key
    public Map<Object, List<DataRow>> groupBy(
        DataSet data,
        String groupByColumn
    );

    // Group by multiple keys (for multi-level nesting)
    public Map<List<Object>, List<DataRow>> groupBy(
        DataSet data,
        List<String> groupByColumns
    );

    // Group with ordering (preserve order within groups)
    public LinkedHashMap<Object, List<DataRow>> groupByOrdered(
        DataSet data,
        String groupByColumn,
        Comparator<DataRow> rowOrder
    );
}
```

**Implementation:**
```java
public Map<Object, List<DataRow>> groupBy(DataSet data, String column) {
    return data.getRows().stream()
        .collect(Collectors.groupingBy(
            row -> row.get(column),
            LinkedHashMap::new,  // Preserve insertion order
            Collectors.toList()
        ));
}
```

---

### 4. NestedObjectBuilder

**Responsibility:** Build nested object structures from flat data

**API:**
```java
public class NestedObjectBuilder {
    // Build nested structure from flat map
    public Map<String, Object> buildNested(
        Map<String, Object> flatData,
        Map<String, String> pathMappings
    );

    // Add value at nested path
    public void setNested(
        Map<String, Object> target,
        String path,
        Object value
    );

    // Merge multiple nested structures
    public Map<String, Object> merge(
        List<Map<String, Object>> structures
    );
}
```

**Example:**
```java
// Input
flatData = {
    "EMPLOYEE_ID": 1,
    "NAME": "Alice",
    "SALARY": 80000
}

pathMappings = {
    "EMPLOYEE_ID": "employee.id",
    "NAME": "employee.name",
    "SALARY": "compensation.salary"
}

// Output
{
    "employee": {
        "id": 1,
        "name": "Alice"
    },
    "compensation": {
        "salary": 80000
    }
}
```

**Algorithm:**
```java
public Map<String, Object> buildNested(Map<String, Object> flat, Map<String, String> mappings) {
    Map<String, Object> result = new LinkedHashMap<>();

    for (Map.Entry<String, String> mapping : mappings.entrySet()) {
        String sourceColumn = mapping.getKey();
        String targetPath = mapping.getValue();
        Object value = flat.get(sourceColumn);

        setNested(result, targetPath, value);
    }

    return result;
}

private void setNested(Map<String, Object> target, String path, Object value) {
    List<PathSegment> segments = pathEngine.parse(path);

    Map<String, Object> current = target;
    for (int i = 0; i < segments.size() - 1; i++) {
        String segment = segments.get(i).getName();
        current.putIfAbsent(segment, new LinkedHashMap<>());
        current = (Map<String, Object>) current.get(segment);
    }

    String finalKey = segments.get(segments.size() - 1).getName();
    current.put(finalKey, value);
}
```

---

### 5. ArrayNestingEngine

**Responsibility:** Create nested arrays from grouped data

**API:**
```java
public class ArrayNestingEngine {
    // Convert grouped rows into nested array structure
    public List<Map<String, Object>> nestArray(
        List<DataRow> rows,
        NestedArraySpec spec
    );

    // Handle recursive nesting (arrays within arrays)
    public List<Map<String, Object>> nestRecursive(
        List<DataRow> rows,
        List<NestedArraySpec> specs,
        int depth
    );
}
```

**Example:**
```java
// Input: Grouped employees
List<DataRow> engineeringEmployees = [
    {EMPLOYEE_ID: 1, NAME: "Alice", SALARY: 80000},
    {EMPLOYEE_ID: 3, NAME: "Charlie", SALARY: 85000}
]

NestedArraySpec spec = new NestedArraySpec()
    .map("EMPLOYEE_ID", "id")
    .map("NAME", "name")
    .map("SALARY", "salary");

// Output
[
    {"id": 1, "name": "Alice", "salary": 80000},
    {"id": 3, "name": "Charlie", "salary": 85000}
]
```

---

### 6. AggregationEngine

**Responsibility:** Calculate aggregations within groups

**API:**
```java
public class AggregationEngine {
    // Calculate single aggregation
    public Object aggregate(
        List<DataRow> rows,
        String column,
        AggregationFunction function
    );

    // Calculate multiple aggregations
    public Map<String, Object> aggregateMultiple(
        List<DataRow> rows,
        List<AggregationSpec> specs
    );

    // Custom aggregation with lambda
    public Object aggregateCustom(
        List<DataRow> rows,
        Function<List<DataRow>, Object> aggregator
    );
}

public enum AggregationFunction {
    COUNT,
    SUM,
    AVG,
    MIN,
    MAX,
    FIRST,
    LAST,
    DISTINCT_COUNT
}
```

**Integration with grouping:**
```java
// For each group, calculate aggregations
for (Map.Entry<Object, List<DataRow>> group : groups.entrySet()) {
    List<DataRow> rows = group.getValue();

    // Calculate headcount
    long count = aggregationEngine.aggregate(rows, "EMPLOYEE_ID", COUNT);

    // Calculate total salary
    double total = aggregationEngine.aggregate(rows, "SALARY", SUM);

    // Add to group result
    groupResult.put("headcount", count);
    groupResult.put("totalSalary", total);
}
```

---

### 7. TypeConverter

**Responsibility:** Convert values to target types

**API:**
```java
public class TypeConverter {
    // Convert based on JSON Schema type
    public Object convert(Object value, JsonSchemaType targetType);

    // Convert with custom converter
    public Object convert(Object value, Function<Object, Object> converter);

    // Validate type compatibility
    public boolean isCompatible(Object value, JsonSchemaType type);
}
```

---

## High-Level API Design

### The User-Facing API

```java
// Example 1: Simple nested structure
HierarchicalMapper mapper = HierarchicalMapper.create()
    .map("EMPLOYEE_ID", "id")
    .map("NAME", "fullName")
    .mapNested("SALARY", "compensation.annualSalary")
    .mapConstant("compensation.currency", "USD");

String json = mapper.transform(dataSet);

// Example 2: Grouped with nested arrays
HierarchicalMapper mapper = HierarchicalMapper.create()
    .setRoot("company")
    .mapConstant("company.name", "Acme Corp")

    // Group by department
    .groupBy("DEPARTMENT_ID", "company.departments[]")
        .map("DEPARTMENT_NAME", "name")
        .map("BUDGET", "budget")
        .aggregate("EMPLOYEE_ID", "headcount", COUNT)
        .aggregate("SALARY", "totalSalary", SUM)

        // Nest employees within each department
        .nestArray("employees[]")
            .map("EMPLOYEE_ID", "id")
            .map("NAME", "name")
            .map("SALARY", "salary")
        .endArray()
    .endGroup();

String json = mapper.transform(dataSet);

// Example 3: Multi-level nesting
HierarchicalMapper mapper = HierarchicalMapper.create()
    .groupBy("DEPARTMENT_ID", "departments[]")
        .map("DEPARTMENT_NAME", "name")

        .nestArray("employees[]")
            .map("EMPLOYEE_ID", "id")
            .map("NAME", "name")

            // Further nest projects per employee
            .groupBy("PROJECT_ID", "projects[]")
                .map("PROJECT_NAME", "name")
                .map("HOURS", "hoursWorked")
            .endGroup()
        .endArray()
    .endGroup();
```

### Builder Pattern Implementation

```java
public class HierarchicalMapper {
    private MappingSpec spec;
    private PathEngine pathEngine;
    private GroupingEngine groupingEngine;
    private NestedObjectBuilder objectBuilder;
    private ArrayNestingEngine arrayEngine;
    private AggregationEngine aggEngine;

    // Factory method
    public static HierarchicalMapper create() {
        return new HierarchicalMapper();
    }

    // Root configuration
    public HierarchicalMapper setRoot(String rootPath) {
        spec.setRootPath(rootPath);
        return this;
    }

    // Simple field mapping
    public HierarchicalMapper map(String sourceColumn, String targetPath) {
        spec.addFieldMapping(sourceColumn, targetPath);
        return this;
    }

    // Nested field mapping (alias for map, clearer intent)
    public HierarchicalMapper mapNested(String sourceColumn, String targetPath) {
        return map(sourceColumn, targetPath);
    }

    // Constant value
    public HierarchicalMapper mapConstant(String targetPath, Object value) {
        spec.addConstant(targetPath, value);
        return this;
    }

    // Computed field
    public HierarchicalMapper mapComputed(
        String targetPath,
        Function<DataRow, Object> computer
    ) {
        spec.addComputed(targetPath, computer);
        return this;
    }

    // Start grouping
    public GroupByBuilder groupBy(String column, String targetPath) {
        return new GroupByBuilder(this, column, targetPath);
    }

    // Execute transformation
    public String transform(DataSet dataSet) {
        Map<String, Object> result = executeMapping(dataSet);
        return toJSON(result);
    }
}

// Builder for group definitions
public class GroupByBuilder {
    private HierarchicalMapper parent;
    private GroupingSpec groupSpec;

    public GroupByBuilder map(String source, String target) {
        groupSpec.addMapping(source, target);
        return this;
    }

    public GroupByBuilder aggregate(
        String column,
        String targetPath,
        AggregationFunction fn
    ) {
        groupSpec.addAggregation(column, targetPath, fn);
        return this;
    }

    public NestArrayBuilder nestArray(String arrayPath) {
        return new NestArrayBuilder(this, arrayPath);
    }

    public HierarchicalMapper endGroup() {
        parent.addGrouping(groupSpec);
        return parent;
    }
}

// Builder for nested arrays
public class NestArrayBuilder {
    private GroupByBuilder parent;
    private NestedArraySpec arraySpec;

    public NestArrayBuilder map(String source, String target) {
        arraySpec.addMapping(source, target);
        return this;
    }

    public NestObjectBuilder nestObject(String objectPath) {
        return new NestObjectBuilder(this, objectPath);
    }

    public GroupByBuilder groupBy(String column, String targetPath) {
        // Recursive grouping within array
        return new GroupByBuilder(this, column, targetPath);
    }

    public GroupByBuilder endArray() {
        parent.addNestedArray(arraySpec);
        return parent;
    }
}
```

---

## Implementation Plan

### Week 1: Foundation

**Days 1-2: Core Infrastructure**
- [ ] PathEngine implementation
  - Path parsing logic
  - Path resolution
  - Unit tests for all path types
- [ ] MappingSpec data structures
  - All spec classes
  - Validation logic
- [ ] TypeConverter basics

**Days 3-4: Simple Mapping (Phase 1 equivalent)**
- [ ] NestedObjectBuilder implementation
- [ ] Simple field mapping
  - Flat field to flat field
  - Flat field to nested path
- [ ] Builder API (basic)
- [ ] Unit tests
- [ ] **Milestone: Can do Phase 1 functionality**

**Day 5: Testing & Refinement**
- [ ] Integration tests with FullWorkflowIntegrationTest data
- [ ] Edge case testing
- [ ] Performance testing
- [ ] Documentation

---

### Week 2: Nested Objects & Arrays

**Days 1-2: Nested Objects (Phase 2 equivalent)**
- [ ] Complete NestedObjectBuilder
- [ ] Constant value support
- [ ] Computed field support
- [ ] Nested object tests
- [ ] **Milestone: Can do Phase 2 functionality**

**Days 3-5: Array Support**
- [ ] ArrayNestingEngine implementation
- [ ] Single-level array nesting
- [ ] Array mapping logic
- [ ] Array builder API
- [ ] Tests with real data

---

### Week 3: Grouping & Aggregation

**Days 1-2: Grouping Engine**
- [ ] GroupingEngine implementation
- [ ] Single-key grouping
- [ ] Multi-key grouping
- [ ] Ordered grouping
- [ ] Tests

**Days 3-4: Aggregation Engine**
- [ ] AggregationEngine implementation
- [ ] All aggregation functions (COUNT, SUM, AVG, MIN, MAX)
- [ ] Custom aggregations
- [ ] Integration with grouping
- [ ] Tests

**Day 5: Integration**
- [ ] Connect grouping + aggregation + nesting
- [ ] End-to-end test: departments with employees
- [ ] Debugging & fixes

---

### Week 4: Recursive Nesting & Polish

**Days 1-2: Multi-Level Nesting**
- [ ] Recursive grouping support
- [ ] Nested arrays within nested arrays
- [ ] Complex hierarchy builder
- [ ] Tests for 3+ level nesting

**Days 3-4: Edge Cases & Optimization**
- [ ] Handle empty groups
- [ ] Handle null values
- [ ] Handle missing fields
- [ ] Performance optimization
- [ ] Memory efficiency for large datasets

**Day 5: Documentation & Examples**
- [ ] Complete API documentation
- [ ] Usage examples for common scenarios
- [ ] Integration with existing FullWorkflowIntegrationTest
- [ ] Performance benchmarks
- [ ] README updates

---

## File Structure

```
src/main/java/com/reporting/framework/schema/
├── HierarchicalMapper.java           # Main API
├── builder/
│   ├── GroupByBuilder.java           # Group definition builder
│   ├── NestArrayBuilder.java         # Array nesting builder
│   └── NestObjectBuilder.java        # Object nesting builder
├── spec/
│   ├── MappingSpec.java              # Mapping specification
│   ├── GroupingSpec.java             # Grouping specification
│   ├── NestedArraySpec.java          # Array nesting spec
│   ├── AggregationSpec.java          # Aggregation spec
│   └── FieldMapping.java             # Field mapping definition
├── engine/
│   ├── PathEngine.java               # Path parsing & resolution
│   ├── GroupingEngine.java           # Row grouping
│   ├── NestedObjectBuilder.java      # Nested object construction
│   ├── ArrayNestingEngine.java       # Array nesting
│   ├── AggregationEngine.java        # Aggregations
│   └── TypeConverter.java            # Type conversions
└── path/
    ├── PathSegment.java              # Path segment representation
    └── PathParser.java               # Path syntax parser

src/test/java/com/reporting/framework/schema/
├── HierarchicalMapperTest.java       # Main integration tests
├── PathEngineTest.java               # Path parsing tests
├── GroupingEngineTest.java           # Grouping tests
├── NestedObjectBuilderTest.java      # Nesting tests
├── ArrayNestingEngineTest.java       # Array tests
├── AggregationEngineTest.java        # Aggregation tests
└── examples/
    ├── SimpleNestedExample.java      # Phase 2 equivalent
    └── HierarchicalExample.java      # Phase 3 examples
```

---

## Testing Strategy

### Unit Tests (Per Component)

**PathEngine:**
```java
@Test
void testSimplePath() {
    assertEquals(List.of("id"), pathEngine.parse("id"));
}

@Test
void testNestedPath() {
    assertEquals(
        List.of("employee", "name"),
        pathEngine.parse("employee.name")
    );
}

@Test
void testArrayPath() {
    List<PathSegment> segments = pathEngine.parse("departments[]");
    assertTrue(segments.get(1).isArray());
}
```

**GroupingEngine:**
```java
@Test
void testGroupBySingleKey() {
    Map<Object, List<DataRow>> groups =
        groupingEngine.groupBy(employees, "DEPARTMENT_ID");

    assertEquals(3, groups.size()); // 3 departments
    assertEquals(3, groups.get(10).size()); // 3 in Engineering
}
```

### Integration Tests

**Test 1: Simple Nested (Phase 2 equivalent):**
```java
@Test
void testSimpleNested() {
    HierarchicalMapper mapper = HierarchicalMapper.create()
        .map("EMPLOYEE_ID", "id")
        .mapNested("SALARY", "compensation.salary")
        .mapConstant("compensation.currency", "USD");

    String json = mapper.transform(employees);

    // Verify structure
    JsonNode result = objectMapper.readTree(json);
    assertEquals(1, result.get(0).get("id").asInt());
    assertEquals(80000, result.get(0).get("compensation").get("salary").asInt());
}
```

**Test 2: Hierarchical (Phase 3):**
```java
@Test
void testHierarchicalGrouping() {
    HierarchicalMapper mapper = HierarchicalMapper.create()
        .setRoot("company")
        .groupBy("DEPARTMENT_ID", "company.departments[]")
            .map("DEPARTMENT_NAME", "name")
            .aggregate("EMPLOYEE_ID", "headcount", COUNT)
            .nestArray("employees[]")
                .map("EMPLOYEE_ID", "id")
                .map("NAME", "name")
            .endArray()
        .endGroup();

    String json = mapper.transform(employees);

    // Verify structure
    JsonNode company = objectMapper.readTree(json).get("company");
    JsonNode depts = company.get("departments");

    assertEquals(3, depts.size()); // 3 departments

    JsonNode engineering = depts.get(0);
    assertEquals("Engineering", engineering.get("name").asText());
    assertEquals(3, engineering.get("headcount").asInt());
    assertEquals(3, engineering.get("employees").size());
}
```

**Test 3: Multi-Level Nesting:**
```java
@Test
void testMultiLevelNesting() {
    // Test departments -> employees -> projects
    // 3 levels of nesting
}
```

---

## Performance Considerations

### Optimization Strategies

1. **Path Caching**
```java
private Map<String, List<PathSegment>> pathCache = new ConcurrentHashMap<>();

public List<PathSegment> parse(String path) {
    return pathCache.computeIfAbsent(path, this::doParse);
}
```

2. **Lazy Evaluation**
```java
// Don't build entire structure if only partial needed
// Use streams for large datasets
```

3. **Memory Management**
```java
// For very large datasets (>100k rows)
// Consider streaming approach instead of in-memory
public Stream<Map<String, Object>> transformStream(DataSet data) {
    // Process groups one at a time
}
```

4. **Parallelization**
```java
// For independent groups, process in parallel
groups.parallelStream()
    .map(this::processGroup)
    .collect(Collectors.toList());
```

### Benchmarks to Track

- Small dataset (100 rows): < 10ms
- Medium dataset (10,000 rows): < 500ms
- Large dataset (100,000 rows): < 5s
- Nested 3 levels deep: 2x base time
- Memory usage: < 3x dataset size

---

## Migration Path

### How Existing Code Continues to Work

**Current DataSet.toJSON():** Still works, no changes needed

**New capability added:**
```java
// Old way (still works)
String json = dataSet.toJSON();

// New way (hierarchical)
HierarchicalMapper mapper = /* ... */;
String json = mapper.transform(dataSet);
```

### Integration with FullWorkflowIntegrationTest

Add new test scenarios:
```java
@Test
void testHierarchicalOutput() {
    DataSet employees = createDataSetFromQuery("SELECT * FROM employees", "employees");
    DataSet departments = createDataSetFromQuery("SELECT * FROM departments", "departments");

    // Join first
    DataSet joined = DataOperations.innerJoin(employees, departments, "DEPARTMENT_ID", "DEPARTMENT_ID");

    // Transform to hierarchy
    HierarchicalMapper mapper = HierarchicalMapper.create()
        .groupBy("DEPARTMENT_ID", "departments[]")
            .map("DEPARTMENT_NAME", "name")
            .nestArray("employees[]")
                .map("NAME", "name")
                .map("SALARY", "salary")
            .endArray()
        .endGroup();

    String json = mapper.transform(joined);

    // Verify hierarchical structure
    assertThat(json).contains("\"departments\"");
    assertThat(json).contains("\"employees\"");
}
```

---

## Risk Mitigation

### Technical Risks

**Risk 1: Complexity Explosion**
- Mitigation: Build incrementally, test each component
- Fallback: Simplify API if too complex

**Risk 2: Performance Issues**
- Mitigation: Benchmark early and often
- Fallback: Add streaming mode for large datasets

**Risk 3: Edge Cases**
- Mitigation: Comprehensive test suite
- Focus on: nulls, empty groups, missing fields

### Timeline Risks

**Risk: Takes longer than 4 weeks**
- Mitigation: Weekly milestones, cut scope if needed
- Week 1 milestone: Phase 1 equivalent working
- Week 2 milestone: Phase 2 equivalent working
- Can ship at any milestone if needed

---

## Success Criteria

### Week 1 Success
- [ ] Can rename fields (Phase 1)
- [ ] Can create nested objects (Phase 2)
- [ ] Clean API design
- [ ] 80% test coverage

### Week 2 Success
- [ ] Can nest arrays
- [ ] Can add constants
- [ ] Can compute fields
- [ ] All tests passing

### Week 3 Success
- [ ] Can group by keys
- [ ] Can aggregate within groups
- [ ] Can create 2-level hierarchies
- [ ] Performance acceptable

### Week 4 Success (Final)
- [ ] Can create 3+ level hierarchies
- [ ] All edge cases handled
- [ ] Performance optimized
- [ ] Complete documentation
- [ ] Integration tests with real data

---

## Next Steps

### Immediate Actions

1. **Review & Approve Architecture**
   - Does this design meet your needs?
   - Any changes to API design?
   - Any missing features?

2. **Confirm Timeline**
   - 4 weeks acceptable?
   - Any hard deadlines?
   - Want to ship incrementally?

3. **Start Implementation**
   - Begin with PathEngine (Day 1)
   - Get foundation solid
   - Build up from there

### Questions for You

1. **Does the API design look right?**
   - Is the builder pattern intuitive?
   - Any syntax preferences?

2. **Any specific JSON schemas you need to support?**
   - Share examples so we can test against them

3. **Performance requirements?**
   - How large are your datasets?
   - Response time requirements?

4. **Ready to start?**
   - Shall we begin with PathEngine implementation?

---

**This is the complete architecture. Ready to build when you are!**
