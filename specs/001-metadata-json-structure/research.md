# Research: Report Metadata JSON Structure

**Feature**: Report Metadata JSON Structure
**Date**: 2026-07-07
**Status**: Complete

## Overview

Research findings for replacing the existing metadata implementation with a comprehensive JSON structure supporting multiple datasets, column metadata, and JSON Schema validation.

## Research Areas

### 1. Jackson Immutable DTO Pattern

**Question**: How to create immutable DTOs with Jackson that avoid manual casting and follow Constitution Principle II & III?

**Decision**: Use `@JsonCreator` constructor injection with all-args constructor and final fields

**Rationale**:
- **Immutability**: Final fields enforce immutability at compile time
- **Type Safety**: Jackson provides typed deserialization - no manual casts needed
- **Standard Pattern**: Widely used in Java ecosystems, good IDE support
- **Performance**: Constructor injection is faster than reflection-based setters

**Implementation Pattern**:
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportMetadata {
    private final String reportId;
    private final String reportName;
    private final String reportDescription;
    private final List<Dataset> datasets;

    @JsonCreator
    public ReportMetadata(
        @JsonProperty("reportId") String reportId,
        @JsonProperty("reportName") String reportName,
        @JsonProperty("reportDescription") String reportDescription,
        @JsonProperty("datasets") List<Dataset> datasets
    ) {
        this.reportId = Objects.requireNonNull(reportId, "reportId cannot be null");
        this.reportName = Objects.requireNonNull(reportName, "reportName cannot be null");
        this.reportDescription = reportDescription; // nullable
        this.datasets = Collections.unmodifiableList(
            datasets != null ? new ArrayList<>(datasets) : Collections.emptyList()
        );
    }

    // Getters only, no setters
    public String getReportId() { return reportId; }
    public String getReportName() { return reportName; }
    public String getReportDescription() { return reportDescription; }
    public List<Dataset> getDatasets() { return datasets; }
}
```

**Alternatives Considered**:
- **Lombok @Value**: Rejected - adds external dependency, team preference for explicit code
- **Record classes (Java 14+)**: Rejected - want familiar patterns, records less flexible for validation
- **Builder pattern**: Keep for tests only, not for production deserialization

---

### 2. JSON Schema Generation Strategy

**Question**: Should JSON Schema be hand-written or generated from Java classes?

**Decision**: Hand-write JSON Schema as source of truth, validate Java classes against it

**Rationale**:
- **Contract-First**: Schema defines the contract, Java implements it
- **Stability**: Schema changes require explicit decisions, not accidental from code refactoring
- **Documentation**: Schema serves as formal API documentation
- **Tooling**: Hand-written schemas work better with validation tools and code generators

**Implementation**:
- Store schema at `src/main/resources/schema/metadata-schema.json`
- Use JSON Schema Draft 7 specification
- Include in build process (validate sample JSON files against schema)
- Make schema accessible via classpath for runtime validation

**Alternatives Considered**:
- **Generate from annotations**: Rejected - requires additional library, less control over output
- **Both directions**: Rejected - creates circular dependency and version conflicts

---

### 3. Database Schema Design

**Question**: How to structure the REPORT_METADATA table for the new JSON format?

**Decision**: Simple schema with reportId PK and JSON blob, drop old columns

**New Schema**:
```sql
CREATE TABLE REPORT_METADATA (
    REPORT_ID VARCHAR(100) PRIMARY KEY,
    REPORT_NAME VARCHAR(255) NOT NULL,
    REPORT_DESCRIPTION VARCHAR(1000),
    METADATA_JSON NVARCHAR(MAX) NOT NULL,
    CREATED_DATE DATETIME2 DEFAULT GETDATE(),
    MODIFIED_DATE DATETIME2 DEFAULT GETDATE()
);

CREATE INDEX IX_REPORT_METADATA_NAME ON REPORT_METADATA(REPORT_NAME);
```

**Rationale**:
- **reportId as PK**: Better than reportName (allows name changes without breaking references)
- **Denormalized Name/Description**: Quick catalog queries without parsing JSON
- **JSON Blob**: Full flexibility, avoids complex relational mapping
- **Audit Columns**: Track when metadata created/modified

**Old Schema** (to be dropped):
```sql
-- OLD - will be deleted
CREATE TABLE REPORT_METADATA (
    REPORT_NAME VARCHAR(255) PRIMARY KEY,
    STORED_PROCEDURE VARCHAR(500),
    RESULT_CLASS VARCHAR(500),
    METADATA_JSON NVARCHAR(MAX)
);
```

**Alternatives Considered**:
- **Fully normalized**: Rejected - complex joins, harder to maintain, no real benefit for this use case
- **Keep old columns**: Rejected - creates confusion, no backward compat needed

---

### 4. Parameter Direction Enumeration

**Question**: How to represent parameterDirection ("Input"/"Output") in Java?

**Decision**: Use enum with JSON serialization to string values

**Implementation**:
```java
public enum ParameterDirection {
    @JsonProperty("Input")
    INPUT,

    @JsonProperty("Output")
    OUTPUT;

    @JsonCreator
    public static ParameterDirection fromString(String value) {
        if ("Input".equals(value)) return INPUT;
        if ("Output".equals(value)) return OUTPUT;
        throw new IllegalArgumentException("Invalid parameter direction: " + value);
    }

    @JsonValue
    public String toJson() {
        return this == INPUT ? "Input" : "Output";
    }
}
```

**Rationale**:
- **Type Safety**: Enum prevents invalid values at compile time
- **Clear Serialization**: Explicit @JsonProperty mapping to string values
- **Validation**: fromString rejects invalid values early

**Alternatives Considered**:
- **Plain strings**: Rejected - no compile-time safety, error-prone
- **Boolean isInput**: Rejected - less clear than enum, harder to extend (what about INOUT future?)

---

### 5. SQL Type to Java Type Mapping

**Question**: How to handle SQL type strings in the metadata?

**Decision**: Store as strings in JSON, provide utility class for type conversion

**Type Mapping Table**:
| SQL Type | Java Type | JDBC Type |
|----------|-----------|-----------|
| INTEGER | java.lang.Integer | Types.INTEGER |
| BIGINT | java.lang.Long | Types.BIGINT |
| VARCHAR | java.lang.String | Types.VARCHAR |
| NVARCHAR | java.lang.String | Types.NVARCHAR |
| DECIMAL | java.math.BigDecimal | Types.DECIMAL |
| NUMERIC | java.math.BigDecimal | Types.NUMERIC |
| DATE | java.time.LocalDate | Types.DATE |
| DATETIME | java.time.LocalDateTime | Types.TIMESTAMP |
| DATETIME2 | java.time.LocalDateTime | Types.TIMESTAMP |
| BIT | java.lang.Boolean | Types.BIT |

**Utility Class**:
```java
public class SqlTypeMapper {
    private static final Map<String, Class<?>> SQL_TO_JAVA = Map.ofEntries(
        entry("INTEGER", Integer.class),
        entry("BIGINT", Long.class),
        entry("VARCHAR", String.class),
        entry("NVARCHAR", String.class),
        entry("DECIMAL", BigDecimal.class),
        entry("NUMERIC", BigDecimal.class),
        entry("DATE", LocalDate.class),
        entry("DATETIME", LocalDateTime.class),
        entry("DATETIME2", LocalDateTime.class),
        entry("BIT", Boolean.class)
    );

    public static Class<?> toJavaClass(String sqlType) {
        Class<?> javaClass = SQL_TO_JAVA.get(sqlType.toUpperCase());
        if (javaClass == null) {
            throw new IllegalArgumentException("Unsupported SQL type: " + sqlType);
        }
        return javaClass;
    }
}
```

**Rationale**:
- **Flexibility**: String storage allows easy addition of new types
- **Validation**: Utility provides central validation point
- **Clear Mapping**: Explicit table documents supported types

---

### 6. JSON Schema Validation Strategy

**Question**: When and how to validate metadata against JSON Schema?

**Decision**: Validate at load time (startup/cache refresh), fail fast on invalid metadata

**Validation Points**:
1. **Load Time**: When MetadataLoader reads from database
2. **Test Time**: Unit tests validate all sample JSON files
3. **Optional Runtime**: Configurable validation for development/debugging

**Implementation**:
```java
public class JsonSchemaValidator {
    private final JsonSchema schema;

    public JsonSchemaValidator() {
        // Load schema from classpath
        this.schema = loadSchema("/schema/metadata-schema.json");
    }

    public void validate(String json) throws MetadataValidationException {
        Set<ValidationMessage> errors = schema.validate(json);
        if (!errors.isEmpty()) {
            throw new MetadataValidationException(
                "Metadata validation failed: " + errors
            );
        }
    }
}
```

**Library Choice**: Use `networknt/json-schema-validator` (compatible with Jackson)

**Rationale**:
- **Fail Fast**: Catch errors at load time, not runtime
- **Developer Feedback**: Clear error messages with schema path
- **Performance**: One-time validation at load, cached metadata thereafter

**Alternatives Considered**:
- **No validation**: Rejected - defeats purpose of JSON Schema
- **Always validate**: Rejected - unnecessary performance overhead for cached metadata

---

### 7. Catalog Retrieval Optimization

**Question**: How to efficiently retrieve catalog of 100+ reports?

**Decision**: Load all metadata at startup into in-memory cache, single SELECT query

**Implementation Strategy**:
```java
public class MetadataRepository {
    private final Map<String, ReportMetadata> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadCache() {
        String sql = "SELECT REPORT_ID, METADATA_JSON FROM REPORT_METADATA";
        // Execute single query, parse all JSON, populate cache
    }

    public ReportMetadata getById(String reportId) {
        ReportMetadata metadata = cache.get(reportId);
        if (metadata == null) {
            throw new MetadataNotFoundException(reportId);
        }
        return metadata;
    }

    public ReportCatalog getCatalog() {
        return new ReportCatalog(new ArrayList<>(cache.values()));
    }
}
```

**Rationale**:
- **Performance**: Single query vs N queries for 100 reports
- **Simplicity**: In-memory cache eliminates repeated database calls
- **Realistic Scale**: 100-500 reports × 10KB avg = 1-5MB in memory (acceptable)

**Cache Refresh Strategy**:
- On-demand via admin endpoint
- Scheduled (e.g., every hour)
- Event-driven (when metadata updated)

**Alternatives Considered**:
- **Query per report**: Rejected - too slow for catalog
- **Database caching**: Rejected - adds complexity, framework should own caching

---

### 8. CamelCase Naming Transformation

**Question**: How to convert database parameter names and display names to camelCase automatically?

**Decision**: Use Google Guava's CaseFormat utility (already in dependencies) during metadata loading

**Implementation Strategy**:
```java
public class NamingConverter {
    public static String toCamelCase(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Detect input format and convert to camelCase
        if (name.contains("_")) {
            // snake_case or UPPER_SNAKE_CASE → camelCase
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, name.toUpperCase());
        } else if (Character.isUpperCase(name.charAt(0)) && name.length() > 1) {
            // PascalCase → camelCase
            return Character.toLowerCase(name.charAt(0)) + name.substring(1);
        } else {
            // Already camelCase or lowercase
            return name;
        }
    }
}
```

**Conversion Examples**:
| Input (Database) | Output (JSON) | Format Detected |
|------------------|---------------|-----------------|
| Department_Id | departmentId | snake_case |
| EMPLOYEE_NAME | employeeName | UPPER_SNAKE_CASE |
| StartDate | startDate | PascalCase |
| totalSales | totalSales | Already camelCase |
| employee_id | employeeId | snake_case |
| TOTAL_RECORDS | totalRecords | UPPER_SNAKE_CASE |

**Rationale**:
- **Consistency**: All JSON field names follow Java/JavaScript camelCase convention
- **Existing Dependency**: Google Guava already in pom.xml - no new dependencies
- **Standard Library**: CaseFormat is well-tested and handles edge cases
- **Predictable**: Clear rules for conversion prevent surprises

**Application Points**:
- **parameterName**: Convert when deserializing Parameter from database JSON
- **displayName**: Convert when deserializing Column from database JSON
- **sourceColumn**: NO conversion - preserve exact database column name

**Why preserve sourceColumn?**: The sourceColumn is used to reference the actual database column in queries - changing it would break SQL generation.

**Edge Cases Handled**:
- Numbers in names: "PARAM_1_VALUE" → "param1Value"
- Single character: "X" → "x"
- Already camelCase: "employeeId" → "employeeId" (no change)
- Empty/null: Returns as-is

**Alternatives Considered**:
- **Manual mapping**: Rejected - too brittle, hard to maintain
- **Custom regex**: Rejected - Guava CaseFormat more robust
- **Apache Commons**: Rejected - would add new dependency

---

### 9. H2 Testing Strategy

**Question**: How to test metadata loading given H2 stored procedure limitations?

**Decision**: Use direct SQL queries in tests, mock stored procedure execution layer

**Test Approach**:
```java
@Test
public void testMetadataLoading() {
    // Insert test metadata directly into H2
    String json = readResource("sample-metadata/single-report.json");
    jdbcTemplate.update(
        "INSERT INTO REPORT_METADATA (REPORT_ID, REPORT_NAME, METADATA_JSON) VALUES (?, ?, ?)",
        "test-001", "Test Report", json
    );

    // Test metadata loading
    ReportMetadata metadata = metadataLoader.load("test-001");

    // Assertions
    assertThat(metadata.getReportId()).isEqualTo("test-001");
    assertThat(metadata.getDatasets()).hasSize(1);
}
```

**Rationale**:
- **Focus**: Test metadata parsing, not stored procedure execution
- **Isolation**: Metadata concerns separate from execution concerns
- **Documented**: CLAUDE.md already documents H2 limitation

---

## Implementation Recommendations

### Phase 1 Priorities

1. **Define DTOs**: ReportMetadata, Dataset, Parameter, Column (immutable with Jackson)
2. **Create JSON Schema**: Formal contract in `metadata-schema.json`
3. **Implement MetadataLoader**: Parse JSON, validate against schema
4. **Database Migration**: DROP old table, CREATE new schema
5. **Update Tests**: New metadata format in test data

### Non-Goals (Explicitly Deferred)

- Migration from old format (not needed - clean replacement)
- Multiple datasource types (only "StoredProc" for now, extensible later)
- Catalog pagination (not needed for 100-500 reports scale)
- Metadata versioning (no schema version field - handle with new table if needed)

### Testing Strategy

1. **Unit Tests**: JSON serialization, deserialization, validation
2. **Integration Tests**: MetadataLoader with H2 database
3. **Contract Tests**: Sample JSON files validate against schema
4. **Performance Tests**: Catalog loading <1s for 100 reports

---

## Open Questions

None - all research questions resolved.

## References

- Jackson annotations: https://github.com/FasterXML/jackson-annotations
- JSON Schema Draft 7: https://json-schema.org/draft-07/schema
- SQL Server JDBC types: https://docs.microsoft.com/en-us/sql/connect/jdbc/understanding-the-jdbc-driver-data-types
- Constitution v1.0.0: `.specify/memory/constitution.md`
