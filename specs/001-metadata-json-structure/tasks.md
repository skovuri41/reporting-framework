# Tasks: Report Metadata JSON Structure

**Input**: Design documents from `/specs/001-metadata-json-structure/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Included - following test-first approach per Constitution Principle V

**Organization**: Tasks grouped by user story for independent implementation and testing. This is a **clean replacement** - no backward compatibility needed.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)
- Include exact file paths in descriptions

## Path Conventions

Single Java project structure:
- `src/main/java/com/reporting/framework/` - Production code
- `src/test/java/com/reporting/framework/` - Test code
- `src/main/resources/` - Resources (schema, SQL)
- `src/test/resources/` - Test resources

---

## Phase 1: Setup & Database Migration

**Purpose**: Initialize project structure and prepare database for new metadata format

- [ ] T001 Review and update pom.xml - verify Jackson 2.17.2, Guava 33.0.0, JUnit 5.10.3 dependencies
- [ ] T002 Create database migration script in src/main/resources/db/migration/drop-old-create-new-metadata-table.sql
- [ ] T003 Update H2 test schema in src/test/resources/test-schema.sql with new REPORT_METADATA table structure
- [ ] T004 Create JSON Schema file at src/main/resources/schema/metadata-schema.json (copy from contracts/metadata-schema.json)
- [ ] T005 [P] Create sample JSON test files directory at src/test/resources/sample-metadata/
- [ ] T006 [P] Copy contract examples to src/test/resources/sample-metadata/ (single-report.json, multi-dataset-report.json, catalog.json)

**Checkpoint**: Database schema ready, JSON Schema in place, sample files available

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core DTOs and utilities that ALL user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Core DTOs (Immutable with Jackson)

- [ ] T007 [P] Create ParameterDirection enum in src/main/java/com/reporting/framework/metadata/model/ParameterDirection.java
- [ ] T008 [P] Create Parameter DTO in src/main/java/com/reporting/framework/metadata/model/Parameter.java
- [ ] T009 [P] Create Column DTO in src/main/java/com/reporting/framework/metadata/model/Column.java
- [ ] T010 Create Dataset DTO in src/main/java/com/reporting/framework/metadata/model/Dataset.java (depends on T008, T009)
- [ ] T011 Create ReportMetadata DTO in src/main/java/com/reporting/framework/metadata/model/ReportMetadata.java (depends on T010)
- [ ] T012 Create ReportCatalog DTO in src/main/java/com/reporting/framework/metadata/model/ReportCatalog.java (depends on T011)

### Utilities & Support

- [ ] T013 [P] Create NamingConverter utility in src/main/java/com/reporting/framework/metadata/util/NamingConverter.java
- [ ] T014 [P] Create SqlTypeMapper utility in src/main/java/com/reporting/framework/metadata/util/SqlTypeMapper.java
- [ ] T015 [P] Create MetadataNotFoundException in src/main/java/com/reporting/framework/exception/MetadataNotFoundException.java
- [ ] T016 [P] Create MetadataValidationException in src/main/java/com/reporting/framework/exception/MetadataValidationException.java

### Core Services

- [ ] T017 Create JsonSchemaValidator in src/main/java/com/reporting/framework/metadata/JsonSchemaValidator.java (depends on T004)
- [ ] T018 Create MetadataRepository in src/main/java/com/reporting/framework/metadata/MetadataRepository.java (depends on T011, T012, T017)
- [ ] T019 Rewrite MetadataLoader in src/main/java/com/reporting/framework/metadata/MetadataLoader.java (depends on T018, T013)

### Cleanup

- [ ] T020 Delete old metadata DTO classes from src/main/java/com/reporting/framework/metadata/ (if they exist)
- [ ] T021 Remove old metadata parsing utilities (if they exist)

**Checkpoint**: Foundation complete - DTOs immutable, utilities ready, core services implemented

---

## Phase 3: User Story 1 - Retrieve Single Report Metadata (Priority: P1) 🎯 MVP

**Goal**: Load and return complete metadata for a single report by reportId, including all datasets, parameters, and columns with camelCase naming transformation

**Independent Test**: Request metadata for known reportId, validate JSON structure, verify all fields present and naming transformed

### Tests for User Story 1 (Test-First Approach) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T022 [P] [US1] Create MetadataJsonTest in src/test/java/com/reporting/framework/metadata/MetadataJsonTest.java - test DTO serialization/deserialization
- [ ] T023 [P] [US1] Create NamingConverterTest in src/test/java/com/reporting/framework/metadata/util/NamingConverterTest.java - test camelCase conversion
- [ ] T024 [P] [US1] Create JsonSchemaValidationTest in src/test/java/com/reporting/framework/metadata/JsonSchemaValidationTest.java - test schema validation
- [ ] T025 [US1] Create MetadataLoaderTest in src/test/java/com/reporting/framework/metadata/MetadataLoaderTest.java - unit tests for loading single report
- [ ] T026 [US1] Create MetadataIntegrationTest in src/test/java/com/reporting/framework/metadata/MetadataIntegrationTest.java - integration test with H2 database

### Implementation for User Story 1

- [ ] T027 [US1] Insert test metadata into H2 test schema (update test-schema.sql with INSERT statements)
- [ ] T028 [US1] Implement single report loading in MetadataLoader.load(reportId) method
- [ ] T029 [US1] Implement JSON Schema validation in load() method (validate before deserialization)
- [ ] T030 [US1] Implement camelCase transformation for parameterName during deserialization
- [ ] T031 [US1] Implement camelCase transformation for column displayName during deserialization
- [ ] T032 [US1] Ensure sourceColumn is preserved exactly (no transformation)
- [ ] T033 [US1] Add error handling for MetadataNotFoundException when reportId not found
- [ ] T034 [US1] Add error handling for MetadataValidationException when JSON invalid
- [ ] T035 [US1] Update ReportService in src/main/java/com/reporting/framework/api/ReportService.java to use new MetadataLoader
- [ ] T036 [US1] Verify all US1 tests pass (run MetadataIntegrationTest)

**Checkpoint**: Single report metadata retrieval fully functional - tests pass, naming transformation works, error handling robust

---

## Phase 4: User Story 2 - Retrieve All Reports Catalog (Priority: P2)

**Goal**: Return complete catalog of all available reports with consistent metadata structure for each

**Independent Test**: Request catalog without parameters, validate all reports returned, verify count field matches

### Tests for User Story 2 (Test-First Approach) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T037 [P] [US2] Add catalog retrieval test to MetadataIntegrationTest.java - test getCatalog() method
- [ ] T038 [P] [US2] Add catalog performance test in src/test/java/com/reporting/framework/metadata/MetadataPerformanceTest.java - verify <1s for 100 reports

### Implementation for User Story 2

- [ ] T039 [US2] Insert additional test reports into H2 test schema (at least 3 total reports for catalog testing)
- [ ] T040 [US2] Implement catalog loading in MetadataRepository.getCatalog() method
- [ ] T041 [US2] Implement in-memory caching in MetadataRepository (load all metadata at startup)
- [ ] T042 [US2] Implement cache refresh mechanism (manual/scheduled)
- [ ] T043 [US2] Ensure catalog count field matches reports.size()
- [ ] T044 [US2] Add cache warming on repository initialization (@PostConstruct)
- [ ] T045 [US2] Update ReportService to expose catalog retrieval method (if needed)
- [ ] T046 [US2] Verify all US2 tests pass (run MetadataIntegrationTest and MetadataPerformanceTest)

**Checkpoint**: Catalog retrieval functional - all reports returned, count accurate, performance meets <1s target

---

## Phase 5: Integration & End-to-End Validation

**Purpose**: Validate complete system integration and update existing framework components

- [ ] T047 Update StoredProcedureExecutor in src/main/java/com/reporting/framework/executor/StoredProcedureExecutor.java for dataset concept (minor changes)
- [ ] T048 Update existing FullWorkflowIntegrationTest in src/test/java/com/reporting/framework/integration/FullWorkflowIntegrationTest.java to use new metadata format
- [ ] T049 Create comprehensive end-to-end test scenario validating full flow: database → load → transform names → cache → retrieve
- [ ] T050 Run full test suite: mvn test (expect all tests to pass)
- [ ] T051 Verify JSON Schema validation works for all sample files (run validation tests)
- [ ] T052 Verify naming transformation for all edge cases (snake_case, UPPER_CASE, PascalCase, numbers)
- [ ] T053 [P] Performance validation: Single report <100ms, catalog <1s
- [ ] T054 [P] Verify immutability: Attempt to modify returned metadata fails (defensive copying works)

**Checkpoint**: Full integration verified - all tests pass, performance targets met

---

## Phase 6: Documentation & Cleanup

**Purpose**: Update documentation and clean up deprecated code

- [ ] T055 [P] Update CLAUDE.md with new metadata structure, examples, and known limitations
- [ ] T056 [P] Document naming transformation behavior in CLAUDE.md (snake_case → camelCase examples)
- [ ] T057 [P] Document new JSON Schema location and validation approach in CLAUDE.md
- [ ] T058 [P] Add metadata loading examples to CLAUDE.md (single report + catalog)
- [ ] T059 Verify no old metadata classes remain in codebase (run `find` command for old class names)
- [ ] T060 Clean up unused imports in updated files (ReportService, StoredProcedureExecutor)
- [ ] T061 Verify database migration script is ready for deployment (review DROP/CREATE SQL)
- [ ] T062 Create sample INSERT scripts for initial report metadata (optional, for deployment)

**Checkpoint**: Documentation complete, codebase clean, ready for deployment

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational - can start after T021
- **User Story 2 (Phase 4)**: Depends on Foundational - can start after T021 (independent of US1)
- **Integration (Phase 5)**: Depends on US1 and US2 completion
- **Documentation (Phase 6)**: Can run in parallel with Phase 5

### User Story Dependencies

- **User Story 1 (P1)**: Independent - only needs Foundation
- **User Story 2 (P2)**: Independent - only needs Foundation (can parallelize with US1 if resources available)

### Within Each User Story

**US1 Execution Order**:
1. Tests FIRST (T022-T026) - all written and failing
2. Implementation (T027-T035) - make tests pass
3. Verification (T036) - all tests green

**US2 Execution Order**:
1. Tests FIRST (T037-T038) - all written and failing
2. Implementation (T039-T045) - make tests pass
3. Verification (T046) - all tests green

### Parallel Opportunities

**Setup Phase (can all run in parallel)**:
- T005 (sample files dir) + T006 (copy samples) can run together
- T001, T004 independent

**Foundational Phase**:
- T007-T009 (DTOs) all parallel - different files
- T013-T016 (utilities/exceptions) all parallel - different files
- T010 AFTER T008, T009
- T011 AFTER T010
- T012 AFTER T011

**User Story 1 Tests**:
- T022-T024 all parallel (different test files)

**User Story 2 Implementation**:
- Can run entirely in parallel with US1 if team has resources

**Documentation Phase**:
- T055-T058 all parallel (different doc sections)

---

## Parallel Example: Foundational Phase

Launch all these tasks together after Setup complete:
```bash
# Core DTOs (different files, no conflicts)
Task: "Create ParameterDirection enum" (T007)
Task: "Create Parameter DTO" (T008)
Task: "Create Column DTO" (T009)

# Utilities (different files, no conflicts)
Task: "Create NamingConverter utility" (T013)
Task: "Create SqlTypeMapper utility" (T014)
Task: "Create MetadataNotFoundException" (T015)
Task: "Create MetadataValidationException" (T016)
```

Then run sequentially:
```bash
Task: "Create Dataset DTO" (T010) - needs T008, T009
Task: "Create ReportMetadata DTO" (T011) - needs T010
Task: "Create ReportCatalog DTO" (T012) - needs T011
Task: "Create JsonSchemaValidator" (T017) - needs T004
Task: "Create MetadataRepository" (T018) - needs T011, T012, T017
Task: "Rewrite MetadataLoader" (T019) - needs T018, T013
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

**Fastest path to working system**:
1. Complete Phase 1: Setup (T001-T006)
2. Complete Phase 2: Foundational (T007-T021)
3. Complete Phase 3: User Story 1 (T022-T036)
4. **STOP and VALIDATE**: Run `mvn test -Dtest=MetadataIntegrationTest`
5. Single report metadata retrieval is now working - can demo/deploy

**Deliverable**: Working metadata loading for single reports with camelCase transformation

---

### Incremental Delivery (Full Feature)

**Complete implementation**:
1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Working MVP
3. Add User Story 2 → Test independently → Catalog functionality added
4. Run Integration tests → Full system validated
5. Add Documentation → Production ready

**Each phase adds value without breaking previous functionality**

---

### Parallel Team Strategy

**With 2 developers**:
1. Both work together on Setup + Foundational (T001-T021)
2. Once Foundational complete:
   - Developer A: User Story 1 (T022-T036)
   - Developer B: User Story 2 (T037-T046)
3. Both converge on Integration (T047-T054)
4. Both work on Documentation (T055-T062)

**Stories complete independently and integrate at the end**

---

## Task Summary

**Total Tasks**: 62

**By Phase**:
- Setup: 6 tasks
- Foundational: 15 tasks (BLOCKING)
- User Story 1: 15 tasks (P1 - MVP)
- User Story 2: 10 tasks (P2)
- Integration: 8 tasks
- Documentation: 8 tasks

**By User Story**:
- US1 (Single Report): 15 tasks
- US2 (Catalog): 10 tasks
- Shared/Foundation: 37 tasks

**Parallel Opportunities**: 18 tasks marked with [P]

**Test Tasks**: 7 test files (all test-first)

**MVP Scope**: Phases 1-3 (36 tasks) = Single report metadata retrieval

---

## Validation Checklist

Before considering feature complete:

- [ ] All 62 tasks completed and checked off
- [ ] All tests passing: `mvn test` shows 100% pass rate
- [ ] JSON Schema validation working for all samples
- [ ] Naming transformation verified: snake_case → camelCase working
- [ ] Performance targets met: <100ms single report, <1s catalog
- [ ] Immutability verified: DTOs cannot be modified after creation
- [ ] Error handling verified: MetadataNotFoundException and MetadataValidationException working
- [ ] Documentation updated: CLAUDE.md reflects new structure
- [ ] Old code removed: No deprecated metadata classes remain
- [ ] Database migration ready: SQL scripts tested

---

## Notes

- **Test-First**: Write tests before implementation (Constitution Principle V)
- **Clean Replacement**: No backward compatibility - delete old code
- **Immutability**: All DTOs use final fields, no setters
- **Naming Transform**: Automatic camelCase conversion for parameterName and displayName
- **Preserve sourceColumn**: No transformation - used for SQL references
- **Commit Strategy**: Commit after each task or logical group (not after each checkbox)
- **Stop at Checkpoints**: Validate story works before moving to next phase
