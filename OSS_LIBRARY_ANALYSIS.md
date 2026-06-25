# OSS Library Analysis - JSON Transformation Solutions

**Goal:** Transform flat DataSet rows into hierarchical JSON structures with grouping and nesting

---

## 🔍 Libraries Found

### 1. **Jolt** (Most Relevant) ⭐⭐⭐⭐

**Source:** [GitHub - bazaarvoice/jolt](https://github.com/bazaarvoice/jolt)

**What it does:**
- JSON-to-JSON transformation library written in Java
- Declarative spec-based transformations (spec is itself JSON)
- Focuses on structural transformation, not data manipulation

**Maven Dependency:**
```xml
<dependency>
    <groupId>com.bazaarvoice.jolt</groupId>
    <artifactId>jolt-core</artifactId>
    <version>0.1.8</version> <!-- Check for latest version -->
</dependency>
```

**Core Transforms:**
- `shift` - Copy/relocate data in output tree
- `default` - Apply default values
- `remove` - Delete data
- `sort` - Alphabetize keys
- `cardinality` - Fix list vs single value issues

**Basic Usage:**
```java
// 1. Load transformation spec (JSON)
Chainr chainr = Chainr.fromSpec(specList);

// 2. Transform input
Object input = /* your data */;
Object output = chainr.transform(input);
```

**Example Spec (Flat to Nested):**
```json
{
  "operation": "shift",
  "spec": {
    "EMPLOYEE_ID": "employee.id",
    "NAME": "employee.name",
    "SALARY": "compensation.salary"
  }
}
```

**Pros:**
✅ Mature library (Bazaarvoice production use)
✅ Declarative specs (no code for simple transforms)
✅ Chainable transforms
✅ Active community
✅ [Interactive demo](https://jolt-demo.appspot.com/) available

**Cons:**
❌ Not stream-based (holds entire JSON in memory)
❌ Heavy GC usage (creates many objects)
❌ **Grouping/aggregation not built-in** (requires custom transforms)
❌ Learning curve for complex specs
❌ No native support for SQL-like GROUP BY

**Can it do our Phase 3?**
- ✅ Phase 1: Simple field renaming - **YES**
- ✅ Phase 2: Nested objects - **YES**
- ⚠️ Phase 3: Hierarchical grouping - **PARTIAL** (would need custom Java transform)

**References:**
- [Jolt GitHub](https://github.com/bazaarvoice/jolt)
- [Mastering JOLT Transformations: Grouping](https://www.devgem.io/posts/mastering-jolt-transformations-grouping-json-objects-into-nested-structures)
- [Jolt Tutorial with Examples](https://dev.to/sadiul_hakim/java-jolt-library-tutorial-with-examples-3afc)

---

### 2. **json-flattener** ⭐⭐⭐

**Source:** [GitHub - wnameless/json-flattener](https://github.com/wnameless/json-flattener)

**What it does:**
- Flatten nested JSON to flat key-value pairs
- Unflatten flat structures back to nested JSON

**Maven Dependency:**
```xml
<dependency>
    <groupId>com.github.wnameless.json</groupId>
    <artifactId>json-flattener</artifactId>
    <version>0.16.6</version>
</dependency>
```

**Usage:**
```java
// Flatten
String json = "{\"a\":{\"b\":1,\"c\":null}}";
String flattened = new JsonFlattener(json).flatten();
// Output: {"a.b":1,"a.c":null}

// Unflatten
String json = "{\"a.b\":1,\"a.c\":null}";
String unflattened = new JsonUnflattener(json).unflatten();
// Output: {"a":{"b":1,"c":null}}
```

**Pros:**
✅ Simple API
✅ Good for flatten/unflatten operations
✅ Customizable separators

**Cons:**
❌ **No grouping support**
❌ **No aggregation support**
❌ Only handles flatten/unflatten, not arbitrary transformations
❌ Doesn't solve our problem directly

**Can it do our Phase 3?** **NO**
- Only handles bidirectional flat ↔ nested conversion
- No concept of grouping data by keys
- No aggregation capabilities

---

### 3. **JSONata** ⭐⭐⭐⭐

**Source:** [JSONata.org](http://jsonata.org/)

**What it does:**
- Query and transformation language for JSON
- Functional programming style
- Supports grouping, aggregation, sorting

**Java Implementation:**
```xml
<dependency>
    <groupId>com.dashjoin</groupId>
    <artifactId>jsonata</artifactId>
    <version>0.9.5</version>
</dependency>
```

**Capabilities:**
- Path operators
- Numeric, boolean, comparison operators
- **Grouping and aggregation** ✅
- Functions and expressions
- Functional programming constructs

**Example (Grouping):**
```javascript
/* JSONata expression */
{
  "departments": (
    $ ~> | $ | {"dept": DEPARTMENT_NAME}, "dept" |
    {
      "name": dept,
      "employees": [{"name": NAME, "salary": SALARY}]
    }
  )
}
```

**Pros:**
✅ **Supports grouping and aggregation**
✅ Powerful query language
✅ Functional programming features
✅ Turing complete

**Cons:**
❌ Different syntax/paradigm (not Java)
❌ Learning curve for JSONata language
❌ Performance concerns for large datasets
❌ Less intuitive than SQL-style operations

**Can it do our Phase 3?** **YES, BUT...**
- ✅ Can do grouping
- ✅ Can do nested structures
- ⚠️ Requires learning JSONata expression language
- ⚠️ Not as natural for tabular → hierarchical transform

**References:**
- [JQ vs JSONata comparison](https://dashjoin.medium.com/jq-vs-jsonata-language-and-tooling-compared-5f0f7acc778e)
- [JSONata Performance Considerations](https://nearform.com/insights/the-jsonata-performance-dilemma/)

---

### 4. **Smooks** ⭐⭐⭐

**Source:** [Smooks.org](https://www.smooks.org/)

**What it does:**
- Extensible ETL engine for structured data
- Supports XML, CSV, EDI, JSON
- Can bind to Java Object Models

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.smooks</groupId>
    <artifactId>smooks-core</artifactId>
    <version>2.0.0</version>
</dependency>
```

**Pros:**
✅ Comprehensive ETL framework
✅ Supports multiple formats
✅ Java-based

**Cons:**
❌ **Very heavyweight** (full ETL framework)
❌ Overkill for our use case
❌ Steep learning curve
❌ Complex configuration

**Can it do our Phase 3?** Probably yes, but massive overkill

---

### 5. **JsonEDI** ⭐⭐

**Source:** [JsonEDI.com](https://www.jsonedi.com/)

**What it does:**
- Transform hierarchical ↔ tabular data
- SQL to JSON transformation
- Low latency

**Pros:**
✅ Specifically designed for SQL → JSON hierarchy
✅ Multi-table merging

**Cons:**
❌ **Proprietary/Commercial** (not clear if fully open source)
❌ Limited documentation
❌ Less community support

---

## 📊 Comparison Matrix

| Library | Phase 1 | Phase 2 | Phase 3 | Grouping | Aggregation | Maturity | Learning Curve |
|---------|---------|---------|---------|----------|-------------|----------|----------------|
| **Jolt** | ✅ | ✅ | ⚠️ Partial | ❌ (custom) | ❌ (custom) | ⭐⭐⭐⭐⭐ | Medium |
| **json-flattener** | ✅ | ✅ | ❌ | ❌ | ❌ | ⭐⭐⭐ | Low |
| **JSONata** | ✅ | ✅ | ✅ | ✅ | ✅ | ⭐⭐⭐⭐ | High |
| **Smooks** | ✅ | ✅ | ✅ | ✅ | ✅ | ⭐⭐⭐⭐ | Very High |
| **Custom Build** | ✅ | ✅ | ✅ | ✅ | ✅ | ⭐ (new) | Low (we design it) |

---

## 💡 Analysis: Which Approach?

### Option A: Use Jolt + Custom Transform for Grouping

**Approach:**
1. Use Jolt for field mapping and nested objects (Phase 1 & 2)
2. Write custom `Transform` implementation for grouping/aggregation
3. Chain them together

**Implementation:**
```java
// Custom grouping transform
public class GroupingTransform implements Transform {
    @Override
    public Object transform(Object input) {
        // Your grouping logic here
        List<Map<String, Object>> rows = (List) input;

        // Group by department
        Map<Object, List<Map<String, Object>>> groups =
            rows.stream().collect(Collectors.groupingBy(
                row -> row.get("DEPARTMENT_ID")
            ));

        // Build hierarchical structure
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Object, List<Map<String, Object>>> group : groups.entrySet()) {
            Map<String, Object> dept = new LinkedHashMap<>();
            dept.put("name", group.getValue().get(0).get("DEPARTMENT_NAME"));
            dept.put("employees", group.getValue());
            result.add(dept);
        }

        return result;
    }
}

// Chain with Jolt
List<Object> chainrSpec = Arrays.asList(
    // First: Jolt transform for field mapping
    JsonUtils.classpathToObject("/specs/fieldMapping.json"),
    // Then: Custom grouping
    new GroupingTransform()
);

Chainr chainr = Chainr.fromSpec(chainrSpec);
Object output = chainr.transform(dataSet.toMaps());
```

**Pros:**
✅ Leverage mature Jolt for field mapping
✅ Write Java for complex logic (familiar)
✅ Chainable/composable
✅ Best of both worlds

**Cons:**
⚠️ Still writing custom code for the hard part (grouping)
⚠️ Jolt specs can be verbose for simple mappings
⚠️ Two paradigms to maintain (Jolt specs + Java code)

**Effort:** ~2 weeks
- Week 1: Learn Jolt, implement field mapping specs
- Week 2: Write custom GroupingTransform

---

### Option B: Use JSONata

**Approach:**
Use JSONata expressions for entire transformation

**Implementation:**
```java
import com.dashjoin.jsonata.Jsonata;

String expression = """
{
  "departments": (
    $ ~> | $ | {"dept": DEPARTMENT_NAME}, "dept" |
    {
      "name": dept,
      "headcount": $count($),
      "employees": [{
        "name": NAME,
        "salary": SALARY
      }]
    }
  )
}
""";

Jsonata jsonata = new Jsonata(expression);
String output = jsonata.evaluate(dataSet.toMaps());
```

**Pros:**
✅ One language for everything
✅ Supports grouping and aggregation natively
✅ Concise expressions

**Cons:**
❌ New language to learn (not Java-like)
❌ Harder to debug than Java
❌ Team needs to learn JSONata
❌ Less intuitive for tabular transformations

**Effort:** ~2-3 weeks
- Week 1-2: Learn JSONata expression language
- Week 1-2: Build expressions for use cases
- Ongoing: Team learning curve

---

### Option C: Build Custom Solution (Our Phase 3 Plan)

**Approach:**
Build exactly what we need, optimized for DataSet → JSON hierarchy

**Pros:**
✅ **Tailored to our exact needs**
✅ **Natural Java API** (builder pattern)
✅ **Optimized for our use case** (DataSet → hierarchical JSON)
✅ **No external dependencies**
✅ **Full control over features/performance**
✅ Team understands it completely

**Cons:**
❌ 3-4 weeks development time
❌ Need to maintain it ourselves
❌ Reinventing some wheels

**Effort:** 3-4 weeks (as per our Phase 3 plan)

---

## 🎯 Recommendation

### **Hybrid Approach: Jolt + Custom Transform**

**Why:**
1. **Jolt handles 60% of the work** (field mapping, nested objects)
2. **Custom Java handles 40%** (grouping, aggregation) - what we know best
3. **Faster than full custom build** (~2 weeks vs 3-4 weeks)
4. **Leverages proven library** (Jolt is battle-tested)
5. **Stays in Java** (no new languages like JSONata)

**Implementation Plan:**

### Week 1: Jolt Integration
**Days 1-2:**
- [ ] Add Jolt dependency
- [ ] Create Jolt specs for Phase 1 (field renaming)
- [ ] Test with FullWorkflowIntegrationTest data

**Days 3-5:**
- [ ] Create Jolt specs for Phase 2 (nested objects)
- [ ] Handle constants and computed fields in Java
- [ ] Integration tests

### Week 2: Custom Grouping Transform
**Days 1-3:**
- [ ] Implement `GroupingTransform` class
- [ ] Support single-level grouping
- [ ] Support aggregations (COUNT, SUM, AVG)

**Days 4-5:**
- [ ] Support multi-level nesting
- [ ] Chain Jolt + Custom transforms
- [ ] Complete integration tests
- [ ] Documentation

**Total: 2 weeks to Phase 3 capability**

---

## 🔬 Proof of Concept

### Quick Test with Jolt

**Input (our DataSet as List<Map>):**
```json
[
  {"EMPLOYEE_ID": 1, "NAME": "Alice", "SALARY": 80000, "DEPT_NAME": "Engineering"},
  {"EMPLOYEE_ID": 2, "NAME": "Bob", "SALARY": 90000, "DEPT_NAME": "Sales"}
]
```

**Jolt Spec (Phase 1 - Rename):**
```json
{
  "operation": "shift",
  "spec": {
    "*": {
      "EMPLOYEE_ID": "[&1].id",
      "NAME": "[&1].fullName",
      "SALARY": "[&1].salary",
      "DEPT_NAME": "[&1].department"
    }
  }
}
```

**Output:**
```json
[
  {"id": 1, "fullName": "Alice", "salary": 80000, "department": "Engineering"},
  {"id": 2, "fullName": "Bob", "salary": 90000, "department": "Sales"}
]
```

**Jolt Spec (Phase 2 - Nested):**
```json
{
  "operation": "shift",
  "spec": {
    "*": {
      "EMPLOYEE_ID": "[&1].employee.id",
      "NAME": "[&1].employee.name",
      "SALARY": "[&1].compensation.salary",
      "DEPT_NAME": "[&1].employment.department"
    }
  }
}
```

**Output:**
```json
[
  {
    "employee": {"id": 1, "name": "Alice"},
    "compensation": {"salary": 80000},
    "employment": {"department": "Engineering"}
  }
]
```

**Then Custom Java for Grouping:**
```java
// GroupingTransform implementation
// Groups the above by department, nests employees
```

---

## 📈 Final Recommendation

### **Go with: Jolt + Custom Transform (Hybrid)**

**Timeline:** 2 weeks

**Rationale:**
1. **50% faster** than full custom build (2 weeks vs 4 weeks)
2. **Lower risk** - Jolt is proven, we only build the hard part
3. **Maintainable** - Jolt specs are declarative, custom code is focused
4. **Flexible** - Can extend either side as needed
5. **Team-friendly** - Mostly Java, small JSON specs

**What We Build:**
- `GroupingTransform` class (~300-400 lines)
- `AggregationEngine` utility (~200 lines)
- Jolt spec files (JSON, ~50-100 lines total)
- Integration with DataSet (~100 lines)

**What We Get for Free:**
- Field mapping (via Jolt)
- Nested object creation (via Jolt)
- Path parsing (via Jolt)
- Transform chaining (via Jolt Chainr)

---

## 🚀 Next Steps

1. **POC** - Build quick prototype with Jolt (1 day)
2. **Evaluate** - Confirm it fits our needs
3. **Implement** - 2-week sprint
4. **Ship** - Working Phase 3 capability

**Want to:**
- **A)** Try Jolt POC first (recommended - 1 day)
- **B)** Skip POC, commit to Jolt hybrid (2 weeks)
- **C)** Stick with full custom build (4 weeks)
- **D)** Try JSONata instead (2-3 weeks)

Which path makes sense?

---

## Sources

- [Jolt GitHub Repository](https://github.com/bazaarvoice/jolt)
- [Jolt Tutorial with Examples](https://dev.to/sadiul_hakim/java-jolt-library-tutorial-with-examples-3afc)
- [Mastering JOLT Transformations](https://www.devgem.io/posts/mastering-jolt-transformations-grouping-json-objects-into-nested-structures)
- [Jolt Interactive Demo](https://jolt-demo.appspot.com/)
- [json-flattener GitHub](https://github.com/wnameless/json-flattener)
- [JSONata vs JQ Comparison](https://dashjoin.medium.com/jq-vs-jsonata-language-and-tooling-compared-5f0f7acc778e)
- [JSONata Performance Analysis](https://nearform.com/insights/the-jsonata-performance-dilemma/)
- [Java ETL Tools Overview](https://northconcepts.com/blog/2017/08/31/java-etl-tools/)
