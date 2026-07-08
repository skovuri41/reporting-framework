# Implementation Plan: Report Metadata JSON Structure

**Branch**: `001-metadata-json-structure` | **Date**: 2026-07-07 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-metadata-json-structure/spec.md`

**Note**: This is a **clean replacement** of the existing metadata implementation - no backward compatibility required.

## Summary

Replace the existing simple metadata structure with a comprehensive JSON structure that supports multiple datasets per report, column-level metadata for UI generation, and formal JSON Schema validation. The new structure enables:
- Multiple data sources (datasets) per report
- Rich parameter metadata with direction (Input/Output)
- Column metadata with UI capabilities (sortable, groupable, filterable)
- Formal JSON Schema for validation and code generation

**Approach**: Remove old Metadata class files and replace MetadataLoader implementation. Keep core framework (DataSet, DataQuery, StoredProcedureExecutor) unchanged.

## Technical Context

**Language/Version**: Java 17

**Primary Dependencies**:
- Jackson 2.17.2 (JSON serialization/deserialization)
- SLF4J 2.0.13 + Logback 1.5.6 (logging)
- SQL Server JDBC 12.8.1 (database connectivity)
- Google Guava 33.0.0 (utilities)

**Storage**: SQL Server (production), H2 1.4.200 (testing) - New REPORT_METADATA table schema

**Testing**: JUnit Jupiter 5.10.3, AssertJ 3.26.0, Mockito 5.12.0, H2 in-memory database

**Target Platform**: JVM-based applications (Java 17+ runtime)

**Project Type**: Java library/framework (consumed by Java applications)

**Performance Goals**:
- Metadata retrieval <100ms for single-dataset reports
- Catalog retrieval <1 second for 100 reports
- Support 10 datasets per report without degradation
- JSON parsing overhead <50ms

**Constraints**:
- Must use Jackson (existing dependency) - no new JSON libraries
- Must maintain existing DataSet/DataQuery API (only metadata changes)
- H2 testing limitation (stored procedures not fully supported - use direct SQL)
- JSON Schema must be Draft 7 compatible

**Scale/Scope**:
- Support 100-500 reports in catalog
- Up to 10 datasets per report
- Up to 20 parameters per dataset
- Up to 50 columns per dataset

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Based on the Reporting Framework Constitution (v1.0.0), verify:

- [x] **Metadata-Driven**: Are report definitions configured via database metadata, not hardcoded?
  - ✅ YES - All metadata stored in REPORT_METADATA table as JSON
- [x] **Type Safety**: Does the design provide typed accessors without requiring manual casting?
  - ✅ YES - Jackson DTOs with typed getters (getReportId(), getDatasets(), etc.)
- [x] **Immutability**: Do transformations return new instances rather than modifying in place?
  - ✅ YES - All metadata DTOs are immutable (final fields, no setters)
- [x] **Fluent DSL**: Are complex operations expressible through chainable APIs?
  - ⚠️ N/A - This is metadata structure definition, not transformation operations
- [x] **Test-First**: Are integration tests planned before implementation (FullWorkflowIntegrationTest pattern)?
  - ✅ YES - Integration tests for metadata loading, parsing, and JSON Schema validation
- [x] **Transparent Limitations**: Are known constraints documented with workarounds in CLAUDE.md?
  - ✅ YES - H2 stored procedure limitation documented; JSON Schema validation requirements clear
- [x] **Simplicity**: Does the design avoid over-engineering and unnecessary abstraction (YAGNI)?
  - ✅ YES - Clean replacement, no migration code, straightforward Jackson DTOs

**Violations Requiring Justification**: None - all gates pass or N/A

## Project Structure

### Documentation (this feature)

```text
specs/001-metadata-json-structure/
├── plan.md              # This file
├── research.md          # Phase 0: JSON Schema design, Jackson patterns, SQL type mapping
├── data-model.md        # Phase 1: ReportMetadata, Dataset, Parameter, Column entities
├── quickstart.md        # Phase 1: Validation scenarios and test execution guide
├── contracts/           # Phase 1: JSON Schema definitions and examples
│   ├── metadata-schema.json
│   ├── report-example.json
│   └── catalog-example.json
└── tasks.md             # Phase 2: (Created by /speckit-tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/reporting/framework/
├── metadata/
│   ├── model/                           # NEW package for DTOs
│   │   ├── ReportMetadata.java         # NEW - Top-level report metadata DTO
│   │   ├── Dataset.java                # NEW - Dataset DTO with parameters & columns
│   │   ├── Parameter.java              # NEW - Parameter definition DTO
│   │   ├── Column.java                 # NEW - Column metadata DTO
│   │   └── ReportCatalog.java          # NEW - Catalog wrapper DTO
│   ├── MetadataLoader.java             # REPLACE - Rewrite for new JSON structure
│   ├── MetadataRepository.java         # NEW - Database access layer
│   └── JsonSchemaValidator.java        # NEW - JSON Schema validation
├── api/
│   └── ReportService.java              # UPDATE - Use new metadata model
├── executor/
│   └── StoredProcedureExecutor.java    # MINOR UPDATE - Support dataset concept
├── exception/
│   ├── MetadataNotFoundException.java  # NEW - For invalid reportId
│   └── MetadataValidationException.java # NEW - For schema validation failures
└── (other packages unchanged)

src/test/java/com/reporting/framework/
├── metadata/
│   ├── MetadataLoaderTest.java         # NEW - Unit tests for metadata loading
│   ├── MetadataJsonTest.java           # NEW - JSON serialization tests
│   ├── JsonSchemaValidationTest.java   # NEW - Schema validation tests
│   └── MetadataIntegrationTest.java    # NEW - End-to-end integration test
└── (update existing integration tests for new metadata format)

src/main/resources/
└── schema/
    └── metadata-schema.json            # NEW - JSON Schema definition

src/test/resources/
├── test-schema.sql                     # UPDATE - New REPORT_METADATA table
└── sample-metadata/                    # NEW - Test JSON samples
    ├── single-report.json
    ├── multi-dataset-report.json
    └── catalog.json
```

**Structure Decision**: Single project structure (existing framework). New metadata DTOs added to `metadata.model` package. Old metadata classes removed. Core framework packages (data, executor, mapper, connection) remain unchanged - only metadata layer is replaced.

**Files to DELETE**:
- Any old metadata DTO classes (if they exist in current codebase)
- Old metadata parsing utilities

**Files to UPDATE**:
- `MetadataLoader.java` - Complete rewrite
- `ReportService.java` - Use new metadata model
- `StoredProcedureExecutor.java` - Minor updates for dataset concept
- Test files - Update to new metadata format

**Files to KEEP UNCHANGED**:
- DataSet, DataRow, DataQuery, DataOperations - Core framework stays the same
- ResultSetToMapConverter.java
- ConnectionProvider implementations
- All mapper and connection packages

## Complexity Tracking

> **No violations** - Constitution gates all pass. No complexity justification needed.
