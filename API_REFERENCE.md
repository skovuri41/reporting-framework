# API Reference

Complete API documentation for the Metadata-Driven Java Reporting Framework.

## Table of Contents

1. [ReportService](#reportservice)
2. [DataSet](#dataset)
3. [DataRow](#datarow)
4. [DataQuery](#dataquery)
5. [DataOperations](#dataoperations)
6. [Exceptions](#exceptions)

---

## ReportService

Main entry point for executing stored procedures and retrieving data.

### Constructor

```java
// With default settings (cache enabled)
public ReportService(ConnectionProvider connectionProvider)

// With cache control
public ReportService(ConnectionProvider connectionProvider, boolean enableMetadataCache)
```

**Parameters:**
- `connectionProvider` - Provider for database connections
- `enableMetadataCache` - Enable/disable metadata caching (default: true)

**Example:**
```java
ConnectionProvider provider = new DataSourceConnectionProvider(dataSource);
ReportService service = new ReportService(provider);
```

### execute()

Execute a stored procedure and return results as DataSet.

```java
public DataSet execute(String procedureId, Map<String, Object> inputParameters)
public DataSet execute(String procedureId)
```

**Parameters:**
- `procedureId` - Unique identifier for the stored procedure
- `inputParameters` - Map of parameter names to values (optional)

**Returns:** `DataSet` containing results with camelCase field names

**Throws:**
- `MetadataNotFoundException` - Procedure metadata not found
- `MissingParameterException` - Required parameter missing or null
- `ReportExecutionException` - Execution failed

**Example:**
```java
DataSet employees = service.execute("emp_001",
    Map.of("departmentId", 10, "minSalary", 50000));

DataSet allDepts = service.execute("dept_001"); // No parameters
```

### Cache Management

```java
// Invalidate cache for specific procedure
public void invalidateMetadataCache(String procedureId)

// Clear entire cache
public void clearMetadataCache()

// Get cache size
public int getMetadataCacheSize()
```

**Example:**
```java
service.invalidateMetadataCache("emp_001");
service.clearMetadataCache();
int size = service.getMetadataCacheSize();
```

---

## DataSet

Immutable collection of DataRow objects with metadata.

### Basic Properties

```java
// Get row count
public int count()

// Check if empty
public boolean isEmpty()

// Get report name
public String getReportName()
```

**Example:**
```java
DataSet data = service.execute("emp_001", params);

int rowCount = data.count();
boolean empty = data.isEmpty();
String name = data.getReportName();
```

### Accessing Rows

```java
// Get all rows
public List<DataRow> getRows()

// Get first row
public DataRow first()

// Get last row
public DataRow last()

// Get specific row by index
public DataRow getRow(int index)
```

**Example:**
```java
DataRow first = data.first();
DataRow last = data.last();
DataRow fifth = data.getRow(4); // Zero-indexed

for (DataRow row : data.getRows()) {
    // Process each row
}
```

### Output Parameters

```java
// Get output parameter value
public Object getOutputParameter(String name)

// Check if output parameter exists
public boolean hasOutputParameter(String name)

// Get all output parameters
public Map<String, Object> getOutputParameters()
```

**Example:**
```java
if (data.hasOutputParameter("totalCount")) {
    Integer total = (Integer) data.getOutputParameter("totalCount");
}
```

### JSON Export

```java
// Compact JSON
public String toJSON()

// Pretty-printed JSON
public String toPrettyJSON()

// Convert to list of maps
public List<Map<String, Object>> toMaps()
```

**Example:**
```java
String json = data.toJSON();
String prettyJson = data.toPrettyJSON();
List<Map<String, Object>> maps = data.toMaps();
```

---

## DataRow

Immutable wrapper over Map with type-safe accessors.

### Type-Safe Getters

All getters have two variants: one that throws on missing key, one with default value.

```java
// String
public String getString(String key)
public String getString(String key, String defaultValue)

// Integer
public Integer getInt(String key)
public Integer getInt(String key, Integer defaultValue)

// Long
public Long getLong(String key)
public Long getLong(String key, Long defaultValue)

// Double
public Double getDouble(String key)
public Double getDouble(String key, Double defaultValue)

// BigDecimal
public BigDecimal getBigDecimal(String key)
public BigDecimal getBigDecimal(String key, BigDecimal defaultValue)

// Boolean
public Boolean getBoolean(String key)
public Boolean getBoolean(String key, Boolean defaultValue)

// LocalDate
public LocalDate getLocalDate(String key)

// LocalDateTime
public LocalDateTime getLocalDateTime(String key)

// LocalTime
public LocalTime getLocalTime(String key)

// Generic (returns Object)
public Object get(String key)
```

**Example:**
```java
DataRow row = data.first();

String name = row.getString("firstName");
Integer id = row.getInt("employeeId");
BigDecimal salary = row.getBigDecimal("salary");
LocalDate hired = row.getLocalDate("hireDate");

// With defaults
String dept = row.getString("department", "Unknown");
Integer bonus = row.getInt("bonus", 0);
```

### Utility Methods

```java
// Check if key exists
public boolean has(String key)

// Get all keys
public Set<String> keys()

// Get raw map
public Map<String, Object> toMap()
```

**Example:**
```java
if (row.has("managerId")) {
    Integer managerId = row.getInt("managerId");
}

Set<String> columns = row.keys();
Map<String, Object> map = row.toMap();
```

### Immutable Transformations

```java
// Add/update field
public DataRow with(String key, Object value)

// Select specific fields
public DataRow select(String... keys)

// Remove field
public DataRow without(String key)
```

**Example:**
```java
// Original row is unchanged
DataRow newRow = row.with("bonus", BigDecimal.valueOf(5000));

// Select subset of fields
DataRow subset = row.select("firstName", "lastName", "salary");

// Remove field
DataRow minimal = row.without("internalId");
```

### Builder

```java
// Create DataRow from scratch
DataRow row = DataRow.builder()
    .put("employeeId", 1)
    .put("firstName", "Alice")
    .put("salary", BigDecimal.valueOf(85000))
    .build();

// Create from Map
DataRow row = DataRow.of(map);
```

---

## DataQuery

Fluent DSL builder for transformations. All operations are immutable - each returns a new DataSet.

### Creating Query

```java
// Start query from DataSet
DataQuery query = DataQuery.from(dataSet);
```

### Filter

```java
public DataQuery filter(Predicate<DataRow> predicate)
```

**Example:**
```java
DataSet result = DataQuery.from(employees)
    .filter(row -> row.getInt("departmentId") == 10)
    .filter(row -> row.getBigDecimal("salary")
        .compareTo(BigDecimal.valueOf(80000)) >= 0)
    .execute();
```

### Select

```java
public DataQuery select(String... columnNames)
```

**Example:**
```java
DataSet result = DataQuery.from(employees)
    .select("firstName", "lastName", "salary")
    .execute();
```

### Order By

```java
public OrderByBuilder orderBy(String column)

// OrderByBuilder methods:
public DataQuery asc()
public DataQuery desc()
```

**Example:**
```java
// Ascending
DataSet result = DataQuery.from(employees)
    .orderBy("lastName").asc()
    .execute();

// Descending
DataSet result = DataQuery.from(employees)
    .orderBy("salary").desc()
    .execute();
```

### Limit

```java
public DataQuery limit(int maxRows)
```

**Example:**
```java
DataSet top10 = DataQuery.from(employees)
    .orderBy("salary").desc()
    .limit(10)
    .execute();
```

### Distinct

```java
public DataQuery distinct()
```

**Example:**
```java
DataSet unique = DataQuery.from(data)
    .select("departmentId", "departmentName")
    .distinct()
    .execute();
```

### Computed Columns

```java
public DataQuery withColumn(String columnName, Function<DataRow, Object> valueGenerator)
```

**Example:**
```java
DataSet result = DataQuery.from(employees)
    .withColumn("fullName", row ->
        row.getString("firstName") + " " + row.getString("lastName"))

    .withColumn("bonus", row ->
        row.getBigDecimal("salary").multiply(BigDecimal.valueOf(0.1)))

    .withColumn("yearsEmployed", row -> {
        LocalDate hired = row.getLocalDate("hireDate");
        return Period.between(hired, LocalDate.now()).getYears();
    })

    .execute();
```

### Group By and Aggregations

```java
public GroupByBuilder groupBy(String... columns)

// GroupByBuilder methods:
public GroupByBuilder sum(String column)
public GroupByBuilder avg(String column)
public GroupByBuilder count(String column)
public GroupByBuilder min(String column)
public GroupByBuilder max(String column)
public DataSet execute()
```

**Aggregation Column Naming:** `{column}_{function}`
- `sum("salary")` creates column `salary_sum`
- `avg("salary")` creates column `salary_avg`
- `count("id")` creates column `id_count`

**Example:**
```java
DataSet summary = DataQuery.from(employees)
    .groupBy("departmentId")
        .avg("salary")
        .count("employeeId")
        .max("salary")
        .min("salary")
        .sum("salary")
    .execute();

for (DataRow row : summary.getRows()) {
    Integer dept = row.getInt("departmentId");
    Double avgSalary = row.getDouble("salary_avg");
    Long empCount = row.getLong("employeeId_count");
    Double maxSalary = row.getDouble("salary_max");
}
```

### Execution Order

Transformations execute in this order regardless of method chaining:
1. Filters
2. Computed columns (withColumn)
3. Select (column projection)
4. OrderBy
5. Distinct
6. Limit
7. GroupBy with aggregations

### Execute

```java
public DataSet execute()
```

Call `execute()` to apply all transformations and return new DataSet.

---

## DataOperations

Static utility methods for complex operations.

### Joins

#### Inner Join
```java
public static DataSet innerJoin(
    DataSet left,
    DataSet right,
    String leftKey,
    String rightKey
)
```

Returns rows where key exists in both datasets.

**Example:**
```java
DataSet result = DataOperations.innerJoin(
    employees, departments,
    "departmentId", "departmentId"
);
```

#### Left Join
```java
public static DataSet leftJoin(
    DataSet left,
    DataSet right,
    String leftKey,
    String rightKey
)
```

Returns all rows from left, matching rows from right (null if no match).

**Example:**
```java
DataSet result = DataOperations.leftJoin(
    employees, sales,
    "employeeId", "employeeId"
);
```

#### Right Join
```java
public static DataSet rightJoin(
    DataSet left,
    DataSet right,
    String leftKey,
    String rightKey
)
```

Returns all rows from right, matching rows from left (null if no match).

#### Full Outer Join
```java
public static DataSet fullOuterJoin(
    DataSet left,
    DataSet right,
    String leftKey,
    String rightKey
)
```

Returns all rows from both datasets, with nulls where no match.

#### Cross Join
```java
public static DataSet crossJoin(DataSet left, DataSet right)
```

Cartesian product - every row from left paired with every row from right.

**Warning:** Can produce very large results (left.count() × right.count()).

### Unions

```java
// Union (includes duplicates)
public static DataSet union(DataSet first, DataSet second)

// Union distinct (removes duplicates)
public static DataSet unionDistinct(DataSet first, DataSet second)
```

**Example:**
```java
DataSet all = DataOperations.union(dataset1, dataset2);
DataSet unique = DataOperations.unionDistinct(dataset1, dataset2);
```

### Pivot

```java
public static List<DataRow> pivot(
    DataSet dataSet,
    String groupByColumn,
    String pivotColumn,
    String valueColumn
)
```

Transform rows to columns.

**Example:**
```java
// Input:
// product  | month | amount
// Widget   | Jan   | 100
// Widget   | Feb   | 150
// Gadget   | Jan   | 200

List<DataRow> pivoted = DataOperations.pivot(
    monthlySales,
    "product",  // Row grouping
    "month",    // Becomes columns
    "amount"    // Cell values
);

// Output:
// product | Jan | Feb
// Widget  | 100 | 150
// Gadget  | 200 | null
```

### Running Totals

```java
public static DataSet withRunningTotal(
    DataSet dataSet,
    String valueColumn,
    String runningTotalColumn
)
```

**Example:**
```java
DataSet withTotals = DataOperations.withRunningTotal(
    dailySales,
    "revenue",
    "runningTotal"
);
```

---

## Exceptions

### MetadataNotFoundException

Thrown when procedure metadata cannot be found.

```java
public class MetadataNotFoundException extends MetadataException {
    public String getProcedureId()
}
```

### MissingParameterException

Thrown when required parameter is missing or null.

```java
public class MissingParameterException extends RuntimeException {
    public String getParameterName()
    public String getProcedureId()
}
```

### MetadataParseException

Thrown when metadata JSON cannot be parsed.

```java
public class MetadataParseException extends MetadataException
```

### ReportExecutionException

Thrown when stored procedure execution fails.

```java
public class ReportExecutionException extends RuntimeException
```

---

## Complete Example

Putting it all together:

```java
public class SalesAnalysisReport {

    public static void main(String[] args) {
        // Initialize
        ConnectionProvider provider = DatabaseConfig.createConnectionProvider();
        ReportService service = new ReportService(provider);

        try {
            // Execute multiple procedures
            DataSet employees = service.execute("emp_001",
                Map.of("departmentId", 10));

            DataSet departments = service.execute("dept_001");

            DataSet sales = service.execute("sales_001",
                Map.of("startDate", LocalDate.of(2024, 1, 1)));

            // Join data
            DataSet empWithDept = DataOperations.innerJoin(
                employees, departments,
                "departmentId", "departmentId"
            );

            // Aggregate sales
            DataSet salesByEmp = DataQuery.from(sales)
                .groupBy("employeeId")
                    .sum("saleAmount")
                    .count("saleId")
                .execute();

            // Join with sales
            DataSet fullData = DataOperations.leftJoin(
                empWithDept, salesByEmp,
                "employeeId", "employeeId"
            );

            // Transform
            DataSet result = DataQuery.from(fullData)
                .withColumn("fullName", row ->
                    row.getString("firstName") + " " + row.getString("lastName"))

                .withColumn("totalSales", row -> {
                    BigDecimal sum = row.getBigDecimal("saleAmount_sum");
                    return sum != null ? sum : BigDecimal.ZERO;
                })

                .filter(row -> row.getBigDecimal("salary")
                    .compareTo(BigDecimal.valueOf(80000)) >= 0)

                .select("fullName", "departmentName", "salary", "totalSales")
                .orderBy("totalSales").desc()
                .limit(20)
                .execute();

            // Export
            String json = result.toPrettyJSON();
            System.out.println(json);

        } catch (MetadataNotFoundException e) {
            System.err.println("Procedure not found: " + e.getProcedureId());

        } catch (MissingParameterException e) {
            System.err.println("Missing parameter: " + e.getParameterName());

        } catch (ReportExecutionException e) {
            System.err.println("Execution failed: " + e.getMessage());
        }
    }
}
```

---

## Type Mapping

SQL Type | Java Type | DataRow Getter
---------|-----------|---------------
INTEGER, INT | Integer | getInt()
BIGINT | Long | getLong()
DECIMAL, NUMERIC | BigDecimal | getBigDecimal()
FLOAT, REAL | Float | getDouble()
DOUBLE | Double | getDouble()
VARCHAR, NVARCHAR | String | getString()
DATE | LocalDate | getLocalDate()
DATETIME, TIMESTAMP | LocalDateTime | getLocalDateTime()
TIME | LocalTime | getLocalTime()
BIT, BOOLEAN | Boolean | getBoolean()

---

**For more examples, see [Getting Started Guide](GETTING_STARTED.md)**
