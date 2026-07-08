# Speckit Workflow Guide

Complete guide for adding new features to the Reporting Framework using Speckit.

## Quick Start

```bash
/speckit-specify     # 1. Describe your feature
/speckit-plan        # 2. Generate implementation plan
/speckit-tasks       # 3. Create task list
/speckit-implement   # 4. Execute implementation
/speckit-converge    # 5. Verify completeness
# Then commit your changes
```

---

## Detailed Workflow

### Step 1: Create Feature Specification

**Command:** `/speckit-specify`

**Purpose:** Create a formal specification document from your feature description.

**What happens:**
- You describe your feature in natural language
- Speckit creates a formal specification document (`spec.md`)
- Defines requirements, user stories, acceptance criteria, success metrics
- Output location: `specs/[feature-name]/spec.md`

**Example input:**
```
"Add hierarchical JSON mapping that transforms flat DataSets into nested
structures - like grouping employees under their departments"
```

**What you get:**
- Functional requirements (FR-001, FR-002, ...)
- User stories with acceptance scenarios
- Success criteria (measurable outcomes)
- Key entities and their relationships
- Assumptions and constraints

**Best practice:** Review the spec carefully before proceeding. Ask for clarifications if anything is unclear.

---

### Step 2: Create Implementation Plan

**Command:** `/speckit-plan`

**Purpose:** Generate detailed technical plan from your specification.

**Prerequisites:** Must have `spec.md` from Step 1

**What happens:**
- Analyzes your spec.md
- Creates detailed implementation plan (`plan.md`)
- Defines architecture, technical decisions, file structure
- Checks against constitution principles
- Output: `specs/[feature-name]/plan.md`

**What you get:**
- Technical context (languages, dependencies, constraints)
- Architecture and design decisions
- File structure (what to create/modify/delete)
- Constitution compliance check
- Complexity tracking and justifications

**Best practice:** Review architecture decisions and file changes. Ensure they align with your project structure.

---

### Step 3: Generate Task List

**Command:** `/speckit-tasks`

**Purpose:** Break down the plan into actionable, dependency-ordered tasks.

**Prerequisites:** Must have `spec.md` and `plan.md`

**What happens:**
- Reads spec.md and plan.md
- Generates dependency-ordered task list (`tasks.md`)
- Groups tasks by phase and user story
- Marks parallel opportunities with [P]
- Includes test-first tasks
- Output: `specs/[feature-name]/tasks.md`

**Task format:**
```markdown
## Phase 1: Setup & Foundation

- [ ] T001 Create base classes in src/main/java/...
- [ ] T002 [P] Add unit tests (can run in parallel)
- [ ] T003 Update pom.xml dependencies

**Checkpoint**: Foundation ready for feature implementation

## Phase 2: User Story 1 - Core Functionality

- [ ] T004 [US1] Create integration test (test-first)
- [ ] T005 [US1] Implement core logic
...
```

**Task markers:**
- `[P]` - Can be parallelized (independent of other tasks)
- `[US1]` - Belongs to User Story 1
- `[US2]` - Belongs to User Story 2

**Best practice:** Review task order and dependencies. Understand checkpoints before starting implementation.

---

### Step 4: Implement Tasks

**Command:** `/speckit-implement`

**Purpose:** Execute the implementation plan by completing tasks sequentially.

**Prerequisites:** Must have `tasks.md`

**What happens:**
- Reads tasks.md sequentially
- Implements each unchecked task
- Follows test-first approach (write tests before code)
- Checks off tasks as completed
- Stops at checkpoints for validation

**Interactive mode options:**
```bash
# Implement specific phase
"implement phase 1 tasks"
"implement phase 3 tasks"

# Implement specific task range
"implement tasks T010-T020"

# Continue from where you left off
"continue implementing remaining tasks"
```

**Test-first approach:**
- Tests are written BEFORE implementation
- Tests should FAIL initially (red)
- Implementation makes tests pass (green)
- This is NON-NEGOTIABLE per Constitution Principle V

**Checkpoints:**
- Implementation stops at phase boundaries
- Run tests to validate: `mvn test`
- Verify checkpoint criteria before continuing

**Best practice:**
- Implement phase-by-phase, not all at once
- Run tests after each phase
- Commit after logical groups of tasks (not after every task)

---

### Step 5: Verify Convergence

**Command:** `/speckit-converge`

**Purpose:** Verify implementation completeness against spec, plan, and tasks.

**Prerequisites:** All tasks in `tasks.md` should be checked off

**What happens:**
- Compares codebase against spec.md, plan.md, tasks.md
- Identifies any missing/partial/contradictory implementations
- Validates constitution compliance
- Either reports "CONVERGED ✅" or appends new tasks for gaps

**Possible outcomes:**

#### Outcome 1: Converged ✅
```
✅ CONVERGED - The implementation fully satisfies the specification

All requirements met:
- Functional Requirements: 25/25 ✅
- User Stories: 2/2 ✅
- Constitution Compliance: 7/7 ✅
- Tests passing: 98/100 ✅

No convergence tasks to append. Ready to commit!
```

**Action:** Proceed to commit your changes.

#### Outcome 2: Gaps Found
```
## Convergence Findings

| ID | Gap Type | Severity | Source | Remaining Work |
|----|----------|----------|--------|----------------|
| F1 | missing  | HIGH     | FR-008 | Add error handling for... |
| F2 | partial  | MEDIUM   | SC-003 | Complete performance test |

## Phase 7: Convergence

- [ ] T063 Add error handling per FR-008 (missing)
- [ ] T064 Complete performance test per SC-003 (partial)
```

**Action:**
1. Implement the appended convergence tasks
2. Run `/speckit-converge` again
3. Repeat until converged

**Best practice:** Always run convergence before committing. Fix CRITICAL findings first.

---

### Step 6: Commit Changes

**Command:** Standard git workflow or speckit extension

**Purpose:** Commit your completed, converged feature.

**Prerequisites:** `/speckit-converge` reports ✅ CONVERGED

**Standard approach:**
```bash
git add -A
git commit -m "feat: Your feature description

[Detailed explanation of changes]

Tasks Completed: T001-T062
Constitution Compliance: ✅ All principles
Convergence Status: ✅ CONVERGED

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>"
```

**Follow constitution commit format:**
- Type: feat, fix, refactor, test, docs, chore
- Concise subject line
- Detailed body with key changes
- Reference tasks completed
- Include co-author attribution

---

## Optional Helper Commands

### Clarify Requirements

**Command:** `/speckit-clarify`

**When to use:** Your spec is vague or underspecified

**What it does:**
- Analyzes spec.md for ambiguities
- Asks up to 5 targeted clarification questions
- Encodes answers back into spec.md
- Improves spec clarity before planning

**Example questions:**
- "Should the export support multiple sheets or single sheet only?"
- "What error handling is expected when the file cannot be written?"
- "Should formatting be configurable or use defaults?"

---

### Analyze Consistency

**Command:** `/speckit-analyze`

**When to use:** Checking for conflicts between documents

**What it does:**
- Non-destructive analysis across spec.md, plan.md, tasks.md
- Identifies inconsistencies, contradictions, gaps
- Reports quality issues
- Does NOT modify files

**Checks for:**
- Requirements mentioned in spec but missing from plan
- Tasks that don't trace to requirements
- Conflicting decisions across documents

---

### Generate Checklist

**Command:** `/speckit-checklist`

**When to use:** Creating custom validation checklist

**What it does:**
- Generates checklist based on requirements
- Customized for your specific feature
- Helps track progress

---

### Update Constitution

**Command:** `/speckit-constitution`

**When to use:** Setting or updating project-wide principles

**What it does:**
- Creates or updates `.specify/memory/constitution.md`
- Defines principles that ALL features must follow
- Examples: immutability, test-first, type safety
- Ensures dependent templates stay in sync

**Your project already has:**
- Reporting Framework Constitution v1.0.0
- 7 core principles (Metadata-Driven, Type Safety, Immutability, etc.)
- Technical standards
- Development workflow guidelines

---

## Complete Example: Add Excel Export Feature

Here's a real end-to-end example:

### Step 1: Specify

```bash
/speckit-specify
```

**User input:**
```
Add ability to export DataSet results to Excel (.xlsx) format with formatting
support. Features needed:
- Export any DataSet to Excel file
- Auto-format headers (bold, background color)
- Auto-size columns based on content
- Support number formats (decimals, currency, dates)
- Handle large datasets (streaming for >10k rows)
- Return file path or byte array
```

**Output:** `specs/002-excel-export/spec.md` created with:
- FR-001: System must export DataSet to .xlsx format
- FR-002: Headers must be formatted (bold, colored background)
- US1: As a user, I want to export report results to Excel
- SC-001: Export completes in <5 seconds for 10k rows
- ...

### Step 2: Plan

```bash
/speckit-plan
```

**Output:** `specs/002-excel-export/plan.md` created with:
- Dependency: Apache POI 5.2.3 for Excel generation
- Architecture: ExcelExporter class with formatting strategy
- Files to create:
  - `src/main/java/com/reporting/framework/export/ExcelExporter.java`
  - `src/main/java/com/reporting/framework/export/ExcelFormatter.java`
  - `src/test/java/com/reporting/framework/export/ExcelExporterTest.java`
- Constitution check: ✅ All principles satisfied

### Step 3: Generate Tasks

```bash
/speckit-tasks
```

**Output:** `specs/002-excel-export/tasks.md` with 35 tasks:
```markdown
## Phase 1: Setup (5 tasks)
- [ ] T001 Add Apache POI dependency to pom.xml
- [ ] T002 Create export package structure
...

## Phase 2: Core Export (12 tasks)
- [ ] T006 [P] Create ExcelExporterTest (test-first)
- [ ] T007 Create ExcelExporter class
...

## Phase 3: Formatting (8 tasks)
- [ ] T018 Implement header formatting
...

## Phase 4: Integration (10 tasks)
- [ ] T026 Integrate with ReportService
...
```

### Step 4: Implement

```bash
# Start with Phase 1
"implement phase 1 tasks"
```

**What happens:**
1. Adds Apache POI to pom.xml
2. Creates package structure
3. Checks off T001-T005

```bash
# Continue with Phase 2
"implement phase 2 tasks"
```

**What happens:**
1. Creates ExcelExporterTest FIRST (red tests)
2. Implements ExcelExporter (makes tests green)
3. Validates at checkpoint

```bash
# Continue with remaining phases
"implement phase 3 tasks"
"implement phase 4 tasks"
```

### Step 5: Verify Convergence

```bash
/speckit-converge
```

**First run finds a gap:**
```
## Convergence Findings

| ID | Gap Type | Severity | Source | Remaining Work |
|----|----------|----------|--------|----------------|
| F1 | missing  | HIGH     | SC-002 | Performance test for 50k rows |

## Phase 5: Convergence
- [ ] T036 Add performance test per SC-002 (missing)
```

**Implement convergence task:**
```bash
"implement phase 5 tasks"
```

**Run convergence again:**
```bash
/speckit-converge
```

**Result:**
```
✅ CONVERGED - All requirements met
Ready to commit!
```

### Step 6: Commit

```bash
"commit the changes"
```

**Commit created:**
```
feat: Add Excel export functionality for DataSet

Complete implementation of Excel (.xlsx) export with formatting support.

Key Features:
- Export any DataSet to Excel format using Apache POI 5.2.3
- Automatic header formatting (bold, colored background)
- Column auto-sizing based on content
- Number format support (decimals, currency, dates)
- Streaming support for large datasets (>10k rows)

Implementation:
- Created ExcelExporter with configurable formatting
- Added ExcelFormatter for cell styling
- Integrated with ReportService API
- Added comprehensive tests (12 test scenarios)

Performance:
- 10k rows: 3.2s (target: <5s) ✅
- 50k rows: 14.8s (streaming mode)

Tasks Completed: T001-T036 (35 tasks across 5 phases)
Constitution Compliance: ✅ All principles
Convergence Status: ✅ CONVERGED

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

---

## Best Practices

### 1. Start Small and Focused

✅ **Good:**
```
"Add CSV export functionality for DataSet with basic formatting"
```

❌ **Too broad:**
```
"Add export to CSV, Excel, PDF, JSON with advanced formatting,
templates, scheduling, and email delivery"
```

**Why:**
- Smaller features are easier to spec, plan, and implement
- You can always add follow-up features
- Easier to test and validate

### 2. Review Before Proceeding

**Workflow:**
1. Run `/speckit-specify` → **STOP and review spec.md**
2. Run `/speckit-plan` → **STOP and review plan.md**
3. Run `/speckit-tasks` → **STOP and review tasks.md**
4. Run `/speckit-implement` → **STOP at each checkpoint**

**Why:** Catching issues early prevents wasted implementation time.

### 3. Implement in Phases

✅ **Good approach:**
```bash
"implement phase 1 tasks"  # Setup
mvn test                   # Validate
"implement phase 2 tasks"  # Core functionality
mvn test                   # Validate
...
```

❌ **Risky approach:**
```bash
"implement all tasks"      # Everything at once
# No validation until the end
```

**Why:** Phase-by-phase allows early validation and course correction.

### 4. Always Converge Before Committing

**Required workflow:**
```bash
# After all tasks complete
/speckit-converge

# If converged:
commit the changes

# If gaps found:
"implement convergence tasks"
/speckit-converge  # Run again
# Repeat until converged
```

**Why:** Convergence ensures nothing was missed and all requirements are met.

### 5. Follow Test-First Approach

**Correct order:**
1. Write test (should fail - red)
2. Implement code (make test pass - green)
3. Refactor if needed

**Why:** Constitution Principle V (Test-First) is NON-NEGOTIABLE. This is how your project ensures quality.

### 6. Constitution Compliance

**All features must comply with:**
- I. Metadata-Driven Configuration
- II. Type Safety Without Casting
- III. Immutability
- IV. Fluent DSL for Transformations
- V. Test-First with Integration Focus
- VI. Transparent Known Limitations
- VII. Simplicity Over Abstraction

**Why:** Constitution violations appear as CRITICAL findings in convergence and must be fixed.

---

## Quick Reference Table

| Command | Purpose | When to Use | Output |
|---------|---------|-------------|--------|
| `/speckit-specify` | Create feature spec | Starting new feature | `spec.md` |
| `/speckit-plan` | Create implementation plan | After spec approved | `plan.md` |
| `/speckit-tasks` | Generate task list | After plan reviewed | `tasks.md` |
| `/speckit-implement` | Execute tasks | Ready to code | Checks off tasks |
| `/speckit-converge` | Verify completeness | Before committing | ✅ or new tasks |
| `/speckit-clarify` | Improve spec clarity | Spec is vague | Updated `spec.md` |
| `/speckit-analyze` | Check consistency | Detecting conflicts | Analysis report |
| `/speckit-constitution` | Update principles | Setting standards | `constitution.md` |
| `/speckit-checklist` | Generate checklist | Custom validation | Checklist file |

---

## Project File Structure

After creating features with speckit:

```
reporting-framework/
├── .specify/
│   ├── memory/
│   │   └── constitution.md              # Project-wide principles (v1.0.0)
│   ├── templates/                       # Spec/plan/task templates
│   └── scripts/                         # Speckit helper scripts
│
├── specs/
│   ├── 001-metadata-json-structure/     # Completed feature
│   │   ├── spec.md                      # Feature specification
│   │   ├── plan.md                      # Implementation plan
│   │   ├── tasks.md                     # Task list (62 tasks)
│   │   ├── research.md                  # Phase 0 research
│   │   ├── data-model.md                # Entity definitions
│   │   ├── quickstart.md                # Validation guide
│   │   └── contracts/                   # JSON schemas, examples
│   │
│   └── 002-your-new-feature/            # New feature (you'll create this)
│       ├── spec.md
│       ├── plan.md
│       └── tasks.md
│
├── src/
│   ├── main/java/com/reporting/framework/
│   │   └── ...                          # Implementation
│   └── test/java/com/reporting/framework/
│       └── ...                          # Tests
│
├── CLAUDE.md                            # Development guide
├── SPECKIT_WORKFLOW.md                  # This file
└── pom.xml                              # Project configuration
```

---

## Common Issues and Solutions

### Issue: "Spec is too vague"

**Symptoms:** Plan has many questions, tasks are unclear

**Solution:**
```bash
/speckit-clarify
# Answer the clarification questions
/speckit-plan  # Regenerate plan with clearer spec
```

### Issue: "Implementation doesn't match spec"

**Symptoms:** Convergence finds many gaps

**Solution:**
```bash
/speckit-converge
# Review findings
"implement convergence tasks"
/speckit-converge  # Verify again
```

### Issue: "Too many tasks (100+)"

**Symptoms:** Feature scope is too large

**Solution:**
- Break into multiple smaller features
- Implement MVP first, then enhancements
- Focus on single user story initially

### Issue: "Constitution violation found"

**Symptoms:** CRITICAL findings in convergence

**Solution:**
- Fix constitution violations FIRST (highest priority)
- These are non-negotiable project principles
- May require architectural changes

### Issue: "Tests failing after implementation"

**Symptoms:** Red tests after "implement phase X"

**Solution:**
- This is expected if test-first approach was followed
- Implementation should make tests green
- If still red, debug the implementation

---

## Tips for Success

### 1. Keep Features Independent

Each feature should be self-contained and independently deployable.

### 2. Use Descriptive Feature Names

❌ `feature-1`, `new-export`
✅ `excel-export`, `hierarchical-json-mapping`, `redis-caching`

### 3. Commit After Each Feature

Don't accumulate multiple features before committing.

### 4. Document Limitations

If you discover constraints during implementation, document them in CLAUDE.md per Constitution Principle VI.

### 5. Leverage Parallel Tasks

Tasks marked with `[P]` can be implemented concurrently if you have multiple developers or want to batch similar work.

### 6. Test at Checkpoints

Always run `mvn test` at phase boundaries to catch issues early.

---

## Example: Starting a New Feature Right Now

If you're ready to add a feature:

```bash
# 1. Describe what you want
/speckit-specify

# When prompted, provide a clear description:
"Add Redis caching layer for report metadata to reduce database load.
Support TTL configuration, cache invalidation, and fallback to database
when Redis is unavailable."

# 2. Review the generated spec.md

# 3. Generate plan
/speckit-plan

# 4. Review the plan.md

# 5. Generate tasks
/speckit-tasks

# 6. Review tasks.md, then implement
"implement phase 1 tasks"

# Continue through all phases...

# 7. Verify and commit
/speckit-converge
"commit the changes"
```

---

## Additional Resources

### In Your Project

- **Constitution:** `.specify/memory/constitution.md` - Project principles
- **Development Guide:** `CLAUDE.md` - Implementation patterns and limitations
- **Completed Feature:** `specs/001-metadata-json-structure/` - Reference example

### Speckit Documentation

Each speckit skill has detailed documentation in:
- `.claude/skills/speckit-*/SKILL.md`

### Need Help?

If you encounter issues or need clarification during the workflow, ask:
- "How do I handle X in speckit?"
- "What does this convergence finding mean?"
- "Should I split this into multiple features?"

---

**Last Updated:** 2025-07-07
**Project:** Reporting Framework
**Constitution Version:** 1.0.0
