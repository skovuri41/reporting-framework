# Architecture Guide

Understanding how the framework works, design decisions, and future roadmap.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Core Components](#core-components)
3. [Execution Flow](#execution-flow)
4. [Design Principles](#design-principles)
5. [Metadata Model](#metadata-model)
6. [Future Roadmap](#future-roadmap)

---

## Architecture Overview

The framework uses a **metadata-driven, layered architecture** with three main layers:

```
┌─────────────────────────────────────────────────┐
│           Application Layer                      │
│  (Your ETL code, REST APIs, Reports)            │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│         Service Layer (ReportService)           │
│  • Execute stored procedures                    │
│  • Parameter filtering                          │
│  • Metadata caching                            │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│      Data Transformation Layer                  │
│  • DataSet (immutable collections)             │
│  • DataQuery (fluent DSL)                      │
│  • DataOperations (joins, pivots)              │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│        Metadata & Execution Layer               │
│  • MetadataLoader (cached metadata)            │
│  • StoredProcedureExecutor                     │
│  • ResultSetToMapConverter                     │
└──────────────────┬──────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────┐
│           Database Layer                        │
│  • SQL Server stored procedures                │
│  • Metadata stored procedure                   │
│  • Connection pooling                          │
└─────────────────────────────────────────────────┘
```

---

## Core Components

### 1. ReportService
**Purpose:** Main entry point for executing stored procedures

**Responsibilities:**
- Load metadata from cache or database
- Filter parameters based on metadata definitions
- Execute stored procedures via executor
- Return results as immutable DataSet

**Key Features:**
- Automatic cache warming on startup
- Parameter validation and filtering
- Output parameter extraction

### 2. MetadataLoader
**Purpose:** Load and cache stored procedure metadata

**Responsibilities:**
- Call metadata stored procedure
- Parse JSON metadata
- Cache metadata in-memory (ConcurrentHashMap)
- Handle cache invalidation

**Cache Strategy:**
- Warm cache on startup (loads all procedures)
- Lazy load on cache miss
- Thread-safe concurrent access
- Manual invalidation support

### 3. StoredProcedureExecutor
**Purpose:** Execute SQL Server stored procedures

**Responsibilities:**
- Build CallableStatement SQL
- Bind input parameters
- Register output parameters
- Execute procedure and return ResultSet

**Error Handling:**
- Validates required parameters
- Resource cleanup on errors
- Detailed exception messages

### 4. ResultSetToMapConverter
**Purpose:** Convert JDBC ResultSet to Maps with field name mapping

**Responsibilities:**
- Map SQL column names to camelCase field names
- Type conversion (SQL types → Java types)
- Support naming strategies (CAMEL_CASE, SNAKE_CASE, etc.)

**Field Name Mapping:**
```java
// SQL: EMPLOYEE_ID → Java: employeeId
// SQL: FIRST_NAME → Java: firstName
```

### 5. ParameterFilter
**Purpose:** Filter and validate input parameters

**Responsibilities:**
- Extract parameter definitions from metadata
- Filter superset of parameters to only those defined
- Validate required parameters are present
- Prevent null values on required parameters

### 6. DataSet
**Purpose:** Immutable collection of DataRow objects

**Characteristics:**
- Immutable (thread-safe)
- Contains metadata (report name, output parameters)
- JSON serialization support
- List-like access patterns

### 7. DataRow
**Purpose:** Type-safe wrapper over Map<String, Object>

**Features:**
- Type-safe getters (no casting)
- Null-safe with defaults
- Immutable transformations
- Builder pattern for creation

### 8. DataQuery
**Purpose:** Fluent DSL builder for transformations

**Operations:**
- filter, select, orderBy, limit, distinct
- withColumn (computed columns)
- groupBy with aggregations

**Execution Order:**
1. Filters
2. Computed columns
3. Select (column projection)
4. OrderBy
5. Distinct
6. Limit
7. GroupBy

### 9. DataOperations
**Purpose:** Complex operations (joins, pivots, unions)

**Operations:**
- Joins: inner, left, right, full outer, cross
- Unions: union, unionDistinct
- Pivots: row-to-column transformation
- Running totals: cumulative calculations

---

## Execution Flow

### Complete Request Flow

```
1. Application calls ReportService.execute(procedureId, params)
                    │
                    ▼
2. MetadataLoader retrieves metadata
   ├─ Check cache
   ├─ If miss: call usp_GetProcedureMetadata
   └─ Parse JSON and cache
                    │
                    ▼
3. ParameterFilter validates and filters parameters
   ├─ Check required parameters present
   ├─ Filter out undefined parameters
   └─ Return filtered parameter map
                    │
                    ▼
4. StoredProcedureExecutor executes procedure
   ├─ Build CallableStatement
   ├─ Bind input parameters
   ├─ Register output parameters
   ├─ Execute stored procedure
   └─ Return ExecutionResult (ResultSet + output params)
                    │
                    ▼
5. ResultSetToMapConverter converts to Maps
   ├─ Map SQL column names to camelCase
   ├─ Apply type conversions
   └─ Return List<Map<String, Object>>
                    │
                    ▼
6. DataRow.of() wraps each Map
                    │
                    ▼
7. Create DataSet with rows + output params
                    │
                    ▼
8. Return DataSet to application
                    │
                    ▼
9. Application applies transformations (optional)
   ├─ DataQuery: filter, select, groupBy, etc.
   └─ DataOperations: join, pivot, etc.
                    │
                    ▼
10. Export to JSON or process data
```

### Example Execution Trace

```java
// 1. Application call
DataSet employees = reportService.execute("emp_001",
    Map.of("departmentId", 10, "minSalary", 50000, "extra", "ignored"));

// 2. MetadataLoader
// - Cache hit: emp_001 metadata loaded
// - Metadata defines: departmentId (required), minSalary (optional)

// 3. ParameterFilter
// - Validates departmentId present (required)
// - Filters out "extra" parameter (not in metadata)
// - Returns: {departmentId=10, minSalary=50000}

// 4. StoredProcedureExecutor
// - SQL: {call usp_GetEmployees(?, ?)}
// - Binds: departmentId=10, minSalary=50000
// - Executes procedure
// - Returns ResultSet

// 5. ResultSetToMapConverter
// - Converts: EMPLOYEE_ID → employeeId
// - Converts: FIRST_NAME → firstName
// - Returns: List<Map<String, Object>>

// 6-8. Create DataSet
// - Wraps maps as DataRow objects
// - Creates DataSet with 5 rows
// - Returns to application

// 9. Application transforms
DataSet result = DataQuery.from(employees)
    .filter(row -> row.getBigDecimal("salary").compareTo(BigDecimal.valueOf(80000)) >= 0)
    .orderBy("salary").desc()
    .execute();

// 10. Export
String json = result.toJSON();
```

---

## Design Principles

### 1. Metadata-Driven Configuration

**Principle:** Add reports via metadata, not code

**Benefits:**
- No code deployment for new reports
- Centralized configuration
- Runtime flexibility

**Implementation:**
```sql
-- Add new report by inserting metadata
INSERT INTO STORED_PROCEDURE_METADATA (...)
VALUES (...);

-- Immediately available
DataSet data = service.execute("new_report", params);
```

### 2. Immutability

**Principle:** All data structures are immutable

**Benefits:**
- Thread-safe by default
- No side effects
- Easier reasoning about code

**Implementation:**
```java
DataSet original = service.execute("emp_001");

// Returns NEW DataSet, original unchanged
DataSet filtered = DataQuery.from(original)
    .filter(...)
    .execute();
```

### 3. Type Safety

**Principle:** Compile-time type checking, no casting

**Benefits:**
- Fewer runtime errors
- Better IDE support
- Self-documenting code

**Implementation:**
```java
// Type-safe access
Integer id = row.getInt("employeeId");           // Not: (Integer) row.get()
BigDecimal salary = row.getBigDecimal("salary"); // Not: (BigDecimal) row.get()
```

### 4. Fluent API

**Principle:** Readable, chainable operations

**Benefits:**
- Self-documenting code
- Natural to read and write
- Reduced boilerplate

**Implementation:**
```java
DataSet result = DataQuery.from(data)
    .filter(row -> row.getInt("age") > 21)
    .select("name", "age", "city")
    .orderBy("age").desc()
    .limit(10)
    .execute();
```

### 5. Separation of Concerns

**Principle:** Clear responsibility boundaries

**Implementation:**
- **DataSet**: Storage (immutable data holder)
- **DataQuery**: Transformation builder (fluent DSL)
- **DataOperations**: Complex operations (static utilities)
- **ReportService**: Orchestration (execute & coordinate)

### 6. Fail-Fast Validation

**Principle:** Validate early, fail with clear messages

**Implementation:**
```java
// Missing required parameter
MissingParameterException: Required parameter 'departmentId' is missing

// Invalid metadata JSON
MetadataParseException: Failed to parse metadata JSON for procedure: emp_001

// Procedure not found
MetadataNotFoundException: Metadata not found for procedure: emp_999
```

---

## Metadata Model

### Stored Procedure Metadata Structure

```json
{
  "procedureId": "emp_001",
  "procedureName": "usp_GetEmployees",
  "description": "Fetch employees by department",
  "parameters": [
    {
      "name": "DepartmentId",
      "fieldName": "departmentId",
      "type": "INTEGER",
      "nullable": false
    }
  ],
  "columns": [
    {
      "columnName": "EMPLOYEE_ID",
      "fieldName": "employeeId",
      "columnType": "INTEGER"
    }
  ],
  "outputParameters": [
    {
      "name": "TotalCount",
      "fieldName": "totalCount",
      "type": "INTEGER"
    }
  ]
}
```

### Key Concepts

**procedureId vs procedureName:**
- `procedureId`: Unique identifier for caching and lookup (e.g., "emp_001")
- `procedureName`: Actual SQL stored procedure name (e.g., "usp_GetEmployees")

**Field Name Mapping:**
- Maps database column names (UPPER_SNAKE_CASE) to Java field names (camelCase)
- Applied during ResultSet conversion
- Consistent across entire application

**Parameter Filtering:**
- `nullable: false` = required parameter (validated)
- `nullable: true` = optional parameter (can be null or missing)
- Superset of parameters filtered to only those defined

---

## Future Roadmap

### Phase 1: Enhanced Field Mapping ✅ (Partially Complete)

**Status:** Field name mapping implemented

**Current Capability:**
```java
// SQL columns mapped to camelCase
DataRow row = dataSet.first();
Integer id = row.getInt("employeeId");  // Not EMPLOYEE_ID
```

**Future Enhancement:** Custom field transformations
```java
// Planned
FieldMapper mapper = new FieldMapper()
    .rename("EMPLOYEE_ID", "id")
    .rename("FIRST_NAME", "fullName")
    .transform("SALARY", v -> ((BigDecimal) v).multiply(1.1));
```

### Phase 2: Nested Object Support

**Status:** Planned (Q3 2024)

**Goal:** Map flat rows to nested JSON objects

**Example:**
```json
{
  "id": 1,
  "name": "Alice",
  "contact": {
    "email": "alice@example.com",
    "phone": "+1-555-0100"
  },
  "address": {
    "street": "123 Main St",
    "city": "Boston"
  }
}
```

**Proposed API:**
```java
ObjectMapper mapper = new ObjectMapper()
    .nest("email", "phone").as("contact")
    .nest("street", "city", "state").as("address");

DataSet mapped = mapper.apply(employees);
```

### Phase 3: Hierarchical Arrays (Parent-Child)

**Status:** Planned (Q4 2024)

**Goal:** Group rows into hierarchical structures

**Example:**
```json
{
  "departments": [
    {
      "departmentId": 10,
      "departmentName": "Engineering",
      "employees": [
        {"id": 1, "name": "Alice"},
        {"id": 2, "name": "Bob"}
      ]
    }
  ]
}
```

**Proposed API:**
```java
HierarchyMapper mapper = HierarchyMapper.builder()
    .groupBy("departmentId", "departmentName")
        .childArray("employees")
            .fields("employeeId", "firstName", "lastName")
        .build()
    .build();

String json = mapper.apply(flatData).toJSON();
```

See [PHASE3_PLANNING.md](PHASE3_PLANNING.md) for detailed future roadmap.

### Phase 4: Report-Level Composition

**Status:** Research (2025)

**Goal:** Compose complex reports from multiple stored procedures

**Concept:**
```json
{
  "reportId": "dept_summary",
  "procedures": [
    {"procedureId": "emp_001", "alias": "employees"},
    {"procedureId": "dept_001", "alias": "departments"}
  ],
  "transformations": [
    {"type": "join", "left": "employees", "right": "departments"}
  ]
}
```

---

## Performance Considerations

### Metadata Caching
- **Warm on startup**: Loads all procedures once
- **Concurrent access**: Thread-safe ConcurrentHashMap
- **Manual invalidation**: Clear cache after metadata changes

### Connection Pooling
- Use HikariCP or similar for production
- Configure appropriate pool sizes
- Monitor connection usage

### Memory Management
- DataSet is in-memory (suitable for typical report sizes)
- For very large datasets (>100K rows), consider streaming or pagination
- Transformations create new DataSet instances

### Recommended Limits
- **Rows per DataSet**: < 50,000 for optimal performance
- **Metadata cache size**: < 1,000 procedures
- **Connection pool**: 10-50 connections depending on load

---

## Technology Stack

### Core Dependencies
- **Java 17**: Modern Java features (records, text blocks, sealed classes)
- **Jackson 2.17+**: JSON processing
- **SLF4J**: Logging facade
- **Google Guava**: Utility library (CaseFormat for naming strategies)

### Testing
- **JUnit 5**: Test framework
- **AssertJ**: Fluent assertions
- **H2 Database**: In-memory testing
- **Mockito**: Mocking framework

### Build
- **Maven**: Dependency management and build
- **Java 17 compiler**: Target and source compatibility

---

## Security Considerations

### SQL Injection Prevention
- All parameters passed via CallableStatement (parameterized)
- No dynamic SQL construction
- Parameters validated against metadata

### Connection Security
- Use encrypted connections (SSL/TLS)
- Store credentials securely (environment variables, vault)
- Use connection pooling with proper timeouts

### Data Access Control
- Database-level permissions control access
- Framework respects database security model
- No bypassing of stored procedure logic

---

## Monitoring and Logging

### Log Levels

**DEBUG:**
- Metadata cache hits/misses
- Parameter filtering details
- Transformation operations

**INFO:**
- Procedure execution start/end
- Cache warming status
- Row counts and execution summary

**WARN:**
- Cache warming failures (fallback to lazy load)
- Large result sets (>10K rows)
- Cross joins (potential performance issue)

**ERROR:**
- Metadata not found
- Required parameter missing
- Stored procedure execution failure

### Example Log Output
```
INFO  - ReportService initialized with metadata cache: true
INFO  - Warming metadata cache...
INFO  - Cache warmed with 45 procedures
INFO  - Executing procedure: usp_GetEmployees (ID: emp_001)
DEBUG - Metadata cache hit for procedure: emp_001
DEBUG - Passing 2 parameters to procedure: emp_001
INFO  - Procedure emp_001 executed successfully with 127 results
```

---

## Best Practices

### For ETL Developers
1. Use connection pooling in production
2. Monitor metadata cache effectiveness
3. Batch large data loads (don't load 1M rows in single DataSet)
4. Leverage parameter filtering (pass superset, framework filters)

### For Java Developers
1. Prefer type-safe getters over generic get()
2. Use try-with-resources for ReportService if needed
3. Don't modify DataRow or DataSet (they're immutable)
4. Chain transformations for readability

### For Operations
1. Monitor connection pool metrics
2. Set appropriate JVM heap size for data volume
3. Configure logging levels appropriately
4. Warm cache on startup for better performance

---

**For implementation details, see [Getting Started Guide](GETTING_STARTED.md) and [API Reference](API_REFERENCE.md)**
