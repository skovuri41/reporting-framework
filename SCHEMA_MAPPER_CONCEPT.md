# JSON Schema Mapping - Concept & Feasibility

## Current State: Simple JSON Output

```java
// Current capability
DataSet employees = getEmployees();
String json = employees.toJSON();

// Output:
[
  {"EMPLOYEE_ID": 1, "NAME": "Alice", "SALARY": 80000},
  {"EMPLOYEE_ID": 2, "NAME": "Bob", "SALARY": 90000}
]
```

## Vision: Schema-Driven Output

### Use Case 1: Simple Field Mapping (EASY ✅)

**Target Schema:**
```json
{
  "type": "array",
  "items": {
    "type": "object",
    "properties": {
      "id": {"type": "integer"},
      "fullName": {"type": "string"},
      "annualSalary": {"type": "number"}
    }
  }
}
```

**Desired Output:**
```json
[
  {"id": 1, "fullName": "Alice", "annualSalary": 80000},
  {"id": 2, "fullName": "Bob", "annualSalary": 90000}
]
```

**Implementation Approach:**
```java
// Simple field renaming + selection
SchemaMapper mapper = new SchemaMapper()
    .map("EMPLOYEE_ID", "id")
    .map("NAME", "fullName")
    .map("SALARY", "annualSalary");

DataSet transformed = DataQuery.from(employees)
    .applySchemaMapping(mapper)
    .execute();

String json = transformed.toJSON();
```

**Effort:** ~2-3 hours
- Add SchemaMapper class
- Add field renaming utility
- Add toJSONWithMapping() method

---

### Use Case 2: Nested Objects (MEDIUM 🟡)

**Target Schema:**
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
          "name": {"type": "string"}
        }
      },
      "compensation": {
        "type": "object",
        "properties": {
          "salary": {"type": "number"},
          "currency": {"type": "string"}
        }
      }
    }
  }
}
```

**Desired Output:**
```json
[
  {
    "employee": {"id": 1, "name": "Alice"},
    "compensation": {"salary": 80000, "currency": "USD"}
  }
]
```

**Implementation Approach:**
```java
SchemaMapper mapper = new SchemaMapper()
    .map("EMPLOYEE_ID", "employee.id")
    .map("NAME", "employee.name")
    .map("SALARY", "compensation.salary")
    .mapConstant("compensation.currency", "USD");

// New method to build nested structures
String json = employees.toNestedJSON(mapper);
```

**Effort:** ~1-2 days
- Path parsing (employee.id -> nested structure)
- Nested object builder
- Support for constant values
- Type conversions

---

### Use Case 3: Grouped/Nested Arrays (HARD 🔴)

**Target Schema:**
```json
{
  "type": "object",
  "properties": {
    "departments": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {"type": "string"},
          "employees": {
            "type": "array",
            "items": {
              "type": "object",
              "properties": {
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
```

**Desired Output:**
```json
{
  "departments": [
    {
      "name": "Engineering",
      "employees": [
        {"name": "Alice", "salary": 80000},
        {"name": "Charlie", "salary": 85000}
      ]
    },
    {
      "name": "Sales",
      "employees": [
        {"name": "Bob", "salary": 90000}
      ]
    }
  ]
}
```

**Implementation Approach:**
```java
// This requires grouping + nesting
SchemaMapper mapper = new SchemaMapper()
    .groupBy("DEPARTMENT_NAME", "departments[]")
    .map("DEPARTMENT_NAME", "departments[].name")
    .nestArray("departments[].employees[]")
        .map("NAME", "name")
        .map("SALARY", "salary");

String json = employees.toNestedJSON(mapper);
```

**Effort:** ~1 week
- Grouping logic
- Array nesting support
- Hierarchical structure builder
- Complex path resolution

---

## Recommended Approach

### Phase 1: Simple Field Mapping (Start Here) ✅

**What to build:**
```java
public class FieldMapper {
    private Map<String, String> fieldMap = new HashMap<>();

    public FieldMapper rename(String from, String to) {
        fieldMap.put(from, to);
        return this;
    }

    public DataSet apply(DataSet source) {
        List<DataRow> remapped = source.getRows().stream()
            .map(row -> {
                Map<String, Object> newData = new HashMap<>();
                for (String key : row.keys()) {
                    String newKey = fieldMap.getOrDefault(key, key);
                    newData.put(newKey, row.get(key));
                }
                return DataRow.of(newData);
            })
            .collect(Collectors.toList());

        return new DataSet(remapped, source.getOutputParameters(),
                          source.getReportName() + "_mapped");
    }
}
```

**Usage:**
```java
FieldMapper mapper = new FieldMapper()
    .rename("EMPLOYEE_ID", "id")
    .rename("NAME", "fullName")
    .rename("SALARY", "annualSalary");

DataSet mapped = mapper.apply(employees);
String json = mapped.toJSON();
```

**Benefits:**
- Simple to implement (~2 hours)
- Solves 80% of use cases
- No external dependencies
- Works with existing code

### Phase 2: Nested Objects (If Needed) 🟡

**What to add:**
```java
public class NestedMapper {
    public NestedMapper mapNested(String from, String toPath);
    public String toNestedJSON(DataSet source);
}
```

**When to implement:**
- When you need nested object structures
- When API responses require specific nesting
- Requires more complex path parsing

### Phase 3: Full JSON Schema Support (Future) 🔴

**What to add:**
- JSON Schema parser
- Schema validation
- Automatic type conversion
- Grouping/aggregation driven by schema

**When to implement:**
- When you have complex nested array requirements
- When you need schema validation
- When multiple teams need different output formats

---

## Quick Win: Current Workarounds

You can achieve a lot **right now** without new code:

### Example: Custom JSON Structure

```java
// 1. Transform data with existing DSL
DataSet employees = getEmployees();

// 2. Build custom structure programmatically
List<Map<String, Object>> customStructure = employees.getRows().stream()
    .map(row -> {
        Map<String, Object> employee = new HashMap<>();
        employee.put("id", row.getInt("EMPLOYEE_ID"));
        employee.put("fullName", row.getString("NAME"));

        Map<String, Object> compensation = new HashMap<>();
        compensation.put("salary", row.getBigDecimal("SALARY"));
        compensation.put("currency", "USD");

        Map<String, Object> result = new HashMap<>();
        result.put("employee", employee);
        result.put("compensation", compensation);
        return result;
    })
    .collect(Collectors.toList());

// 3. Convert to JSON
ObjectMapper mapper = new ObjectMapper();
String json = mapper.writeValueAsString(customStructure);
```

**Pros:** Works today, no new code
**Cons:** Manual, verbose, not reusable

---

## Recommendation

### Implement Phase 1 (Simple Field Mapping) First

**Why:**
1. **High value, low effort** - Solves most real-world cases
2. **Foundation for later** - Can extend to nested structures
3. **No dependencies** - Pure Java solution
4. **Test easily** - Can add tests immediately

**Implementation Plan:**
1. Create `FieldMapper` class (1 hour)
2. Add `apply()` method to transform DataSet (30 min)
3. Add tests (1 hour)
4. Update documentation (30 min)

**Total: ~3 hours of work**

### Then Evaluate Need for Phase 2

After using Phase 1, you'll know if you need:
- Nested object support
- Array grouping
- Full schema validation

Most API/report use cases are satisfied by simple field mapping + existing transformations.

---

## Alternative: Jackson JsonViews

If you're mainly concerned about different API response shapes, consider **Jackson @JsonView**:

```java
public class Employee {
    @JsonView(Views.Public.class)
    private String name;

    @JsonView(Views.Internal.class)
    private BigDecimal salary;
}

// Then serialize with view
objectMapper.writerWithView(Views.Public.class)
    .writeValueAsString(employee);
```

This works well when you control the domain objects but doesn't help with dynamic DataSet transformations.

---

## Conclusion

**Yes, it's possible!** And actually pretty straightforward for common cases.

**Start with:** Simple field renaming (FieldMapper) - 80% of use cases
**Add later:** Nested structures if needed
**Full schema:** Only if you have complex hierarchical requirements

Would you like me to implement Phase 1 (FieldMapper) now?
