# Transformation Architecture Research

**Date:** 2024-06-25
**Topic:** Alternative patterns to Fluent DSL and advanced transformation architectures

---

## Executive Summary

Research into alternative patterns for the DataQuery fluent DSL, exploring declarative specifications, lazy evaluation, and a revolutionary two-stage pipeline combining data transformation with schema-driven JSON output.

**Key Finding:** The optimal approach combines:
1. **Lazy Fluent DSL** - Builds execution plan (like Spark/LINQ)
2. **Declarative Specification** - JSON specs for non-developers
3. **Schema-Driven Output** - JSON schema guides hierarchical output generation
4. **Bidirectional Conversion** - Fluent ↔ JSON ↔ Schema

---

## Part 1: Alternatives to Fluent DSL

### Current Pattern: Fluent DSL (Method Chaining)

**What we have:**
```java
DataSet result = DataQuery.from(employees)
    .filter(row -> row.getBigDecimal("SALARY").compareTo(BigDecimal.valueOf(85000)) >= 0)
    .select("NAME", "SALARY", "DEPARTMENT_NAME")
    .orderBy("SALARY").desc()
    .execute();
```

**Characteristics:**
- ✅ Readable, natural language flow
- ✅ Type-safe, compile-time checked
- ✅ IDE autocomplete friendly
- ✅ Easy to discover operations
- ⚠️ Executes eagerly (operations run immediately)
- ⚠️ Can't inspect/optimize before execution
- ⚠️ Can't serialize the query

---

## Evaluated Alternatives

### 1. Stream-Based API (Pure Functional)

**Concept:**
```java
DataSet result = dataSet.stream()
    .filter(predicate)
    .map(transformer)
    .sorted(comparator)
    .collect(toDataSet());
```

**Analysis:**
- **Pros:** Familiar to Java developers, lazy evaluation, composable
- **Cons:** Harder to represent domain operations (groupBy with multiple aggregations), less readable for SQL-like operations
- **Best For:** Simple transformations, functional programming style
- **Rating:** ⭐⭐⭐ (3/5)

---

### 2. SQL-Like Query DSL (jOOQ/QueryDSL style)

**Concept:**
```java
DataSet result = SELECT(name, salary)
    .FROM(employees)
    .WHERE(salary.gt(80000))
    .ORDER_BY(salary.desc())
    .execute();
```

**Analysis:**
- **Pros:** SQL-familiar, very expressive, type-safe with code generation
- **Cons:** Requires code generation/reflection, verbose, not natural for non-SQL transformations
- **Best For:** Teams comfortable with SQL, complex analytical queries
- **Rating:** ⭐⭐⭐⭐ (4/5)

---

### 3. Declarative Specification (Config-Based)

**Concept:**
```json
{
  "operations": [
    {"type": "filter", "condition": "salary > 80000"},
    {"type": "select", "columns": ["name", "salary"]},
    {"type": "orderBy", "column": "salary", "direction": "desc"}
  ]
}
```

**Analysis:**
- **Pros:** Non-developers can modify, easy to serialize/version control, runtime flexibility
- **Cons:** No compile-time safety, harder to debug, limited expressiveness
- **Best For:** User-configurable reports, low-code platforms
- **Rating:** ⭐⭐⭐ (3/5)
- **Note:** Excellent when combined with fluent DSL (see Part 3)

---

### 4. Pipeline/Chain of Responsibility

**Concept:**
```java
Pipeline.create()
    .addStage(new FilterStage(predicate))
    .addStage(new SelectStage(columns))
    .addStage(new OrderByStage(column, direction))
    .execute(dataSet);
```

**Analysis:**
- **Pros:** Clear separation of concerns, testable stages, dynamic stage insertion
- **Cons:** Verbose, more classes to maintain
- **Best For:** Complex ETL pipelines, reusable transformation components
- **Rating:** ⭐⭐⭐ (3/5)

---

### 5. Immutable Transformations (Spark/Pandas style)

**Concept:**
```java
DataSet filtered = employees.filter(predicate);
DataSet selected = filtered.select(columns);
DataSet sorted = selected.orderBy(column);
```

**Analysis:**
- **Pros:** Clear data lineage, easy to cache intermediates, thread-safe
- **Cons:** Verbose, memory overhead, harder to optimize entire pipeline
- **Best For:** Big data processing, data science workflows
- **Rating:** ⭐⭐⭐⭐ (4/5)

---

### 6. Reactive/Observable Pattern (RxJava style)

**Concept:**
```java
Observable<DataRow> rows = dataSet.asObservable()
    .filter(predicate)
    .map(transformer)
    .subscribe(consumer);
```

**Analysis:**
- **Pros:** Great for streaming data, backpressure handling, async by default
- **Cons:** Learning curve, overkill for batch operations, debugging complexity
- **Best For:** Real-time data streams, async processing
- **Rating:** ⭐⭐ (2/5) for our use case

---

## Comparison Matrix

| Pattern | Readability | Type Safety | Flexibility | Learning Curve | Best Use Case |
|---------|-------------|-------------|-------------|----------------|---------------|
| **Fluent DSL (Current)** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Low | General purpose |
| **Stream API** | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | Low | Simple transforms |
| **SQL-like DSL** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | Low (SQL users) | DB operations |
| **Declarative** | ⭐⭐⭐ | ⭐ | ⭐⭐⭐⭐⭐ | Low | User configs |
| **Pipeline** | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Medium | Complex ETL |
| **Immutable** | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Low | Big data |
| **Reactive** | ⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | High | Streaming |

---

## Part 2: Lazy/Reified Fluent DSL

### The Big Idea: Fluent DSL That IS Declarative

Instead of executing immediately, fluent calls build an execution plan internally that can be:
- Inspected
- Serialized to JSON
- Optimized
- Cached
- Executed later

**This is what LINQ (C#), Spark DataFrames, and jOOQ do!**

---

### How It Works

#### Current Behavior (Eager):
```java
DataSet result = DataQuery.from(employees)
    .filter(row -> row.getInt("SALARY") > 80000)  // ← Executes NOW
    .select("NAME", "SALARY")                      // ← Executes NOW
    .execute();                                     // ← Just returns result
```

#### Lazy Behavior (Proposed):
```java
// These calls DON'T execute - they build a plan
DataQuery query = DataQuery.from(employees)
    .filter(row -> row.getInt("SALARY") > 80000)  // ← Stores in plan
    .select("NAME", "SALARY")                      // ← Stores in plan
    .orderBy("SALARY").desc();                     // ← Stores in plan

// NOW we can do interesting things...

// 1. Inspect the plan
QueryPlan plan = query.getPlan();
System.out.println(plan);
// Output: FilterOp -> SelectOp -> OrderByOp

// 2. Serialize to JSON!
String json = query.toJson();
// Output: {"operations": [{"type": "filter", ...}, {"type": "select", ...}]}

// 3. Optimize before execution
query = query.optimize(); // Reorder operations for performance

// 4. Cache the plan
cache.put("high_earners_query", query);

// 5. Execute when ready
DataSet result = query.execute();
```

---

### Powerful Capabilities Unlocked

#### 1. Bidirectional Conversion

```java
// Fluent → JSON
String json = DataQuery.from(employees)
    .filter(row -> row.getInt("SALARY") > 80000)
    .select("NAME", "SALARY")
    .toJson();

// JSON → Fluent
DataQuery query = DataQuery.fromJson(json);
DataSet result = query.execute(employees);
```

#### 2. Query Introspection

```java
DataQuery query = buildComplexQuery();

// See what it will do BEFORE executing
System.out.println("Operations: " + query.getOperationCount());
System.out.println("Estimated rows: " + query.estimateResultSize());
System.out.println("Will use indexes: " + query.canUseIndexes());
```

#### 3. Query Optimization

```java
DataQuery query = DataQuery.from(employees)
    .select("NAME", "SALARY", "DEPT", "HIRE_DATE")  // Select 4 columns
    .filter(row -> row.getInt("SALARY") > 80000)    // Then filter
    .select("NAME", "SALARY");                       // Then drop 2 columns

// Optimizer can rewrite this to:
// .filter(...) → .select("NAME", "SALARY")  // More efficient!
DataQuery optimized = query.optimize();
```

#### 4. Explain Plan (like SQL EXPLAIN)

```java
DataQuery query = DataQuery.from(employees)
    .filter(...)
    .join(departments, ...)
    .groupBy("DEPT")
    .sum("SALARY");

System.out.println(query.explain());
/*
Output:
1. ScanOperation (employees) - estimated 1000 rows
2. FilterOperation - estimated 400 rows
3. JoinOperation (departments) - estimated 400 rows
4. GroupByOperation - estimated 5 groups
5. AggregateOperation (SUM) - estimated 5 rows
*/
```

#### 5. Caching & Reuse

```java
// Define query once
DataQuery highEarnersTemplate = DataQuery.from(null)  // No dataset yet
    .filter(row -> row.getInt("SALARY") > 80000)
    .select("NAME", "SALARY")
    .orderBy("SALARY").desc();

// Reuse with different datasets
DataSet result1 = highEarnersTemplate.execute(employees2023);
DataSet result2 = highEarnersTemplate.execute(employees2024);
```

---

### Implementation Sketch

```java
// Query Plan Structure
interface Operation {
    String getType();
    Map<String, Object> toMap();  // For JSON serialization
}

class FilterOperation implements Operation {
    private final Predicate<DataRow> predicate;
    private final String expression;  // String representation

    public String getType() { return "FILTER"; }

    public Map<String, Object> toMap() {
        return Map.of(
            "type", "FILTER",
            "expression", expression
        );
    }
}

class QueryPlan {
    private final List<Operation> operations;

    public String toJson() {
        // Convert operations list to JSON
    }

    public QueryPlan optimize() {
        // Reorder/combine operations
    }
}

// Fluent DSL builds the plan
public class DataQuery {
    private final DataSet source;
    private final List<Operation> operations = new ArrayList<>();

    public DataQuery filter(Predicate<DataRow> predicate) {
        // DON'T execute - just store
        operations.add(new FilterOperation(predicate, captureExpression(predicate)));
        return this;  // For chaining
    }

    public DataQuery select(String... columns) {
        operations.add(new SelectOperation(columns));
        return this;
    }

    // Only execute when explicitly called
    public DataSet execute() {
        DataSet current = source;
        for (Operation op : operations) {
            current = op.apply(current);
        }
        return current;
    }

    // Export to JSON
    public String toJson() {
        return new Gson().toJson(
            operations.stream()
                .map(Operation::toMap)
                .collect(Collectors.toList())
        );
    }
}
```

---

### The Lambda Challenge

The ONE tricky part: capturing lambda expressions for serialization.

```java
// This lambda can't easily be serialized to JSON
.filter(row -> row.getInt("SALARY") > 80000)
```

**Solutions:**

1. **Expression-based API** (like jOOQ)
```java
.filter(col("SALARY").gt(80000))  // Can serialize!
```

2. **Dual API**
```java
.filter("SALARY > 80000")          // String - serializable
.filter(row -> ...)                 // Lambda - not serializable (more flexible)
```

3. **ASM/ByteBuddy** to inspect lambda bytecode (advanced)

---

## Part 3: The Revolutionary Two-Stage Pipeline

### The Brilliant Idea

Separate **data transformation** from **output formatting**:

```
Raw Data → [Transformation Spec] → Transformed Data → [Output Schema] → Final JSON
```

**Stage 1:** What data do we need? (filter, join, aggregate)
**Stage 2:** How should it look? (nested arrays, grouped objects, field names)

---

### Stage 1: Data Transformation (Declarative)

**transformation.json:**
```json
{
  "name": "employee_department_analysis",
  "transformations": [
    {
      "type": "join",
      "right": "departments",
      "on": "DEPARTMENT_ID"
    },
    {
      "type": "filter",
      "expression": "SALARY >= 80000"
    }
  ]
}
```

**Converts to Fluent DSL:**
```java
DataSet transformed = DataQuery.from(employees)
    .join(departments, "DEPARTMENT_ID", "DEPARTMENT_ID")
    .filter(row -> row.getInt("SALARY") >= 80000)
    .execute();
```

---

### Stage 2: Output Shaping (JSON Schema)

**output-schema.json:**
```json
{
  "type": "object",
  "properties": {
    "company": {
      "const": "Acme Corp"
    },
    "reportDate": {
      "function": "NOW"
    },
    "departments": {
      "type": "array",
      "groupBy": "DEPARTMENT_NAME",
      "items": {
        "type": "object",
        "properties": {
          "name": {
            "source": "DEPARTMENT_NAME"
          },
          "headcount": {
            "aggregate": "COUNT",
            "column": "EMPLOYEE_ID"
          },
          "totalSalary": {
            "aggregate": "SUM",
            "column": "SALARY"
          },
          "employees": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
                "id": {"source": "EMPLOYEE_ID"},
                "name": {"source": "NAME"},
                "salary": {"source": "SALARY"}
              }
            }
          }
        }
      }
    }
  }
}
```

**Key Innovation:** The schema contains transformation hints!
- `"groupBy": "DEPARTMENT_NAME"` → tells engine to group
- `"aggregate": "COUNT"` → tells engine to aggregate
- Nested arrays → tells engine to create hierarchies

---

### Complete Flow

```java
// Separate stages
DataSet transformed = TransformationEngine.execute(
    employees,
    "transformation.json"
);

String json = OutputShaper.shape(
    transformed,
    "output-schema.json"
);

// Or combined
String json = ReportEngine.generate(
    employees,
    "transformation.json",    // How to transform
    "output-schema.json"      // How to output
);
```

---

### Output Result

```json
{
  "company": "Acme Corp",
  "reportDate": "2024-06-25",
  "departments": [
    {
      "name": "Engineering",
      "headcount": 3,
      "totalSalary": 247000,
      "employees": [
        {"id": 1, "name": "Alice Johnson", "salary": 80000},
        {"id": 3, "name": "Charlie Brown", "salary": 85000},
        {"id": 5, "name": "Eve Wilson", "salary": 82000}
      ]
    },
    {
      "name": "Sales",
      "headcount": 3,
      "totalSalary": 273000,
      "employees": [
        {"id": 2, "name": "Bob Smith", "salary": 90000},
        {"id": 4, "name": "David Lee", "salary": 95000},
        {"id": 7, "name": "Grace Davis", "salary": 88000}
      ]
    }
  ]
}
```

---

## Why This Two-Stage Approach Is Genius

### 1. Complete Separation of Concerns

- **What data** → Transformation spec
- **How to present** → Output schema
- Change either independently!

Example: Same transformation, different outputs:
```java
DataSet transformed = transform(employees, "sales_analysis.json");

// CEO dashboard - summary only
String summary = shape(transformed, "schema_summary.json");

// Manager dashboard - detailed breakdown
String detailed = shape(transformed, "schema_detailed.json");

// Export - flat CSV structure
String export = shape(transformed, "schema_export.json");
```

---

### 2. The Schema IS the Transformation Hint

The output schema tells the engine what transformations are needed:

```json
{
  "departments": {
    "groupBy": "DEPARTMENT_NAME",  // ← GROUP operation
    "items": {
      "headcount": {
        "aggregate": "COUNT",       // ← AGGREGATE operation
        "column": "EMPLOYEE_ID"
      },
      "employees": {
        "type": "array"             // ← NEST operation
      }
    }
  }
}
```

The engine can **infer transformations from the desired output!**

---

### 3. Reverse Engineering Possible

```java
// Analyze output schema to determine needed transformations
OutputSchema schema = OutputSchema.fromJson("output-schema.json");
TransformationSpec inferredSpec = SchemaAnalyzer.inferTransformations(schema);

// "Oh, you need groupBy DEPARTMENT_NAME with COUNT aggregation?
//  Let me build that transformation for you!"

System.out.println(inferredSpec.toJson());
/*
{
  "transformations": [
    {"type": "groupBy", "key": "DEPARTMENT_NAME"},
    {"type": "aggregate", "function": "COUNT", "column": "EMPLOYEE_ID"}
  ]
}
*/
```

---

### 4. Cross-Stage Optimization

```java
// Optimizer can see BOTH stages
TransformationSpec dataTrans = loadSpec("transformation.json");
OutputSchema outputSchema = loadSpec("output-schema.json");

// "Ah, you're only outputting NAME and SALARY in the schema,
//  so I don't need to select ALL columns in transformation!"
OptimizedPlan plan = Optimizer.optimize(dataTrans, outputSchema);

// "And you're grouping by DEPARTMENT_NAME in output,
//  so I can push down the group operation to the transformation stage!"
```

**Example optimization:**

Before:
```
Transform: SELECT * FROM employees JOIN departments
Output: Group by DEPARTMENT_NAME, select only NAME, SALARY
```

After optimization:
```
Transform: SELECT NAME, SALARY, DEPARTMENT_NAME FROM employees JOIN departments
           GROUP BY DEPARTMENT_NAME
Output: Format the pre-grouped data
```

---

### 5. Multi-Level Hierarchies Automatically

**Complex 3-level hierarchy:**

```json
{
  "company": {
    "departments": {
      "type": "array",
      "groupBy": "DEPARTMENT_ID",
      "items": {
        "departmentName": {"source": "DEPARTMENT_NAME"},
        "employees": {
          "type": "array",
          "groupBy": "EMPLOYEE_ID",
          "items": {
            "employeeName": {"source": "NAME"},
            "projects": {
              "type": "array",
              "items": {
                "projectName": {"source": "PROJECT_NAME"},
                "hours": {"source": "HOURS_WORKED"}
              }
            }
          }
        }
      }
    }
  }
}
```

**The engine automatically:**
1. Groups by DEPARTMENT_ID (level 1)
2. Within each department, groups by EMPLOYEE_ID (level 2)
3. Within each employee, collects projects as array (level 3)
4. Builds the complete nested JSON

**This solves Phase 3 hierarchical JSON transformation!**

---

## Advanced: Schema Directives

Extend JSON Schema with custom transformation directives:

```json
{
  "employees": {
    "type": "array",
    "x-transform": {
      "filter": "SALARY > 80000",                    // Filter directive
      "orderBy": {"column": "SALARY", "direction": "DESC"},
      "limit": 10                                     // Top 10 only
    },
    "items": {
      "name": {"source": "NAME"},
      "monthlySalary": {
        "source": "SALARY",
        "x-transform": "SALARY / 12",                // Computed field
        "x-format": "currency"                        // Format directive
      },
      "status": {
        "x-computed": "SALARY > 90000 ? 'Senior' : 'Junior'"  // Conditional
      }
    }
  }
}
```

**Supported directives:**
- `x-transform`: Data transformations at this level
- `x-computed`: Computed/derived fields
- `x-format`: Output formatting
- `x-filter`: Filter items at this level
- `x-orderBy`: Sort items at this level
- `x-limit`: Limit number of items

---

## Bidirectional Magic

### Generate Schema FROM Fluent DSL

```java
DataQuery query = DataQuery.from(employees)
    .filter(row -> row.getInt("SALARY") > 80000)
    .select("NAME", "SALARY")
    .orderBy("SALARY").desc();

// Extract output schema from query
OutputSchema schema = query.inferOutputSchema();
String schemaJson = schema.toJson();

/*
{
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "NAME": {"type": "string"},
      "SALARY": {"type": "number"}
    }
  },
  "x-transform": {
    "filter": "SALARY > 80000",
    "orderBy": {"column": "SALARY", "direction": "DESC"}
  }
}
*/
```

### Generate Fluent DSL FROM Schema

```java
OutputSchema schema = OutputSchema.fromJson(schemaJson);

// Analyze schema requirements
TransformationPlan plan = schema.inferRequiredTransformations();

// Generate fluent code (or execute directly)
DataQuery query = plan.toFluentDSL();

// Or execute directly
DataSet result = schema.execute(employees);
```

---

## The Holy Grail: Single Unified Spec

**super-spec.json** (combines transformation + output):

```json
{
  "name": "department_report",
  "input": "employees",
  "transformations": [
    {"type": "join", "right": "departments", "on": "DEPARTMENT_ID"},
    {"type": "filter", "expression": "SALARY >= 80000"}
  ],
  "output": {
    "type": "object",
    "properties": {
      "summary": {
        "totalEmployees": {"aggregate": "COUNT", "column": "EMPLOYEE_ID"},
        "avgSalary": {"aggregate": "AVG", "column": "SALARY"}
      },
      "departments": {
        "type": "array",
        "groupBy": "DEPARTMENT_NAME",
        "items": {
          "department": {"source": "DEPARTMENT_NAME"},
          "staff": {
            "type": "array",
            "items": {
              "name": {"source": "NAME"},
              "salary": {"source": "SALARY"}
            }
          }
        }
      }
    }
  }
}
```

**Single execution call:**
```java
String json = ReportEngine.generate(employees, "super-spec.json");
```

---

## Real-World Analogies

This two-stage pattern is used by:

### GraphQL
```graphql
query {
  departments {              # Implicit transformation + output shape
    name
    employees {              # Nested grouping
      name
      salary
    }
  }
}
```

### dbt (data build tool)
```yaml
models:
  - name: department_summary
    transformations:
      - join: departments
      - filter: salary >= 80000
    output:
      columns:
        - department_name
        - employee_count
```

### Apache Beam
```python
(employees
  | beam.Filter(lambda x: x['salary'] >= 80000)
  | beam.GroupBy('department')
  | beam.Map(format_output_schema))
```

---

## Implementation Architecture

### High-Level Components

```
┌─────────────────────────────────────────────────────┐
│                   ReportEngine                      │
│  (Orchestrates transformation + output shaping)     │
└─────────────────────────────────────────────────────┘
           │                            │
           ▼                            ▼
┌──────────────────────┐    ┌──────────────────────┐
│ TransformationEngine │    │    OutputShaper      │
│  - Parse spec        │    │  - Parse schema      │
│  - Build query plan  │    │  - Infer grouping    │
│  - Optimize          │    │  - Build hierarchy   │
│  - Execute           │    │  - Render JSON       │
└──────────────────────┘    └──────────────────────┘
           │                            │
           ▼                            ▼
    ┌──────────┐              ┌──────────────────┐
    │ DataSet  │─────────────▶│  Hierarchical    │
    │ (Flat)   │              │  JSON Output     │
    └──────────┘              └──────────────────┘
```

### Implementation Sketch

```java
public class ReportEngine {

    // Separate specs
    public static String generate(
        DataSet input,
        String transformationSpec,
        String outputSchema
    ) {
        // Stage 1: Transform data
        TransformationSpec trans = TransformationSpec.fromJson(transformationSpec);
        DataSet transformed = trans.apply(input);

        // Stage 2: Shape output
        OutputSchema schema = OutputSchema.fromJson(outputSchema);
        return schema.render(transformed);
    }

    // Combined spec
    public static String generate(DataSet input, String combinedSpec) {
        CombinedSpec spec = CombinedSpec.fromJson(combinedSpec);

        // Execute transformation
        DataSet transformed = spec.getTransformations().apply(input);

        // Apply output schema
        return spec.getOutputSchema().render(transformed);
    }

    // With optimization
    public static String generateOptimized(DataSet input, String combinedSpec) {
        CombinedSpec spec = CombinedSpec.fromJson(combinedSpec);

        // Cross-stage optimization
        OptimizedPlan plan = Optimizer.optimize(
            spec.getTransformations(),
            spec.getOutputSchema()
        );

        return plan.execute(input);
    }
}
```

---

## Benefits Summary

### 1. For Developers
- ✅ **Fluent DSL** for complex custom logic
- ✅ **Type safety** and IDE support
- ✅ **Debuggable** code with breakpoints
- ✅ **Testable** transformations

### 2. For Business Users
- ✅ **Declarative specs** - no coding required
- ✅ **Visual builders** possible (drag-drop report designer)
- ✅ **Self-service reports** without developer involvement
- ✅ **Version control** for report definitions

### 3. For the System
- ✅ **Optimization** across transformation + output stages
- ✅ **Caching** of query plans and results
- ✅ **Introspection** and explain plans
- ✅ **Serialization** for persistence and sharing
- ✅ **Portability** across platforms

### 4. For Maintenance
- ✅ **Separation of concerns** - clear boundaries
- ✅ **Reusable components** - specs are composable
- ✅ **Self-documenting** - schemas describe output
- ✅ **Testable** - specs can be unit tested

---

## Solving Phase 2/3 JSON Transformation

This approach elegantly solves the Phase 2 and Phase 3 challenges:

**Phase 2 (Nested Objects):**
```json
{
  "output": {
    "type": "array",
    "items": {
      "personal": {
        "id": {"source": "EMPLOYEE_ID"},
        "name": {"source": "NAME"}
      },
      "employment": {
        "department": {"source": "DEPARTMENT_NAME"},
        "hireDate": {"source": "HIRE_DATE"}
      },
      "compensation": {
        "salary": {"source": "SALARY"},
        "currency": {"const": "USD"}
      }
    }
  }
}
```

**Phase 3 (Hierarchical Grouping):**
```json
{
  "output": {
    "departments": {
      "type": "array",
      "groupBy": "DEPARTMENT_ID",
      "items": {
        "name": {"source": "DEPARTMENT_NAME"},
        "employees": {
          "type": "array",
          "items": {
            "name": {"source": "NAME"},
            "salary": {"source": "SALARY"}
          }
        }
      }
    }
  }
}
```

**No manual construction needed!** The schema drives everything.

---

## Next Steps / Implementation Roadmap

### Phase 1: Lazy Fluent DSL (2-3 weeks)
- [ ] Refactor DataQuery to build QueryPlan instead of executing
- [ ] Implement Operation interface and concrete operations
- [ ] Add toJson() / fromJson() serialization
- [ ] Add explain() method
- [ ] Add basic optimizer

### Phase 2: Declarative Transformation Specs (2 weeks)
- [ ] Define transformation spec JSON schema
- [ ] Implement TransformationSpec parser
- [ ] Build spec → fluent DSL converter
- [ ] Add expression evaluator for filters
- [ ] Integration tests

### Phase 3: Schema-Driven Output (3-4 weeks)
- [ ] Define output schema format (extend JSON Schema)
- [ ] Implement OutputSchema parser
- [ ] Build hierarchical renderer with grouping support
- [ ] Handle nested arrays and aggregations
- [ ] Support schema directives (x-transform, x-computed)

### Phase 4: Integration & Optimization (2 weeks)
- [ ] Combine transformation + output in ReportEngine
- [ ] Cross-stage optimizer
- [ ] Caching layer
- [ ] Performance benchmarks

### Phase 5: Developer Experience (1-2 weeks)
- [ ] Fluent → Schema converter
- [ ] Schema → Fluent converter
- [ ] Validation and helpful error messages
- [ ] Documentation and examples

**Total Estimated Effort:** 10-14 weeks for complete implementation

---

## Alternative: Hybrid Approach (Recommended)

Instead of full implementation, start with a **hybrid approach**:

### Immediate (1-2 weeks):
- Keep current fluent DSL (it works great!)
- Add optional lazy evaluation mode
- Add toJson() for simple serialization

### Short-term (2-3 weeks):
- Add declarative spec support alongside fluent
- Users can choose: code OR config
- Build converter: spec → fluent DSL

### Medium-term (4-6 weeks):
- Add basic schema-driven output
- Start with simple grouping and nesting
- Expand based on real use cases

### Long-term:
- Full bidirectional conversion
- Advanced optimization
- Visual report builder UI

---

## Conclusion

The combination of:
1. **Lazy Fluent DSL** (best developer experience)
2. **Declarative Specifications** (best for non-developers)
3. **Schema-Driven Output** (solves hierarchical JSON)

...creates an incredibly powerful and flexible reporting framework that serves:
- Developers (fluent DSL, type safety)
- Business users (declarative specs, no coding)
- Performance (optimization, caching)
- Maintainability (separation of concerns)

This architecture positions the framework as **enterprise-grade** while remaining simple for basic use cases.

---

## References

- **LINQ (C#):** Query syntax with lazy evaluation
- **Apache Spark:** DataFrame API with execution plans
- **jOOQ:** Type-safe SQL DSL with query building
- **GraphQL:** Query language that combines data + shape
- **dbt:** Declarative data transformations
- **JSON Schema:** Standard for describing JSON structure
- **Phase 2/3 Architecture Docs:** SCHEMA_EXAMPLES.md, PHASE3_ARCHITECTURE.md

---

**End of Research Document**
