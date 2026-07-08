<!--
Sync Impact Report (Version 1.0.0):
─────────────────────────────────────────────────────────────────
VERSION CHANGE: Initial constitution (1.0.0)
BUMP RATIONALE: First ratification - establishing foundational governance

MODIFIED PRINCIPLES:
- Created 7 core principles from project context and CLAUDE.md

ADDED SECTIONS:
- Core Principles (7 principles)
- Technical Standards
- Development Workflow
- Governance

TEMPLATES STATUS:
✅ spec-template.md - Verified (no conflicts with constitution principles)
✅ plan-template.md - Verified (constitution check section aligns)
✅ tasks-template.md - Verified (test-first workflow supported)

FOLLOW-UP TODOS:
- None - all placeholders filled
─────────────────────────────────────────────────────────────────
-->

# Reporting Framework Constitution

## Core Principles

### I. Metadata-Driven Configuration

All report definitions MUST be configured via database metadata in the `REPORT_METADATA` table, not hardcoded in application code. This ensures:
- Runtime flexibility without code changes or deployments
- Clear separation between framework code and business logic
- Version-controlled configuration through database migrations
- Centralized report catalog accessible to operational teams

**Rationale**: Business users need to define new reports without engineering involvement. Metadata-driven design makes the framework a true platform, not a collection of one-off implementations.

### II. Type Safety Without Casting

The framework MUST provide typed accessor methods (getString, getInt, getBigDecimal, getLocalDate, etc.) that eliminate manual casting. Users MUST NOT need to cast Object types when accessing data.

```java
// REQUIRED pattern
String name = row.getString("NAME");
Integer id = row.getInt("EMPLOYEE_ID");

// FORBIDDEN pattern
String name = (String) row.get("NAME");
Integer id = (Integer) row.get("EMPLOYEE_ID");
```

**Rationale**: Type safety prevents runtime ClassCastException errors and improves IDE autocomplete support. Manual casting is error-prone and creates brittle code.

### III. Immutability

All data transformations MUST return new instances; original data MUST remain unchanged. This applies to:
- DataSet transformations (filter, select, orderBy, etc.)
- DataRow modifications (with, withColumn)
- Aggregation and join operations

**Rationale**: Immutability prevents action-at-a-distance bugs where one part of code unexpectedly modifies data used elsewhere. It enables safe concurrent access and makes data flow explicit.

### IV. Fluent DSL for Transformations

Complex data operations MUST be expressible through a chainable, fluent API. The DSL MUST:
- Read like natural language describing the transformation
- Support method chaining for common operations
- Have predictable execution order regardless of chaining sequence
- Provide both builder-style (DataQuery) and static utility (DataOperations) APIs

**Rationale**: Fluent APIs reduce cognitive load by making data pipelines self-documenting. Predictable execution order prevents subtle bugs from method ordering.

### V. Test-First with Integration Focus (NON-NEGOTIABLE)

All features MUST have integration tests before implementation. The test suite MUST:
- Prioritize integration tests that validate end-to-end workflows
- Use FullWorkflowIntegrationTest as the primary validation mechanism
- Verify realistic scenarios with multi-step transformations
- Test data flows from stored procedure execution through JSON output

Unit tests are supplementary; integration tests are mandatory.

**Rationale**: This framework orchestrates multiple components (metadata, execution, transformation, serialization). Integration tests catch contract breaks between layers that unit tests miss. The H2 stored procedure limitation makes integration tests the only reliable validation approach.

### VI. Transparent Known Limitations

All known limitations MUST be documented in CLAUDE.md with:
- Clear description of the limitation
- Concrete code examples showing the problem
- Explicit workarounds or alternative approaches
- Root cause explanation for maintainers
- Path to future resolution if known

Current documented limitations:
1. H2 stored procedure incompatibility (requires SQL Server for full testing)
2. Multiple aggregations on same column (Map key collision)
3. Computed column dependencies (columns can't reference other computed columns in same execute())

**Rationale**: Transparent documentation of limitations builds user trust and prevents frustration. Users can design around known constraints rather than discovering them through failure.

### VII. Simplicity Over Abstraction

Feature implementations MUST be simple and focused. The framework MUST NOT:
- Add features, error handling, or abstraction layers beyond what is requested
- Create utilities, helpers, or frameworks for one-time operations
- Refactor surrounding code when fixing bugs or adding features
- Add docstrings, comments, or type annotations to unchanged code
- Design for hypothetical future requirements (YAGNI principle)
- Use feature flags or backward-compatibility shims when direct changes suffice

**Rationale**: Over-engineering creates maintenance burden and obscures actual requirements. Three similar lines of code are better than a premature abstraction. Complexity should only be added when current requirements demand it.

## Technical Standards

### Java Platform

- **Java Version**: 17 or higher (defined in pom.xml)
- **Build Tool**: Maven 3.6+
- **Code Style**: Follow existing patterns in codebase

### Testing Requirements

- **Primary**: JUnit Jupiter 5.10.3 with AssertJ 3.26.0 for fluent assertions
- **Test Database**: H2 1.4.200 (MUST NOT use H2 2.x due to incompatibilities)
- **Coverage**: Integration tests MUST cover all transformation workflows
- **Test Data**: Use realistic multi-table scenarios (employees, departments, sales pattern from FullWorkflowIntegrationTest)

### Dependencies

All runtime dependencies MUST serve a clear purpose:
- **Jackson 2.17.2**: JSON serialization (DataSet.toJSON, toPrettyJSON)
- **SLF4J 2.0.13 + Logback**: Structured logging
- **SQL Server JDBC 12.8.1**: Production database connectivity
- **Google Guava 33.0.0**: Utilities (CaseFormat for NamingStrategy)

New dependencies MUST be justified in terms of functionality that cannot be achieved with existing dependencies or reasonable custom code.

### Package Organization

```
com.reporting.framework/
├── api/            # Public API (ReportService)
├── data/           # DataSet, DataRow, DataQuery, DataOperations
├── metadata/       # Metadata loading and caching
├── executor/       # Stored procedure execution
├── mapper/         # ResultSet to Map conversion
├── connection/     # Connection providers
└── exception/      # Framework exceptions
```

Package structure MUST reflect architectural layers. Cross-layer dependencies MUST flow in one direction: api → data/metadata/executor → mapper/connection.

## Development Workflow

### Branch Strategy

- **Main Branch**: `main` (stable, production-ready)
- **Feature Branches**: Descriptive names (e.g., `dataset-impl`, `hierarchical-json-mapping`)
- **Commits**: Follow existing pattern with co-author attribution when working with AI assistants

### Commit Message Format

```
<type>: <concise description>

<optional detailed explanation>

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

Types: feat, fix, refactor, test, docs, chore

### Pre-Commit Validation

Before committing feature work:
1. Run `mvn test` (expect 69 tests: 52 passed, 17 skipped as baseline)
2. Verify FullWorkflowIntegrationTest passes (primary validation)
3. Check that no new warnings introduced in build output
4. Update CLAUDE.md if architectural changes or new limitations discovered

### Code Review Focus Areas

Reviewers MUST verify:
- **Immutability**: Transformations return new instances
- **Type Safety**: No manual casting in client-facing APIs
- **Test Coverage**: Integration test exists and validates end-to-end workflow
- **Limitation Documentation**: New constraints documented in CLAUDE.md
- **Simplicity**: No unnecessary abstraction or over-engineering

## Governance

### Constitutional Authority

This constitution supersedes all other development practices and preferences. When conflict arises between:
- Constitution principle and implementation convenience → Constitution wins
- Constitution principle and performance optimization → Document exception with justification
- Constitution principle and external library patterns → Adapt library use to constitution

### Amendment Process

Amendments require:
1. Documented justification explaining why current principle is insufficient
2. Evidence that amendment won't create conflicts with existing principles
3. Version bump following semantic versioning:
   - **MAJOR**: Backward-incompatible governance changes, principle removals
   - **MINOR**: New principle additions, material expansion of guidance
   - **PATCH**: Clarifications, wording refinements, typo fixes
4. Update to LAST_AMENDED_DATE
5. Sync Impact Report at top of constitution file documenting changes
6. Propagation of changes to dependent templates (spec, plan, tasks)

### Compliance Review

All pull requests MUST include:
- Verification that constitution principles are followed
- Justification for any complexity introduced
- Reference to CLAUDE.md for implementation guidance

### Guidance File

Runtime development guidance is maintained in `CLAUDE.md` (not this constitution). CLAUDE.md contains:
- Build and test commands
- Architecture overview and execution flow
- Known limitations with workarounds
- Common patterns and anti-patterns
- Session history and context for AI assistants

Constitution defines "what" and "why"; CLAUDE.md defines "how" and "when".

**Version**: 1.0.0 | **Ratified**: 2026-07-07 | **Last Amended**: 2026-07-07
