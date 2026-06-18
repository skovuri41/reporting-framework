# Reporting Framework: Approach Comparison

This document compares the three approaches available in this repository to help you choose the right one for your use case.

## Quick Decision Matrix

| Your Requirement | Recommended Branch |
|-----------------|-------------------|
| **Type-safe results, stable schemas** | `main` (POJO-based) |
| **Dynamic schemas, no POJO classes** | `lightweight-dynamic` |
| **True distributed computing, >1M rows** | `spark-impl` |
| **Spring Boot/Tomcat deployment** | `lightweight-dynamic` or `main` |
| **Minimal dependencies** | `lightweight-dynamic` or `main` |
| **Need Spark ML/streaming** | `spark-impl` |
| **Fast startup (<1s)** | `lightweight-dynamic` or `main` |
| **Low memory (<200MB)** | `lightweight-dynamic` or `main` |

---

## Three Approaches Explained

### 1. POJO-Based (main branch) - Type-Safe

**Philosophy:** Explicit type safety through Java POJOs and explicit column mappings.

**Pros:**
- ✓ Type-safe results: `ReportResult<EmployeeReport>`
- ✓ Compile-time type checking
- ✓ Explicit column-to-field mappings (self-documenting)
- ✓ Small JAR size (~9MB)
- ✓ Fast startup (<1s)
- ✓ Low memory (50-100MB heap)
- ✓ No dependency conflicts

**Cons:**
- ✗ Must create POJO class for each report
- ✗ Must define explicit column mappings in metadata JSON
- ✗ Less flexible for runtime transformations
- ✗ Schema changes require code updates

**Use When:**
- You want type safety (compile-time checking)
- Report schemas are stable and well-defined
- You prefer explicit over implicit
- Building traditional REST APIs with typed responses

**Example:**
```java
// Define POJO
public class EmployeeReport {
    private Integer employeeId;
    private String name;
    private BigDecimal salary;
    // getters/setters
}

// Execute
ReportResult<EmployeeReport> result = reportService.execute("employees", params);
List<EmployeeReport> employees = result.getResults();

// Type-safe access
BigDecimal salary = employees.get(0).getSalary();
```

---

### 2. Lightweight Dynamic (lightweight-dynamic branch) - Flexible

**Philosophy:** Dynamic schemas using Java collections, no POJO classes needed.

**Pros:**
- ✓ No POJO classes required
- ✓ No explicit column mappings (auto-inferred)
- ✓ Flexible transformation API (filter, join, groupBy)
- ✓ Small JAR size (~10MB)
- ✓ Fast startup (<1s)
- ✓ Low memory (100-200MB heap)
- ✓ Easy JSON output
- ✓ Perfect for Tomcat/Spring Boot

**Cons:**
- ✗ No compile-time type checking
- ✗ Runtime ClassCastException possible
- ✗ Slightly more verbose (casting required)

**Use When:**
- You don't want to maintain POJO classes
- Report schemas change frequently
- You need dynamic transformations (filter, join, aggregate)
- Deploying to Tomcat or Spring Boot
- Data volume < 1M rows per report

**Example:**
```java
// Execute (no POJO needed)
DynamicReportResult result = reportService.executeDynamic("employees", params);

// Transform
DynamicReportResult highEarners = result
    .filter(row -> ((BigDecimal) row.get("salary")).compareTo(BigDecimal.valueOf(85000)) > 0)
    .select("name", "salary")
    .orderBy("salary", false);

// Get JSON
String json = highEarners.toJSON();
```

---

### 3. Spark-Based (spark-impl branch) - Big Data

**Philosophy:** Use Apache Spark Dataset/DataFrame API for big data processing.

**Pros:**
- ✓ Powerful transformation API (Spark SQL, Dataset ops)
- ✓ Can scale to Spark clusters (Databricks, EMR)
- ✓ Advanced features (ML, streaming, etc.)
- ✓ Lazy evaluation and query optimization
- ✓ Columnar storage efficiency (for large data)

**Cons:**
- ✗ Large JAR size (~300MB)
- ✗ Slow startup (8-15 seconds for SparkSession)
- ✗ High memory overhead (2GB+ heap)
- ✗ 80+ transitive dependencies
- ✗ High risk of dependency conflicts
- ✗ Overkill for single-node, small datasets
- ✗ Poor fit for Tomcat/Spring Boot

**Use When:**
- Data volume regularly exceeds 1M rows
- You need true distributed computing
- You have Spark infrastructure (Databricks, EMR)
- You need Spark ML or streaming features
- You're already using Spark elsewhere

**Example:**
```java
// Execute
SparkReportResult result = reportService.execute("employees", params);
Dataset<Row> dataset = result.getDataset();

// Spark transformations
Dataset<Row> highEarners = dataset.filter("salary > 85000");
Dataset<Row> sorted = highEarners.orderBy(col("salary").desc());

// Get JSON
String json = sorted.toJSON().collectAsList().toString();
```

---

## Detailed Comparison

### Resource Footprint

| Aspect | POJO-Based | Lightweight Dynamic | Spark-Based |
|--------|------------|-------------------|-------------|
| **JAR Size** | ~9MB | ~10MB | ~300MB |
| **Startup Time** | <1s | <1s | 8-15s |
| **Memory (Heap)** | 50-100MB | 100-200MB | 2GB+ |
| **Dependencies** | 5 | 5 | 80+ |
| **Conflict Risk** | Low | Low | High |

### Feature Matrix

| Feature | POJO-Based | Lightweight Dynamic | Spark-Based |
|---------|------------|-------------------|-------------|
| **Type Safety** | ✓ | ✗ | ✗ |
| **Auto-Infer Columns** | ✗ | ✓ | ✓ |
| **Filter/Select** | Manual | ✓ | ✓ |
| **Join Operations** | Manual | ✓ | ✓ |
| **Aggregations** | Manual | ✓ | ✓ |
| **JSON Output** | Via Jackson | ✓ | ✓ |
| **POJO Classes** | Required | Optional | Optional |
| **Column Mappings** | Required | Optional | Optional |
| **Distributed Computing** | ✗ | ✗ | ✓ |
| **Spark ML** | ✗ | ✗ | ✓ |
| **Tomcat-Friendly** | ✓ | ✓ | ✗ |

### Development Effort

| Task | POJO-Based | Lightweight Dynamic | Spark-Based |
|------|------------|-------------------|-------------|
| **Add New Report** | Medium | Low | Low |
| **Change Schema** | High (update POJO + mappings) | Low (auto-inferred) | Low (auto-inferred) |
| **Complex Transform** | High (manual code) | Medium (API) | Low (Spark SQL) |
| **Debug Issues** | Easy (typed) | Medium (casting) | Medium (Spark logs) |
| **Learning Curve** | Low | Low | High |

### Data Volume Guidelines

| Rows per Report | POJO-Based | Lightweight Dynamic | Spark-Based |
|----------------|------------|-------------------|-------------|
| **< 10K** | Excellent | Excellent | Overkill |
| **10K - 100K** | Good | Excellent | Overkill |
| **100K - 1M** | OK (paginate) | Good | Good |
| **> 1M** | Poor | Poor | Excellent |

---

## Migration Paths

### From POJO-Based to Lightweight Dynamic

**Effort:** Low (1-2 hours per report)

**Steps:**
1. Update metadata JSON: set `resultClass` to `"java.util.Map"`
2. Remove `resultSetMapping` section (or empty it)
3. Update code: replace `ReportResult<T>` with `DynamicReportResult`
4. Update code: replace `getResults()` with `getRows()`
5. Add casting where needed: `(BigDecimal) row.get("salary")`

**Benefits:**
- No more POJO maintenance
- Auto-inferred columns
- Transformation API

**Tradeoffs:**
- Lose compile-time type safety
- Need runtime casting

---

### From Spark to Lightweight Dynamic

**Effort:** Medium (4-8 hours per report)

**Steps:**
1. Remove Spark dependency from `pom.xml`
2. Replace `SparkReportResult` with `DynamicReportResult`
3. Replace `Dataset<Row>` operations with DynamicReportResult API
4. Replace Spark SQL strings with Java predicates
5. Replace `toJSON().collectAsList()` with `toJSON()`
6. Remove SparkSession initialization code

**Benefits:**
- 33x smaller JAR
- 15x faster startup
- 10x less memory
- No dependency conflicts

**Tradeoffs:**
- Lose distributed computing capability
- Lose Spark ML features
- More verbose (Java predicates vs SQL strings)

---

### From Lightweight Dynamic to Spark

**Effort:** Medium (4-8 hours per report)

**Steps:**
1. Add Spark dependency to `pom.xml`
2. Add SparkSession management
3. Replace `DynamicReportResult` with `SparkReportResult`
4. Replace Java predicates with Spark SQL strings
5. Convert joins to Spark DataFrame joins

**When to Do This:**
- Data volume exceeds 1M rows regularly
- Need to deploy to Spark clusters
- Need Spark ML or streaming

**Tradeoffs:**
- 33x larger JAR
- 15x slower startup
- 10x more memory
- Dependency conflict risk

---

## Real-World Scenarios

### Scenario 1: Spring Boot REST API

**Requirement:**
- Expose 20 reports as REST endpoints
- Reports return JSON
- Data volume: 5K-50K rows per report
- Deployment: Tomcat on AWS EC2 (t3.medium)

**Recommendation:** `lightweight-dynamic`

**Why:**
- Small memory footprint fits t3.medium (4GB RAM)
- Fast startup for auto-scaling
- Easy JSON output
- No POJO maintenance burden

---

### Scenario 2: Enterprise Dashboard

**Requirement:**
- 10 reports with stable schemas
- TypeScript frontend expects typed JSON
- Data volume: 10K-100K rows
- Deployment: Tomcat on-premise

**Recommendation:** `main` (POJO-based)

**Why:**
- Type safety matches TypeScript types
- Explicit schemas = better documentation
- Low resource usage
- Schemas are stable

---

### Scenario 3: Data Analytics Platform

**Requirement:**
- 100+ dynamic reports
- Schemas change monthly
- Data volume: 100K-10M rows
- Deployment: Databricks

**Recommendation:** `spark-impl`

**Why:**
- Already have Spark infrastructure
- Data volume requires distributed computing
- Dynamic schemas benefit from Spark's flexibility
- May need Spark ML later

---

### Scenario 4: Microservice Architecture

**Requirement:**
- 50+ microservices, each with 1-3 reports
- Containers (Docker/Kubernetes)
- 512MB memory limit per container
- Data volume: 1K-10K rows

**Recommendation:** `lightweight-dynamic`

**Why:**
- Fits 512MB memory limit
- Small JAR = faster container builds
- Dynamic approach reduces code per service
- Fast startup for rolling deployments

---

## Decision Tree

```
Start Here
│
├─ Do you have Spark infrastructure (Databricks, EMR)?
│  │
│  ├─ YES → Do you need distributed computing (>1M rows)?
│  │  │
│  │  ├─ YES → Use spark-impl
│  │  └─ NO  → Use lightweight-dynamic (don't pay for Spark overhead)
│  │
│  └─ NO  → Continue below
│
├─ Are your report schemas stable and well-defined?
│  │
│  ├─ YES → Do you value compile-time type safety?
│  │  │
│  │  ├─ YES → Use main (POJO-based)
│  │  └─ NO  → Use lightweight-dynamic
│  │
│  └─ NO (schemas change frequently)
│     │
│     └─ Use lightweight-dynamic
│
└─ Deploying to Tomcat/Spring Boot with <1GB memory?
   │
   ├─ YES → Avoid spark-impl, use lightweight-dynamic or main
   └─ NO  → Can use any approach based on other factors
```

---

## Summary Recommendations

### Choose `main` (POJO-based) if:
- ✓ You want compile-time type safety
- ✓ Schemas are stable
- ✓ You prefer explicit over implicit
- ✓ Building traditional REST APIs

### Choose `lightweight-dynamic` if:
- ✓ You don't want to maintain POJO classes
- ✓ Schemas change frequently
- ✓ You need transformation API (filter/join/aggregate)
- ✓ Deploying to Tomcat/Spring Boot
- ✓ Data volume < 1M rows

### Choose `spark-impl` if:
- ✓ Data volume > 1M rows regularly
- ✓ You have Spark infrastructure
- ✓ You need distributed computing
- ✓ You need Spark ML or streaming

**For 90% of Spring Boot/Tomcat deployments:** `lightweight-dynamic` is the best choice.

**For the remaining 10%:** Use `main` if you need type safety, or `spark-impl` if you have Spark infrastructure and truly big data.
