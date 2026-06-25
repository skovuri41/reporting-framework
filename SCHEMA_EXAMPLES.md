# JSON Schema Mapping - Concrete Examples

Using employee data from FullWorkflowIntegrationTest to show real transformations.

---

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
  },
  {
    "EMPLOYEE_ID": 3,
    "NAME": "Charlie Brown",
    "SALARY": 85000.00,
    "DEPARTMENT_NAME": "Engineering"
  }
]
```

**Problem:** Column names are database-style (UPPER_SNAKE_CASE), no nesting, no control over structure.

---

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
    },
    "required": ["id", "fullName", "salary", "department"]
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
  },
  {
    "id": 3,
    "fullName": "Charlie Brown",
    "salary": 85000.00,
    "department": "Engineering"
  }
]
```

### What Changed:
✅ Field names match API conventions (camelCase)
✅ Column selection/filtering
✅ Works with existing DataSet structure
✅ Still flat array of objects

### What's Still Missing:
❌ No nested objects
❌ No constant values
❌ No computed fields

---

## Phase 2: Nested Objects 🟡

### Use Case: Structured API Response with Grouping

**Target JSON Schema:**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
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
          "position": {"type": "string"},
          "startDate": {"type": "string", "format": "date"}
        }
      },
      "compensation": {
        "type": "object",
        "properties": {
          "annualSalary": {"type": "number"},
          "currency": {"type": "string"},
          "payFrequency": {"type": "string"}
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
    .mapNested("HIRE_DATE", "employment.startDate")

    // Map to nested compensation object
    .mapNested("SALARY", "compensation.annualSalary")
    .mapConstant("compensation.currency", "USD")
    .mapConstant("compensation.payFrequency", "monthly");

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
      "position": "Full-time",
      "startDate": "2020-01-15"
    },
    "compensation": {
      "annualSalary": 80000.00,
      "currency": "USD",
      "payFrequency": "monthly"
    }
  },
  {
    "employee": {
      "id": 2,
      "fullName": "Bob Smith"
    },
    "employment": {
      "department": "Sales",
      "position": "Full-time",
      "startDate": "2019-03-10"
    },
    "compensation": {
      "annualSalary": 90000.00,
      "currency": "USD",
      "payFrequency": "monthly"
    }
  }
]
```

### What Changed:
✅ Nested object structures
✅ Logical grouping of related fields
✅ Constant value injection
✅ Dot-path notation (employee.id)

### What's Still Missing:
❌ No grouping/aggregation into nested arrays
❌ Still one JSON object per DataSet row
❌ Can't create parent-child relationships

---

## Phase 3: Grouped Nested Arrays 🔴

### Use Case: Hierarchical Report/Dashboard Data

**Scenario:** You want departments as parent objects, with employees as nested arrays.

**Target JSON Schema:**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
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
                    "salary": {"type": "number"},
                    "performance": {
                      "type": "object",
                      "properties": {
                        "sales": {"type": "number"},
                        "rating": {"type": "string"}
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
    .aggregate("EMPLOYEE_ID", "company.departments[].headcount", AggregationFunction.COUNT)

    // Nest employees within each department
    .nestArray("company.departments[].employees[]")
        .mapNested("EMPLOYEE_ID", "id")
        .mapNested("NAME", "name")
        .mapNested("SALARY", "salary")

        // Further nest performance data
        .nestObject("performance")
            .mapNested("TOTAL_SALES", "sales")
            .mapComputed("rating", row ->
                row.getDouble("TOTAL_SALES") > 75000 ? "Excellent" : "Good");

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
          {
            "id": 1,
            "name": "Alice Johnson",
            "salary": 80000.00,
            "performance": {
              "sales": null,
              "rating": "Good"
            }
          },
          {
            "id": 3,
            "name": "Charlie Brown",
            "salary": 85000.00,
            "performance": {
              "sales": null,
              "rating": "Good"
            }
          },
          {
            "id": 5,
            "name": "Eve Wilson",
            "salary": 82000.00,
            "performance": {
              "sales": null,
              "rating": "Good"
            }
          }
        ]
      },
      {
        "name": "Sales",
        "budget": 750000.00,
        "headcount": 3,
        "employees": [
          {
            "id": 2,
            "name": "Bob Smith",
            "salary": 90000.00,
            "performance": {
              "sales": 55000.00,
              "rating": "Good"
            }
          },
          {
            "id": 4,
            "name": "David Lee",
            "salary": 95000.00,
            "performance": {
              "sales": 83000.00,
              "rating": "Excellent"
            }
          },
          {
            "id": 7,
            "name": "Grace Davis",
            "salary": 88000.00,
            "performance": {
              "sales": 42000.00,
              "rating": "Good"
            }
          }
        ]
      },
      {
        "name": "Marketing",
        "budget": 300000.00,
        "headcount": 1,
        "employees": [
          {
            "id": 6,
            "name": "Frank Miller",
            "salary": 70000.00,
            "performance": {
              "sales": null,
              "rating": "Good"
            }
          }
        ]
      }
    ]
  }
}
```

### What Changed:
✅ Complete hierarchical structure
✅ Grouped data (departments contain employees)
✅ Nested arrays at multiple levels
✅ Aggregations within groups (headcount)
✅ Computed fields based on business logic
✅ Root object wrapping
✅ Multiple levels of nesting

---

## Side-by-Side Comparison

### Phase 1: Flat with Renamed Fields
```json
[
  {"id": 1, "fullName": "Alice Johnson", "salary": 80000, "department": "Engineering"},
  {"id": 2, "fullName": "Bob Smith", "salary": 90000, "department": "Sales"}
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
          {"id": 1, "name": "Alice Johnson", "salary": 80000},
          {"id": 3, "name": "Charlie Brown", "salary": 85000}
        ]
      }
    ]
  }
}
```

---

## Real-World Use Cases

### Phase 1: API Responses (Most Common)
**Who needs it:**
- REST API endpoints
- Mobile app backends
- Microservices communication
- Simple data exports

**Example:**
```
GET /api/employees → Returns flat array with clean field names
```

### Phase 2: Complex APIs
**Who needs it:**
- Enterprise APIs with structured responses
- GraphQL-like outputs
- Systems requiring logical field grouping
- Multi-concern data (personal info, employment info, benefits, etc.)

**Example:**
```
GET /api/employees/profile → Returns nested object with grouped sections
```

### Phase 3: Dashboards & Reports
**Who needs it:**
- Dashboard aggregations
- Hierarchical reports (org charts, etc.)
- Parent-child data relationships
- Complex BI exports
- Document generation (PDF reports, etc.)

**Example:**
```
GET /api/org/structure → Returns full org hierarchy with nested departments/employees
```

---

## Complexity vs Value Matrix

| Phase | Complexity | Value | Use Cases | Implementation Time |
|-------|-----------|-------|-----------|-------------------|
| **Phase 1** | ⭐ Low | ⭐⭐⭐⭐⭐ Very High | 80% of needs | 3 hours |
| **Phase 2** | ⭐⭐⭐ Medium | ⭐⭐⭐ High | 15% of needs | 1-2 days |
| **Phase 3** | ⭐⭐⭐⭐⭐ Very High | ⭐⭐ Medium | 5% of needs | 1 week |

---

## Decision Guide

### Choose Phase 1 if:
✅ You need clean field names (camelCase, different naming)
✅ Your API returns flat objects or simple arrays
✅ You want quick wins with minimal effort
✅ You're building REST APIs with standard responses

### Choose Phase 2 if:
✅ Your API response has logical sections (user profile, settings, etc.)
✅ You need to add constant values (currency, version, etc.)
✅ Your schema has nested objects but not nested arrays
✅ You want better organization without full hierarchy

### Choose Phase 3 if:
✅ You need parent-child relationships (departments → employees)
✅ You're building hierarchical reports or org charts
✅ Your data model is truly tree-like
✅ You need aggregations at multiple levels
✅ You're willing to invest significant development time

---

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
- When API consumers request better structure

### Only If Required: Phase 3
**When:**
- You have explicit hierarchical data requirements
- Phase 2 solutions become too complex
- You need true parent-child relationships

---

## Alternative: Current Workaround

**You can achieve Phase 2 TODAY** with existing code:

```java
// Get data
DataSet employees = getEmployees();

// Build nested structure manually
List<Map<String, Object>> result = employees.getRows().stream()
    .map(row -> {
        Map<String, Object> output = new HashMap<>();

        // Employee section
        Map<String, Object> employee = new HashMap<>();
        employee.put("id", row.getInt("EMPLOYEE_ID"));
        employee.put("fullName", row.getString("NAME"));
        output.put("employee", employee);

        // Compensation section
        Map<String, Object> compensation = new HashMap<>();
        compensation.put("annualSalary", row.getBigDecimal("SALARY"));
        compensation.put("currency", "USD");
        output.put("compensation", compensation);

        return output;
    })
    .collect(Collectors.toList());

// Convert to JSON
String json = objectMapper.writeValueAsString(result);
```

**Pros:** Works now, no new code needed
**Cons:** Verbose, error-prone, not reusable, no type safety

---

## Summary

**Phase 1** = 80% solution, 5% effort → **Start here**
**Phase 2** = 95% solution, 20% effort → Add if needed
**Phase 3** = 100% solution, 75% effort → Only if truly required

Most projects never need Phase 3. Many are happy with Phase 1 + manual nesting for edge cases.

---

## Next Step Question

**What kind of JSON output do you need?**

Show me an example of:
1. Your current DataSet data (what you have)
2. Your desired JSON output (what you want)

I can then tell you which phase you need and show exactly how to achieve it.
