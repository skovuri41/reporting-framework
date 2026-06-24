# Quick Reference Guide

**Status:** ✅ All tests passing | **Branch:** `dataset-impl` | **Last Update:** 2026-06-24

---

## 🚀 Quick Commands

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=FullWorkflowIntegrationTest

# Run single test method
mvn test -Dtest=FullWorkflowIntegrationTest#testEmployeeSalaryAnalysis

# Clean build
mvn clean install
```

---

## 📂 Key Files

| File | Purpose | Status |
|------|---------|--------|
| `FullWorkflowIntegrationTest.java` | Main integration test - 4 scenarios | ✅ Passing |
| `DataSet.java` | Immutable dataset container | ✅ Complete |
| `DataRow.java` | Type-safe row wrapper | ✅ Complete |
| `DataQuery.java` | Fluent DSL builder | ✅ Complete |
| `DataOperations.java` | Joins & aggregations | ✅ Complete |

---

## ⚡ Common Patterns

### Filter & Select
```java
DataSet result = DataQuery.from(dataSet)
    .filter(row -> row.getInt("age") > 18)
    .select("name", "email")
    .execute();
```

### Join
```java
DataSet joined = DataOperations.innerJoin(
    employees, departments,
    "DEPT_ID", "DEPT_ID"
);
```

### Aggregate
```java
DataSet summary = DataQuery.from(joined)
    .groupBy("DEPARTMENT")
    .sum("SALARY")
    .count("EMPLOYEE_ID")
    .execute();
```

### Computed Column
```java
DataSet withBonus = DataQuery.from(employees)
    .withColumn("BONUS", row ->
        row.getBigDecimal("SALARY").multiply(BigDecimal.valueOf(0.1)))
    .execute();
```

---

## ⚠️ Known Limitations

### 1. Multiple Aggregations on Same Column
❌ **Doesn't work:**
```java
.groupBy("DEPT").avg("SALARY").sum("SALARY")  // sum overwrites avg
```

✅ **Workaround:**
```java
.groupBy("DEPT").sum("SALARY").count("ID")    // Different columns
```

### 2. Dependent Computed Columns
❌ **Doesn't work in same execute():**
```java
.withColumn("A", ...)
.withColumn("B", row -> row.get("A"))  // A is null!
.execute()
```

✅ **Workaround:**
```java
// Split into two execute() calls
DataSet step1 = DataQuery.from(ds).withColumn("A", ...).execute();
DataSet step2 = DataQuery.from(step1).withColumn("B", ...).execute();
```

### 3. H2 Stored Procedures
❌ H2 doesn't support callable stored procedures (CREATE ALIAS makes functions, not procedures)

✅ Tests create DataSets directly from queries (see `createDataSetFromQuery()`)

---

## 🐛 Debugging

### View row structure
```java
DataRow row = dataSet.first();
System.out.println("Keys: " + row.keys());
System.out.println("Data: " + row);
```

### Aggregation column names
After aggregation, columns follow pattern: `{COLUMN}_{function}`
- `.sum("SALARY")` → creates `SALARY_sum`
- `.count("ID")` → creates `ID_count`
- `.avg("SALARY")` → creates `SALARY_avg`

### Enable debug logs
```xml
<!-- logback-test.xml -->
<logger name="com.reporting.framework.data" level="DEBUG"/>
```

---

## 📊 Test Data (FullWorkflowIntegrationTest)

### Employees (7)
| ID | Name | Dept | Salary |
|----|------|------|--------|
| 1 | Alice Johnson | 10 | 80000 |
| 2 | Bob Smith | 20 | 90000 |
| 3 | Charlie Brown | 10 | 85000 |
| 4 | David Lee | 20 | 95000 |
| 5 | Eve Wilson | 10 | 82000 |
| 6 | Frank Miller | 30 | 70000 |
| 7 | Grace Davis | 20 | 88000 |

### Departments (3)
| ID | Name | Budget |
|----|------|--------|
| 10 | Engineering | 500000 |
| 20 | Sales | 750000 |
| 30 | Marketing | 300000 |

### Sales (9 records Q1 2024)
- Bob: $55k total
- David: $83k total
- Grace: $42k total

---

## 🎯 Test Scenarios

| Test | What it demonstrates |
|------|---------------------|
| testEmployeeSalaryAnalysis | Join, filter, select, orderBy |
| testDepartmentSalaryAggregation | GroupBy with aggregations |
| testSalesPerformanceAnalysis | Multi-table joins with filtering |
| testComprehensiveAnalysis | Complex workflow with computed columns |

---

## 🔧 Next Priority Items

1. **Fix aggregation API** - Support multiple aggs per column
2. **Test with SQL Server** - Enable stored procedure tests
3. **Fix computed column sequencing** - Apply to newRow not original row

---

## 📚 Full Details

See `IMPLEMENTATION_CONTEXT.md` for:
- Complete technical decisions and rationale
- Detailed known issues
- Usage patterns and examples
- Architecture overview
- Troubleshooting guide
