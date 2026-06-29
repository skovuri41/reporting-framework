# Phase 3: Hierarchical JSON Mapping - Planning & Architecture

**Status:** Planning Phase
**Purpose:** Transform flat DataSets into complex hierarchical JSON structures

---

## Table of Contents

1. [Use Cases & Examples](#use-cases--examples)
2. [Implementation Strategy](#implementation-strategy)
3. [Complete Architecture](#complete-architecture)

---

# Use Cases & Examples

Using employee data from FullWorkflowIntegrationTest to show real transformations.

## 📊 Starting Point: Current DataSet Output

### Input Data (from database):
```sql
SELECT e.employee_id, e.name, e.salary, d.department_name
FROM employees e
JOIN departments d ON e.department_id = d.department_id
```

### Current DataSet JSON Output:
```json
[
  {
    "EMPLOYEE_ID": 1,
    "NAME": "Alice Johnson",
    "SALARY": 80000.00,
    "DEPARTMENT_NAME": "Engineering"
  },
  {
    "EMPLOYEE_ID": 2,
    "NAME": "Bob Smith",
    "SALARY": 90000.00,
    "DEPARTMENT_NAME": "Sales"
  }
]
```

**Problem:** Column names are database-style (UPPER_SNAKE_CASE), no nesting, no control over structure.

## Phase 1: Simple Field Mapping ✅

### Use Case: REST API Response

**Target JSON Schema:**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "id": {"type": "integer"},
      "fullName": {"type": "string"},
      "salary": {"type": "number"},
      "department": {"type": "string"}
    }
  }
}
```

### Mapping Code:
```java
// Simple field renaming
FieldMapper mapper = new FieldMapper()
    .rename("EMPLOYEE_ID", "id")
    .rename("NAME", "fullName")
    .rename("SALARY", "salary")
    .rename("DEPARTMENT_NAME", "department");

DataSet mapped = mapper.apply(employees);
String json = mapped.toJSON();
```

### Output JSON:
```json
[
  {
    "id": 1,
    "fullName": "Alice Johnson",
    "salary": 80000.00,
    "department": "Engineering"
  },
  {
    "id": 2,
    "fullName": "Bob Smith",
    "salary": 90000.00,
    "department": "Sales"
  }
]
```

**What Changed:**
✅ Field names match API conventions (camelCase)
✅ Column selection/filtering
✅ Works with existing DataSet structure

**What's Still Missing:**
❌ No nested objects
❌ No constant values
❌ No computed fields

## Phase 2: Nested Objects 🟡

### Use Case: Structured API Response with Grouping

**Target JSON Schema:**
```json
{
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "employee": {
        "type": "object",
        "properties": {
          "id": {"type": "integer"},
          "fullName": {"type": "string"}
        }
      },
      "employment": {
        "type": "object",
        "properties": {
          "department": {"type": "string"},
          "position": {"type": "string"}
        }
      },
      "compensation": {
        "type": "object",
        "properties": {
          "annualSalary": {"type": "number"},
          "currency": {"type": "string"}
        }
      }
    }
  }
}
```

### Mapping Code:
```java
NestedMapper mapper = new NestedMapper()
    // Map to nested employee object
    .mapNested("EMPLOYEE_ID", "employee.id")
    .mapNested("NAME", "employee.fullName")

    // Map to nested employment object
    .mapNested("DEPARTMENT_NAME", "employment.department")
    .mapConstant("employment.position", "Full-time")

    // Map to nested compensation object
    .mapNested("SALARY", "compensation.annualSalary")
    .mapConstant("compensation.currency", "USD");

String json = mapper.toNestedJSON(employees);
```

### Output JSON:
```json
[
  {
    "employee": {
      "id": 1,
      "fullName": "Alice Johnson"
    },
    "employment": {
      "department": "Engineering",
      "position": "Full-time"
    },
    "compensation": {
      "annualSalary": 80000.00,
      "currency": "USD"
    }
  }
]
```

**What Changed:**
✅ Nested object structures
✅ Logical grouping of related fields
✅ Constant value injection
✅ Dot-path notation (employee.id)

**What's Still Missing:**
❌ No grouping/aggregation into nested arrays
❌ Still one JSON object per DataSet row

## Phase 3: Grouped Nested Arrays 🔴

### Use Case: Hierarchical Report/Dashboard Data

**Scenario:** Departments as parent objects, with employees as nested arrays.

**Target JSON Schema:**
```json
{
  "type": "object",
  "properties": {
    "company": {
      "type": "object",
      "properties": {
        "name": {"type": "string"},
        "departments": {
          "type": "array",
          "items": {
            "type": "object",
            "properties": {
              "name": {"type": "string"},
              "budget": {"type": "number"},
              "headcount": {"type": "integer"},
              "employees": {
                "type": "array",
                "items": {
                  "type": "object",
                  "properties": {
                    "id": {"type": "integer"},
                    "name": {"type": "string"},
                    "salary": {"type": "number"}
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

### Mapping Code (Conceptual):
```java
HierarchicalMapper mapper = new HierarchicalMapper()
    // Root level
    .setRoot("company")
    .mapConstant("company.name", "Acme Corp")

    // Group by department (creates array)
    .groupBy("DEPARTMENT_ID", "company.departments[]")
    .mapNested("DEPARTMENT_NAME", "company.departments[].name")
    .mapNested("BUDGET", "company.departments[].budget")

    // Aggregate count for headcount
    .aggregate("EMPLOYEE_ID", "company.departments[].headcount", COUNT)

    // Nest employees within each department
    .nestArray("company.departments[].employees[]")
        .mapNested("EMPLOYEE_ID", "id")
        .mapNested("NAME", "name")
        .mapNested("SALARY", "salary");

String json = mapper.toHierarchicalJSON(employeesWithSales);
```

### Output JSON:
```json
{
  "company": {
    "name": "Acme Corp",
    "departments": [
      {
        "name": "Engineering",
        "budget": 500000.00,
        "headcount": 3,
        "employees": [
          {"id": 1, "name": "Alice Johnson", "salary": 80000.00},
          {"id": 3, "name": "Charlie Brown", "salary": 85000.00},
          {"id": 5, "name": "Eve Wilson", "salary": 82000.00}
        ]
      },
      {
        "name": "Sales",
        "budget": 750000.00,
        "headcount": 3,
        "employees": [
          {"id": 2, "name": "Bob Smith", "salary": 90000.00},
          {"id": 4, "name": "David Lee", "salary": 95000.00},
          {"id": 7, "name": "Grace Davis", "salary": 88000.00}
        ]
      }
    ]
  }
}
```

**What Changed:**
✅ Complete hierarchical structure
✅ Grouped data (departments contain employees)
✅ Nested arrays at multiple levels
✅ Aggregations within groups (headcount)
✅ Root object wrapping

## Side-by-Side Comparison

### Phase 1: Flat with Renamed Fields
```json
[
  {"id": 1, "fullName": "Alice Johnson", "salary": 80000, "department": "Engineering"}
]
```

### Phase 2: Nested Objects
```json
[
  {
    "employee": {"id": 1, "fullName": "Alice Johnson"},
    "employment": {"department": "Engineering"},
    "compensation": {"annualSalary": 80000, "currency": "USD"}
  }
]
```

### Phase 3: Hierarchical with Arrays
```json
{
  "company": {
    "departments": [
      {
        "name": "Engineering",
        "employees": [
          {"id": 1, "name": "Alice Johnson", "salary": 80000}
        ]
      }
    ]
  }
}
```

## Real-World Use Cases

### Phase 1: API Responses (Most Common)
**Who needs it:**
- REST API endpoints
- Mobile app backends
- Microservices communication
- Simple data exports

### Phase 2: Complex APIs
**Who needs it:**
- Enterprise APIs with structured responses
- GraphQL-like outputs
- Systems requiring logical field grouping

### Phase 3: Dashboards & Reports
**Who needs it:**
- Dashboard aggregations
- Hierarchical reports (org charts, etc.)
- Parent-child data relationships
- Complex BI exports

## Complexity vs Value Matrix

| Phase | Complexity | Value | Use Cases | Implementation Time |
|-------|-----------|-------|-----------|-------------------|
| **Phase 1** | ⭐ Low | ⭐⭐⭐⭐⭐ Very High | 80% of needs | 3 hours |
| **Phase 2** | ⭐⭐⭐ Medium | ⭐⭐⭐ High | 15% of needs | 1-2 days |
| **Phase 3** | ⭐⭐⭐⭐⭐ Very High | ⭐⭐ Medium | 5% of needs | 3-4 weeks |

## Decision Guide

### Choose Phase 1 if:
✅ You need clean field names (camelCase, different naming)
✅ Your API returns flat objects or simple arrays
✅ You want quick wins with minimal effort

### Choose Phase 2 if:
✅ Your API response has logical sections (user profile, settings, etc.)
✅ You need to add constant values (currency, version, etc.)
✅ Your schema has nested objects but not nested arrays

### Choose Phase 3 if:
✅ You need parent-child relationships (departments → employees)
✅ You're building hierarchical reports or org charts
✅ Your data model is truly tree-like
✅ You're willing to invest significant development time

## Recommended Path

### Start Here: Phase 1
**Why:**
- Immediate value
- Low risk
- Fast implementation
- Solves most real needs

### Add If Needed: Phase 2
**When:**
- After using Phase 1 for a while
- When you have specific nested object requirements

### Only If Required: Phase 3
**When:**
- You have explicit hierarchical data requirements
- Phase 2 solutions become too complex

---

# Implementation Strategy

## The Question

If Phase 3 (hierarchical mapping) can do everything Phase 1 & 2 can do, should we build Phase 3 first?

## TL;DR Answer

**Architecturally: YES** - Phase 3 is a superset, Phases 1 & 2 are simplified modes
**Practically: DEPENDS** - On your timeline, certainty, and risk tolerance

## The Architecture Truth

### Phase Relationships

```
┌─────────────────────────────────────────┐
│         Phase 3: Hierarchical           │
│    (Grouping + Nesting + Mapping)       │
│  ┌─────────────────────────────────┐   │
│  │    Phase 2: Nested Objects      │   │
│  │    (Nesting + Mapping)          │   │
│  │  ┌─────────────────────────┐   │   │
│  │  │   Phase 1: Renaming     │   │   │
│  │  │   (Simple Mapping)      │   │   │
│  │  └─────────────────────────┘   │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
```

**Truth:** Phase 1 & 2 ARE subsets of Phase 3.

### Phase 1 as Phase 3 with flags off:
```java
// Phase 1 behavior achieved via Phase 3 engine
HierarchicalMapper mapper = new HierarchicalMapper()
    .setGrouping(false)           // No grouping
    .setNesting(false)            // No nested objects
    .map("EMPLOYEE_ID", "id")     // Simple renames
    .map("NAME", "fullName");
```

### Phase 2 as Phase 3 with partial features:
```java
// Phase 2 behavior achieved via Phase 3 engine
HierarchicalMapper mapper = new HierarchicalMapper()
    .setGrouping(false)                    // No grouping
    .setNesting(true)                      // Enable nesting
    .mapNested("SALARY", "comp.salary")    // Nested paths
    .mapConstant("comp.currency", "USD");
```

## What Phase 3 Actually Requires

### Core Building Blocks (Needed for All Phases)

1. **Path Parser**
2. **Nested Object Builder**
3. **Type Converter**

### Phase 3 Unique Requirements

4. **Grouping Engine**
5. **Array Nesting Logic**
6. **Recursive Structure Builder**
7. **Aggregation Engine**

## The Real Question: Implementation Strategy

### Strategy A: Build Phase 3 First

**Timeline:** ~3-4 weeks (not 1 week)

**Pros:**
✅ One unified architecture
✅ No refactoring later
✅ Phases 1 & 2 are "free" (just configuration)
✅ More elegant/maintainable code

**Cons:**
❌ 3-4 weeks before ANY value
❌ Higher initial complexity = more bugs
❌ Over-engineering if you don't need all features

### Strategy B: Incremental (Phase 1 → 2 → 3)

**Timeline:**
```
Day 1-3: Phase 1 (field mapping)
→ USERS GET VALUE HERE ←

Week 2: Phase 2 (nested objects)
→ USERS GET MORE VALUE ←

Weeks 3-4: Phase 3 (grouping/hierarchy)
→ COMPLETE SOLUTION ←
```

**Pros:**
✅ Value in 3 days, not 3 weeks
✅ Learn from Phase 1 usage before building Phase 2
✅ Can stop at Phase 2 if Phase 3 not needed
✅ Lower risk (smaller pieces)

**Cons:**
❌ Possible refactoring between phases
❌ Risk of architectural mistakes early

## The Architectural Reality

Even if you commit to Phase 3, you'd **naturally build it incrementally**:

**Week 1:** Foundation + Path parser + Field mapper (essentially Phase 1)
**Week 2:** Nested object builder (essentially Phase 2)
**Week 3:** Grouping + Array nesting + Aggregations

**You'd still build Phases 1 & 2 as stepping stones!**

The difference is:
- **Incremental:** Release each step when done
- **Phase 3 First:** Don't release until all done

## When "Phase 3 First" Makes Sense

✅ **Build Phase 3 first if:**

1. **You're 100% certain you need hierarchical output**
2. **Timeline allows 3-4 weeks upfront**
3. **You have clear requirements**
4. **Architectural integrity matters most**

## When Incremental Makes Sense

✅ **Build incrementally if:**

1. **Need quick wins**
2. **Uncertain about Phase 3 need**
3. **Lower risk tolerance**
4. **Learning as you go**

## The Honest Complexity Assessment

**Phase 3 TRUE complexity:**

```
Core Path Engine:        2-3 days
Nested Object Builder:   2-3 days
Grouping Logic:          3-4 days
Array Nesting:           3-4 days
Aggregations:            2 days
Recursive Hierarchy:     3-4 days
Edge Cases:              2-3 days
Testing:                 3-4 days
Documentation:           1-2 days
───────────────────────────────
REALISTIC TOTAL:         3-4 weeks
```

**Why it's complex:**

1. **Array nesting with grouping is HARD**
2. **State management during build**
3. **Edge cases galore** (empty groups, nulls, missing fields)
4. **Performance** (grouping large datasets)

## My Recommendation

### Hybrid Approach: "Build for Phase 3, Release Incrementally"

**Strategy:**
1. **Design** the architecture for Phase 3 from day 1
2. **Implement** core path engine (works for all phases)
3. **Release** Phase 1 capability (3 days)
4. **Get feedback** from real usage
5. **Implement** nested objects
6. **Release** Phase 2 capability (1 week total)
7. **Complete** Phase 3 (3-4 weeks total)

**Best of both worlds:**
✅ Clean architecture (no refactoring)
✅ Quick value (Phase 1 in days)
✅ User feedback (guides Phase 2/3 design)
✅ Lower risk (test each step)

## The Decision Matrix

| Factor | Phase 3 First | Incremental | Hybrid |
|--------|---------------|-------------|---------|
| **Time to first value** | 3-4 weeks | 3 days | 3 days |
| **Architecture quality** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Risk level** | High | Low | Medium |
| **User feedback** | Late | Early | Early |
| **Refactoring needed** | None | Possible | Minimal |

## The Real Answer

**You're absolutely right that Phase 3 makes Phase 1 & 2 "simple" - they're just Phase 3 with features turned off.**

**BUT** - even building Phase 3, you'd implement the simple parts first as building blocks.

**SO** - the question isn't "Phase 3 first vs incremental", it's "release incrementally vs wait for completion?"

**My vote: Build for Phase 3, release incrementally.**

---

# Complete Architecture

**Commitment:** Full Phase 3 implementation with hierarchical grouping, nested arrays, and complete schema mapping.

**Timeline:** 3-4 weeks
**Outcome:** Transform flat DataSets into any JSON structure via declarative mapping

## Executive Summary

### What We're Building

A **declarative schema mapping engine** that transforms flat DataSet rows into complex hierarchical JSON structures.

**Input:** Flat DataSet
```
EMPLOYEE_ID | NAME    | DEPT_ID | DEPT_NAME    | SALARY
1           | Alice   | 10      | Engineering  | 80000
2           | Bob     | 20      | Sales        | 90000
```

**Output:** Hierarchical JSON
```json
{
  "company": {
    "departments": [
      {
        "id": 10,
        "name": "Engineering",
        "headcount": 2,
        "employees": [
          {"id": 1, "name": "Alice", "salary": 80000}
        ]
      }
    ]
  }
}
```

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
```

**API:**
```java
public class PathEngine {
    public List<PathSegment> parse(String path);
    public Object resolve(Map<String, Object> root, String path);
    public void set(Map<String, Object> root, String path, Object value);
    public boolean isArrayPath(String path);
    public String getParent(String path);
}
```

### 2. MappingSpec

**Responsibility:** Declarative specification of how to map data

**Structure:**
```java
public class MappingSpec {
    private String rootPath;
    private Map<String, Object> constants;
    private Map<String, String> fieldMappings;
    private Map<String, Function<DataRow, Object>> computed;
    private List<GroupingSpec> groupings;
    private List<AggregationSpec> aggregations;
}

public class GroupingSpec {
    private String groupByColumn;
    private String targetPath;
    private List<FieldMapping> mappings;
    private List<NestedArraySpec> nestedArrays;
}
```

### 3. GroupingEngine

**Responsibility:** Group DataSet rows by one or more keys

**API:**
```java
public class GroupingEngine {
    public Map<Object, List<DataRow>> groupBy(DataSet data, String groupByColumn);
    public Map<List<Object>, List<DataRow>> groupBy(DataSet data, List<String> groupByColumns);
}
```

### 4. NestedObjectBuilder

**Responsibility:** Build nested object structures from flat data

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
    "employee": {"id": 1, "name": "Alice"},
    "compensation": {"salary": 80000}
}
```

### 5. ArrayNestingEngine

**Responsibility:** Create nested arrays from grouped data

### 6. AggregationEngine

**Responsibility:** Calculate aggregations within groups

**API:**
```java
public class AggregationEngine {
    public Object aggregate(List<DataRow> rows, String column, AggregationFunction function);
    public Map<String, Object> aggregateMultiple(List<DataRow> rows, List<AggregationSpec> specs);
}

public enum AggregationFunction {
    COUNT, SUM, AVG, MIN, MAX, FIRST, LAST, DISTINCT_COUNT
}
```

### 7. TypeConverter

**Responsibility:** Convert values to target types

## High-Level API Design

### The User-Facing API

```java
// Example 1: Simple nested structure
HierarchicalMapper mapper = HierarchicalMapper.create()
    .map("EMPLOYEE_ID", "id")
    .mapNested("SALARY", "compensation.annualSalary")
    .mapConstant("compensation.currency", "USD");

// Example 2: Grouped with nested arrays
HierarchicalMapper mapper = HierarchicalMapper.create()
    .setRoot("company")
    .mapConstant("company.name", "Acme Corp")

    .groupBy("DEPARTMENT_ID", "company.departments[]")
        .map("DEPARTMENT_NAME", "name")
        .aggregate("EMPLOYEE_ID", "headcount", COUNT)

        .nestArray("employees[]")
            .map("EMPLOYEE_ID", "id")
            .map("NAME", "name")
        .endArray()
    .endGroup();

String json = mapper.transform(dataSet);
```

### Builder Pattern Implementation

```java
public class HierarchicalMapper {
    public static HierarchicalMapper create();
    public HierarchicalMapper setRoot(String rootPath);
    public HierarchicalMapper map(String sourceColumn, String targetPath);
    public HierarchicalMapper mapNested(String sourceColumn, String targetPath);
    public HierarchicalMapper mapConstant(String targetPath, Object value);
    public HierarchicalMapper mapComputed(String targetPath, Function<DataRow, Object> computer);
    public GroupByBuilder groupBy(String column, String targetPath);
    public String transform(DataSet dataSet);
}

public class GroupByBuilder {
    public GroupByBuilder map(String source, String target);
    public GroupByBuilder aggregate(String column, String targetPath, AggregationFunction fn);
    public NestArrayBuilder nestArray(String arrayPath);
    public HierarchicalMapper endGroup();
}
```

## Implementation Plan

### Week 1: Foundation

**Days 1-2: Core Infrastructure**
- [ ] PathEngine implementation
- [ ] MappingSpec data structures
- [ ] TypeConverter basics

**Days 3-4: Simple Mapping (Phase 1 equivalent)**
- [ ] NestedObjectBuilder implementation
- [ ] Simple field mapping
- [ ] Builder API (basic)
- [ ] **Milestone: Can do Phase 1 functionality**

**Day 5: Testing & Refinement**
- [ ] Integration tests
- [ ] Edge case testing

### Week 2: Nested Objects & Arrays

**Days 1-2: Nested Objects (Phase 2 equivalent)**
- [ ] Complete NestedObjectBuilder
- [ ] Constant value support
- [ ] Computed field support
- [ ] **Milestone: Can do Phase 2 functionality**

**Days 3-5: Array Support**
- [ ] ArrayNestingEngine implementation
- [ ] Single-level array nesting
- [ ] Array mapping logic

### Week 3: Grouping & Aggregation

**Days 1-2: Grouping Engine**
- [ ] GroupingEngine implementation
- [ ] Single-key and multi-key grouping
- [ ] Tests

**Days 3-4: Aggregation Engine**
- [ ] AggregationEngine implementation
- [ ] All aggregation functions
- [ ] Integration with grouping

**Day 5: Integration**
- [ ] Connect grouping + aggregation + nesting
- [ ] End-to-end test

### Week 4: Recursive Nesting & Polish

**Days 1-2: Multi-Level Nesting**
- [ ] Recursive grouping support
- [ ] Nested arrays within nested arrays
- [ ] Tests for 3+ level nesting

**Days 3-4: Edge Cases & Optimization**
- [ ] Handle empty groups, nulls, missing fields
- [ ] Performance optimization

**Day 5: Documentation & Examples**
- [ ] Complete API documentation
- [ ] Usage examples
- [ ] Integration tests

## File Structure

```
src/main/java/com/reporting/framework/schema/
├── HierarchicalMapper.java
├── builder/
│   ├── GroupByBuilder.java
│   ├── NestArrayBuilder.java
│   └── NestObjectBuilder.java
├── spec/
│   ├── MappingSpec.java
│   ├── GroupingSpec.java
│   ├── NestedArraySpec.java
│   └── AggregationSpec.java
├── engine/
│   ├── PathEngine.java
│   ├── GroupingEngine.java
│   ├── NestedObjectBuilder.java
│   ├── ArrayNestingEngine.java
│   ├── AggregationEngine.java
│   └── TypeConverter.java
└── path/
    ├── PathSegment.java
    └── PathParser.java
```

## Testing Strategy

### Unit Tests (Per Component)

```java
@Test
void testSimplePath() {
    assertEquals(List.of("id"), pathEngine.parse("id"));
}

@Test
void testGroupBySingleKey() {
    Map<Object, List<DataRow>> groups = groupingEngine.groupBy(employees, "DEPARTMENT_ID");
    assertEquals(3, groups.size());
}
```

### Integration Tests

```java
@Test
void testSimpleNested() {
    HierarchicalMapper mapper = HierarchicalMapper.create()
        .map("EMPLOYEE_ID", "id")
        .mapNested("SALARY", "compensation.salary")
        .mapConstant("compensation.currency", "USD");

    String json = mapper.transform(employees);
    // Verify structure
}

@Test
void testHierarchicalGrouping() {
    HierarchicalMapper mapper = HierarchicalMapper.create()
        .setRoot("company")
        .groupBy("DEPARTMENT_ID", "company.departments[]")
            .map("DEPARTMENT_NAME", "name")
            .aggregate("EMPLOYEE_ID", "headcount", COUNT)
            .nestArray("employees[]")
                .map("EMPLOYEE_ID", "id")
            .endArray()
        .endGroup();

    String json = mapper.transform(employees);
    // Verify hierarchy
}
```

## Performance Considerations

### Optimization Strategies

1. **Path Caching**
```java
private Map<String, List<PathSegment>> pathCache = new ConcurrentHashMap<>();
```

2. **Lazy Evaluation** - Use streams for large datasets

3. **Memory Management** - Consider streaming for very large datasets

4. **Parallelization** - Process independent groups in parallel

### Benchmarks to Track
- Small dataset (100 rows): < 10ms
- Medium dataset (10,000 rows): < 500ms
- Large dataset (100,000 rows): < 5s

## Migration Path

**Current DataSet.toJSON():** Still works, no changes needed

**New capability added:**
```java
// Old way (still works)
String json = dataSet.toJSON();

// New way (hierarchical)
HierarchicalMapper mapper = /* ... */;
String json = mapper.transform(dataSet);
```

## Success Criteria

### Week 1 Success
- [ ] Can rename fields (Phase 1)
- [ ] Can create nested objects (Phase 2)
- [ ] Clean API design
- [ ] 80% test coverage

### Week 2 Success
- [ ] Can nest arrays
- [ ] Can add constants
- [ ] All tests passing

### Week 3 Success
- [ ] Can group by keys
- [ ] Can aggregate within groups
- [ ] Can create 2-level hierarchies

### Week 4 Success (Final)
- [ ] Can create 3+ level hierarchies
- [ ] All edge cases handled
- [ ] Complete documentation

## Summary

**Phase 1** = 80% solution, 5% effort → **Start here**
**Phase 2** = 95% solution, 20% effort → Add if needed
**Phase 3** = 100% solution, 75% effort → Only if truly required

Most projects never need Phase 3. Many are happy with Phase 1 + manual nesting for edge cases.

---

**This is the complete architecture. Ready to build when you are!**
