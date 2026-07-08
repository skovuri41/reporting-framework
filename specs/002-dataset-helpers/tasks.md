# Tasks: Dataset Lookup and Multi-Dataset Execution

**Input**: Design documents from `/specs/002-dataset-helpers/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Included - following test-first approach per Constitution Principle V

**Organization**: Tasks grouped by user story for independent implementation and testing. Straightforward enhancement to existing classes.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2)
- Include exact file paths in descriptions

## Path Conventions

Single Java project structure:
- `src/main/java/com/reporting/framework/` - Production code
- `src/test/java/com/reporting/framework/` - Test code
- Documentation in project root

---

## Phase 1: Setup & Verification

**Purpose**: Verify environment and dependencies before implementation

- [x] T001 Verify Java 17+ is configured in pom.xml (no changes needed - just verification)
- [x] T002 Verify JUnit Jupiter 5.10.3 and AssertJ 3.26.0 are available in test scope
- [x] T003 Run existing test suite to establish baseline: mvn test (expect 104/108 passing)

**Checkpoint**: Environment ready, baseline tests passing

---

## Phase 2: Foundational (No Blocking Prerequisites)

**Purpose**: Verify existing infrastructure that will be reused

**⚠️ NOTE**: This feature has NO foundational prerequisites - it uses 100% existing infrastructure

- [x] T004 Review existing ReportMetadata.java to understand datasets collection structure
- [x] T005 Review existing ReportService.java to understand execute() method implementation
- [x] T006 Review existing StoredProcedureExecutor.java to confirm parameter extraction behavior

**Checkpoint**: Existing infrastructure understood, ready for user story implementation

---

## Phase 3: User Story 1 - Lookup Specific Dataset by ID (Priority: P1) 🎯 MVP

**Goal**: Add getDatasetById() method to ReportMetadata for direct dataset access without iteration

**Independent Test**: Can be fully tested by loading report metadata, calling getDatasetById() with valid and invalid IDs, and verifying correct dataset is returned or exception is thrown

### Tests for User Story 1 (Test-First Approach) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T007 [P] [US1] Create test for getDatasetById() with valid ID in src/test/java/com/reporting/framework/metadata/model/ReportMetadataTest.java
- [x] T008 [P] [US1] Create test for getDatasetById() with invalid ID (exception) in ReportMetadataTest.java
- [x] T009 [P] [US1] Create test for getDatasetById() error message includes available IDs in ReportMetadataTest.java
- [x] T010 [US1] Verify all US1 tests fail initially (red phase)

### Implementation for User Story 1

- [x] T011 [US1] Add private helper method getAvailableDatasetIds() to ReportMetadata in src/main/java/com/reporting/framework/metadata/model/ReportMetadata.java
- [x] T012 [US1] Implement getDatasetById(String datasourceId) method in ReportMetadata.java using Stream API
- [x] T013 [US1] Add javadoc to getDatasetById() method explaining behavior, parameters, return, and exceptions
- [x] T014 [US1] Run US1 tests and verify they pass: mvn test -Dtest=ReportMetadataTest
- [x] T015 [US1] Test edge cases: null datasourceId, empty string, case sensitivity

**Checkpoint**: Dataset lookup fully functional - tests pass, exception messages clear, independent of US2

---

## Phase 4: User Story 2 - Execute All Datasets in Report (Priority: P2)

**Goal**: Add executeAllDatasets() method to ReportService that executes all datasets and returns Map<datasourceId, DataSet>

**Independent Test**: Can be fully tested by creating a report with 3 datasets, providing parameters, calling executeAllDatasets(), and verifying all datasets executed and results returned as Map

### Tests for User Story 2 (Test-First Approach) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [x] T016 [P] [US2] Create unit test for executeAllDatasets() with mocked execution in src/test/java/com/reporting/framework/api/ReportServiceTest.java
- [x] T017 [P] [US2] Create integration test scaffold MultiDatasetIntegrationTest in src/test/java/com/reporting/framework/integration/MultiDatasetIntegrationTest.java
- [x] T018 [US2] Add test scenario: execute 3 datasets successfully in MultiDatasetIntegrationTest.java
- [x] T019 [US2] Add test scenario: fail-fast behavior (stop on second dataset failure) in MultiDatasetIntegrationTest.java
- [x] T020 [US2] Add test scenario: empty datasets list returns empty Map in MultiDatasetIntegrationTest.java
- [x] T021 [US2] Add test scenario: parameter extraction verification in MultiDatasetIntegrationTest.java
- [x] T022 [US2] Verify all US2 tests fail initially (red phase)

### Implementation for User Story 2

- [x] T023 [US2] Implement executeAllDatasets(String reportId, Map<String, Object> parameters) in src/main/java/com/reporting/framework/api/ReportService.java
- [x] T024 [US2] Use LinkedHashMap for results to preserve execution order per FR-010
- [x] T025 [US2] Implement fail-fast error handling with datasourceId context per FR-011, FR-012
- [x] T026 [US2] Add javadoc to executeAllDatasets() method with examples and behavior details
- [x] T027 [US2] Run US2 unit tests: mvn test -Dtest=ReportServiceTest
- [x] T028 [US2] Test empty datasets edge case (report with zero datasets)
- [x] T029 [US2] Test parameter sharing behavior (same map reference passed to all datasets)

**Checkpoint**: Multi-dataset execution functional - unit tests pass, ready for integration testing

---

## Phase 5: Integration & End-to-End Validation

**Purpose**: Validate complete system integration and cross-story scenarios

- [x] T030 Create test data for MultiDatasetIntegrationTest: report with 3 datasets in src/test/resources/test-schema.sql
- [x] T031 Implement integration test: testExecuteAllDatasets_HappyPath in MultiDatasetIntegrationTest.java
- [x] T032 Implement integration test: testExecuteAllDatasets_FailFast in MultiDatasetIntegrationTest.java
- [x] T033 Implement integration test: testExecuteAllDatasets_EmptyDatasets in MultiDatasetIntegrationTest.java
- [x] T034 Implement integration test: testParameterExtraction in MultiDatasetIntegrationTest.java
- [x] T035 Run full integration test suite: mvn test -Dtest=MultiDatasetIntegrationTest
- [x] T036 Verify execution order preserved (LinkedHashMap maintains insertion order)
- [x] T037 Test with SQL Server if available (H2 has stored procedure limitations)
- [x] T038 Run full test suite: mvn test (expect baseline + 14 new tests passing)

**Checkpoint**: Full integration verified - all tests pass including cross-story scenarios

---

## Phase 6: Documentation & Polish

**Purpose**: Update documentation and validate examples

- [x] T039 [P] Update CLAUDE.md with Dataset Lookup section and getDatasetById() example
- [x] T040 [P] Update CLAUDE.md with Multi-Dataset Execution section and executeAllDatasets() example
- [x] T041 [P] Add code comparison example to CLAUDE.md showing 87% code reduction (15 lines → 2 lines)
- [x] T042 [P] Document H2 limitation for multi-dataset execution in CLAUDE.md Known Limitations section
- [x] T043 Verify javadocs are complete and accurate for both new methods
- [x] T044 Run javadoc generation: mvn javadoc:javadoc and verify no warnings
- [x] T045 Create before/after code example demonstrating the feature value
- [x] T046 Final test run: mvn clean test (verify all tests pass)

**Checkpoint**: Documentation complete, codebase clean, ready for PR

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup - just review tasks (no code changes)
- **User Story 1 (Phase 3)**: Depends on Phase 2 review - can start immediately after
- **User Story 2 (Phase 4)**: Depends on Phase 2 review - INDEPENDENT of US1 (can parallelize)
- **Integration (Phase 5)**: Depends on US1 AND US2 completion
- **Documentation (Phase 6)**: Can run in parallel with Phase 5

### User Story Dependencies

- **User Story 1 (P1)**: Independent - only needs review of existing code
- **User Story 2 (P2)**: Independent - only needs review of existing code (does NOT depend on US1)

**Key Insight**: US1 and US2 are INDEPENDENT. They can be implemented in parallel if resources allow.

### Within Each User Story

**US1 Execution Order**:
1. Tests FIRST (T007-T010) - all written and failing
2. Implementation (T011-T015) - make tests pass
3. Verification - all tests green

**US2 Execution Order**:
1. Tests FIRST (T016-T022) - all written and failing
2. Implementation (T023-T029) - make tests pass
3. Verification - unit tests green

### Parallel Opportunities

**Setup Phase**:
- T001, T002 can run in parallel (different verification tasks)

**User Story 1 Tests**:
- T007, T008, T009 all parallel (different test methods in same file)

**User Story 2 Tests**:
- T016, T017 parallel (different test files)
- T018, T019, T020, T021 can be prepared in parallel (different test methods)

**Documentation Phase**:
- T039, T040, T041, T042 all parallel (different sections of CLAUDE.md)

**Major Parallelization Opportunity**:
- **US1 (Phase 3) and US2 (Phase 4) can run ENTIRELY in parallel** (different files, no dependencies)

---

## Parallel Example: User Stories

**If you have 2 developers or want to work on features concurrently:**

```bash
# Developer A or Session 1: User Story 1
Tasks: T007-T015 (Dataset Lookup)
Files: ReportMetadata.java, ReportMetadataTest.java
Outcome: Working dataset lookup method

# Developer B or Session 2: User Story 2
Tasks: T016-T029 (Multi-Dataset Execution)
Files: ReportService.java, ReportServiceTest.java, MultiDatasetIntegrationTest.java
Outcome: Working multi-dataset execution method

# Both converge on Phase 5 (Integration)
Tasks: T030-T038
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

**Fastest path to working feature**:
1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Review existing code (T004-T006)
3. Complete Phase 3: User Story 1 (T007-T015)
4. **STOP and VALIDATE**: Run `mvn test -Dtest=ReportMetadataTest`
5. Dataset lookup is now working - can use independently

**Deliverable**: Working getDatasetById() method with tests

**Value**: Developers can lookup specific datasets without iteration (addresses 70% of use cases)

---

### Incremental Delivery (Full Feature)

**Complete implementation**:
1. Complete Setup + Review → Foundation ready
2. Add User Story 1 → Test independently → Working MVP
3. Add User Story 2 → Test independently → Multi-dataset execution added
4. Run Integration tests → Full system validated
5. Add Documentation → Production ready

**Each phase adds value without breaking previous functionality**

---

### Parallel Team Strategy

**With 2 developers or parallel sessions**:
1. Both work together on Setup + Review (T001-T006)
2. Once review complete:
   - Developer A / Session 1: User Story 1 (T007-T015)
   - Developer B / Session 2: User Story 2 (T016-T029)
3. Both converge on Integration (T030-T038)
4. Both work on Documentation (T039-T046)

**Stories complete independently and integrate at the end**

---

## Task Summary

**Total Tasks**: 46

**By Phase**:
- Setup: 3 tasks
- Foundational: 3 tasks (review only)
- User Story 1: 9 tasks (P1 - MVP)
- User Story 2: 14 tasks (P2)
- Integration: 9 tasks
- Documentation: 8 tasks

**By User Story**:
- US1 (Dataset Lookup): 9 tasks
- US2 (Multi-Dataset Execution): 14 tasks
- Shared/Infrastructure: 23 tasks

**Parallel Opportunities**: 11 tasks marked with [P]

**Test Tasks**: 16 test-related tasks (following test-first approach)

**MVP Scope**: Phases 1-3 (15 tasks) = Dataset lookup method

---

## Validation Checklist

Before considering feature complete:

- [ ] All 46 tasks completed and checked off
- [ ] All tests passing: `mvn test` shows US1 + US2 tests passing
- [ ] getDatasetById() works with valid and invalid IDs
- [ ] executeAllDatasets() executes all datasets and returns Map
- [ ] Fail-fast behavior verified (stops on first error)
- [ ] Parameter extraction verified (each dataset gets what it needs)
- [ ] Empty datasets edge case handled (returns empty Map)
- [ ] Exception messages include context (datasourceId, reportId)
- [ ] Documentation updated: CLAUDE.md has examples
- [ ] Javadocs complete for both new methods
- [ ] No regressions: Existing framework tests still pass (104/108 baseline)
- [ ] Code complexity: Both methods under 15 lines (simple helpers)

---

## Notes

- **Test-First**: Write tests before implementation (Constitution Principle V)
- **Simple Enhancement**: Only 2 files modified (ReportMetadata.java, ReportService.java)
- **No New Dependencies**: Uses 100% existing framework infrastructure
- **Independent Stories**: US1 and US2 can be implemented in any order or in parallel
- **Commit Strategy**: Commit after each user story completion (T015, T029) or at logical checkpoints
- **Stop at Checkpoints**: Validate story works independently before moving to next phase
- **Low Risk**: Additive changes only, no modifications to existing methods
- **Quick Win**: Total implementation time estimated at 2-3 hours
