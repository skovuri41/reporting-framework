# Metadata-Driven Java Reporting Framework

A lightweight, flexible framework for executing SQL Server stored procedures with powerful data transformation capabilities. Perfect for ETL pipelines, reporting systems, and data analytics.

## 🎯 Who Is This For?

- **ETL Developers**: Execute stored procedures, transform data, load to destinations
- **Java Developers**: Type-safe API with fluent DSL for data operations
- **Product Teams**: Understand capabilities for reporting and analytics solutions

## ⚡ Key Features

### Zero-Code Report Configuration
Add new reports by inserting metadata - no code changes required:
```sql
-- Define once in metadata, execute anywhere
INSERT INTO metadata...
```

### Powerful Data Transformations
```java
DataSet result = reportService.execute("employees", params);

// Fluent transformations
DataSet analysis = DataQuery.from(result)
    .filter(row -> row.getBigDecimal("salary").compareTo(BigDecimal.valueOf(85000)) >= 0)
    .withColumn("bonus", row -> row.getBigDecimal("salary").multiply(0.1))
    .orderBy("salary").desc()
    .execute();
```

### Advanced Operations
- **Joins**: Inner, left, right, full outer, cross
- **Aggregations**: Sum, avg, count, min, max with groupBy
- **Pivots**: Transform rows to columns
- **Computed Columns**: Add calculated fields on-the-fly
- **JSON Export**: Native JSON output for APIs and reporting

### Type-Safe Data Access
```java
DataRow employee = result.first();
Integer id = employee.getInt("employeeId");           // Type-safe
BigDecimal salary = employee.getBigDecimal("salary"); // No casting
LocalDate hired = employee.getLocalDate("hireDate");  // Java 8 time API
```

## 📊 Quick Examples

### Example 1: ETL Pipeline
```java
// Execute stored procedure
DataSet salesData = reportService.execute("monthly_sales",
    Map.of("month", "2024-01", "region", "US"));

// Transform data
DataSet aggregated = DataQuery.from(salesData)
    .groupBy("productCategory")
        .sum("amount")
        .count("transactionId")
    .execute();

// Export to JSON
String json = aggregated.toJSON();
// Send to API, save to file, load to data warehouse, etc.
```

### Example 2: Business Analytics
```java
// Multi-table analysis
DataSet employees = reportService.execute("employees");
DataSet departments = reportService.execute("departments");

// Join and analyze
DataSet analysis = DataOperations.innerJoin(
    employees, departments,
    "departmentId", "departmentId"
);

DataSet topDepts = DataQuery.from(analysis)
    .groupBy("departmentName")
        .avg("salary")
        .count("employeeId")
    .orderBy("salary_avg").desc()
    .limit(10)
    .execute();
```

### Example 3: Nested JSON for Reporting
```java
DataSet data = reportService.execute("department_summary");

// Create hierarchical structure
String nestedJson = buildNestedJson(data);
// {
//   "departments": [
//     {
//       "name": "Engineering",
//       "employees": [...]
//     }
//   ]
// }
```

## 🏗️ Architecture Overview

```
┌─────────────────┐
│   Your Code     │
│   (Java/ETL)    │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│         ReportService                   │
│  • Execute stored procedures            │
│  • Automatic parameter filtering        │
│  • Metadata-driven configuration        │
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│      Data Transformation Layer          │
│  • DataQuery (fluent DSL)              │
│  • DataOperations (joins, pivots)      │
│  • Type-safe DataRow access            │
└────────┬────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────┐
│      SQL Server Database                │
│  • Stored procedures                    │
│  • Metadata stored proc                 │
└─────────────────────────────────────────┘
```

## 🚀 Getting Started

### 1. Add Maven Dependency
```xml
<dependency>
    <groupId>com.reporting</groupId>
    <artifactId>reporting-framework</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Metadata Stored Procedure
```sql
CREATE PROCEDURE usp_GetProcedureMetadata
    @ProcedureId VARCHAR(100) = NULL,
    @MetadataJson NVARCHAR(MAX) OUTPUT
AS
-- Returns procedure metadata as JSON
-- See GETTING_STARTED.md for full implementation
```

### 3. Execute and Transform
```java
// Initialize
DataSource dataSource = ...; // HikariCP, etc.
ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
ReportService reportService = new ReportService(provider);

// Execute
DataSet data = reportService.execute("procedureId",
    Map.of("param1", value1, "param2", value2));

// Transform
DataSet result = DataQuery.from(data)
    .filter(...)
    .select(...)
    .orderBy(...)
    .execute();

// Export
String json = result.toJSON();
```

## 📚 Documentation

- **[Getting Started Guide](GETTING_STARTED.md)** - Step-by-step tutorial with examples
- **[API Reference](API_REFERENCE.md)** - Complete API documentation
- **[Architecture Guide](ARCHITECTURE.md)** - Design, patterns, and roadmap

## 💡 Use Cases

### ETL Pipelines
- Extract data from SQL Server stored procedures
- Transform with powerful DSL (filter, join, aggregate)
- Load to any destination (JSON, CSV, database, API)

### Reporting Systems
- Execute parameterized reports
- Apply business logic transformations
- Generate JSON for dashboards and APIs

### Data Analytics
- Multi-table joins and aggregations
- Running totals and window functions
- Pivot tables and data reshaping

### Microservices
- Lightweight framework (~10MB JAR)
- Fast execution with metadata caching
- JSON-first output for REST APIs

## 🎯 Design Principles

- **Metadata-Driven**: Add reports via configuration, not code
- **Immutable**: All transformations return new DataSet instances
- **Type-Safe**: No casting - clean API with proper types
- **Fluent API**: Readable, chainable operations
- **Lightweight**: Minimal dependencies, no Spark/Hadoop

## 📋 Requirements

- **Java**: 17 or higher
- **Database**: SQL Server (or H2 for testing)
- **Dependencies**:
  - Jackson 2.17+ (JSON)
  - SLF4J (logging)
  - JDBC driver for your database

## 🔧 Technology Stack

- **Java 17**: Modern Java with records, text blocks
- **Jackson**: JSON processing and serialization
- **SLF4J/Logback**: Structured logging
- **JUnit 5**: Testing framework
- **Maven**: Build and dependency management

## 🤝 Contributing

This framework is designed for internal use. For questions or issues, contact the development team.

## 📝 License

Proprietary - Internal Use Only

---

## Quick Links

- 📖 [Getting Started](GETTING_STARTED.md) - Your first report in 10 minutes
- 🔍 [API Reference](API_REFERENCE.md) - Complete API documentation
- 🏛️ [Architecture](ARCHITECTURE.md) - How it works under the hood
- 🔬 [Research](TRANSFORMATION_ARCHITECTURE_RESEARCH.md) - Advanced transformation patterns

---

**Version**: 1.0.0-SNAPSHOT
**Last Updated**: 2024-06-29
