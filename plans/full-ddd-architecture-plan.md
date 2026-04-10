# SitePulse Engine Full DDD Architecture Plan

## Purpose

This document describes what must be done to move `sitepulse-engine` from its current DDD-lite structure to a fully domain-driven architecture that is:

- explicit about bounded contexts and aggregate ownership
- readable and maintainable for long-term work
- aligned with modern Spring Boot best practices
- friendly to static analysis and Sonar rules
- safe to refactor incrementally without breaking the public API

This plan is intentionally pragmatic. It does not recommend a rewrite. It recommends controlled refactoring in phases with clear acceptance criteria.

## Current State Summary

The codebase already has important structural improvements:

- separate HTTP contract module: `sitepulse-engine-http-api`
- app module: `sitepulse-engine-app`
- clearer context packages: `project`, `sync`, `detection`, `plan`, `report`, `metrics`, `alert`
- many use cases and ports already introduced
- infrastructure adapters already exist for JPA, storage, Dropbox, OpenAI, PDF, YOLO

However, it is still not fully DDD because:

- many domain models are still mostly data holders
- invariants and lifecycle rules still live partly in application use cases
- aggregate boundaries are not yet formally defined and enforced
- some responses and internal flows are still weakly typed
- cross-context orchestration is still direct rather than event-driven
- read and write concerns are not yet consistently separated
- infrastructure and persistence concerns still influence some domain design choices

## End Goal

The desired target is a codebase where:

1. Each bounded context has a clear domain model, its own ubiquitous language, and explicit ownership of business rules.
2. Aggregate roots own state transitions and invariants.
3. Application services orchestrate use cases, not business rules.
4. Infrastructure is behind ports and adapters.
5. Read use cases and write use cases are clearly separated.
6. HTTP contracts remain stable and external to the domain.
7. Transactions are deliberate and aligned to aggregate boundaries.
8. Code stays readable, testable, and Sonar-clean.

## Architectural Principles

Follow these principles consistently.

### 1. Bounded Context First

All architectural decisions should start from bounded contexts, not frameworks or folders.

Target contexts:

- `project`
- `sync`
- `detection`
- `plan`
- `report`
- `metrics`
- `alert`
- `common`

The `integration` package should eventually disappear as a generic dumping ground. External integrations should live under the infrastructure of the context that uses them.

Examples:

- Dropbox sync adapter belongs to `sync.infrastructure.external`
- YOLO gateway belongs to `detection.infrastructure.external`
- OpenAI milestone evaluator belongs to `plan.infrastructure.external`
- OpenAI report generator belongs to `report.infrastructure.external`

### 2. Domain Owns Behavior

Domain objects must not be passive containers.

They should own:

- state transitions
- validation
- consistency rules
- domain language

Use cases should call domain behavior, not rebuild domain rules inline.

### 3. Aggregate Roots Enforce Consistency

Each aggregate root should define the consistency boundary and repository boundary.

Likely aggregate roots:

- `Project`
- `SyncJob`
- `DetectionImage` or `Image`
- `ConstructionPlan`
- `ProgressReport`
- `Alert`

Child entities should not be modified bypassing the aggregate root unless there is a very strong performance or ownership reason.

### 4. Application Layer Orchestrates

Use cases should:

- load aggregates
- call domain behavior
- persist results
- coordinate external ports
- define transaction boundaries

Use cases should not:

- encode business rules as raw if/else logic when the rule belongs in the domain
- depend on web DTOs
- return entities

### 5. Infrastructure Stays Replaceable

JPA, Feign, OpenAI, Dropbox, MinIO, PDF parsers, schedulers, and event transports are infrastructure.

The domain must not know they exist.

### 6. CQRS-Lite For Clarity

Do not force full CQRS everywhere, but do separate:

- command use cases: change state
- query use cases: read projections

Read models should be optimized for API needs without distorting aggregate design.

## Target Package Structure

Use this shape consistently in `sitepulse-engine-app`.

```text
com.sitepulse.engine.<context>
|-- domain
|   |-- model
|   |-- service
|   |-- event
|   |-- policy
|   `-- port
|-- application
|   |-- usecase
|   |-- command
|   |-- query
|   `-- result
`-- infrastructure
    |-- persistence
    |-- external
    |-- event
    `-- mapper
```

Guidelines:

- `domain.model`: aggregates, entities, value objects, enums
- `domain.service`: business logic that does not naturally belong to one entity
- `domain.policy`: decision logic and rule policies
- `domain.event`: domain events
- `domain.port`: repository and gateway contracts
- `application.usecase`: command/query orchestration
- `application.command`: input models for write use cases
- `application.result`: output models
- `infrastructure.persistence`: JPA entities, Spring Data repositories, adapter implementations
- `infrastructure.external`: Dropbox/OpenAI/YOLO/MinIO/PDF adapters
- `infrastructure.mapper`: mapping helpers only if needed and only if they reduce duplication

## Public API Boundary

The public API must remain outside the core.

Rules:

1. `sitepulse-engine-http-api` owns all public API interfaces and HTTP DTOs.
2. Controllers in `sitepulse-engine-app` implement API interfaces and map between HTTP DTOs and application models.
3. Controllers must remain thin adapters.
4. No domain model may depend on HTTP DTOs.
5. No use case should accept `Map<String, Object>` from controllers.

### Required Improvements

- Replace remaining `Map<String, Object>` contracts in HTTP APIs with dedicated DTOs.
- Ensure all request DTOs use validation annotations where appropriate.
- Ensure API responses are explicit and documented in Swagger.

## Domain Model Work Needed

### 1. Identify Aggregate Roots Explicitly

For each context, define:

- aggregate root
- child entities
- value objects
- invariants
- allowed transitions

#### Project Context

Aggregate root:

- `Project`

Children:

- `Camera`

Potential value objects:

- `DropboxPath`
- `CameraKeyPrefix`
- `RoiPolygon`
- `ProjectLocation`

Rules to move into domain:

- camera ownership by project
- ROI validation
- allowed camera updates
- required project sync configuration

#### Sync Context

Aggregate root:

- `SyncJob`

Children:

- `ImageImport`
- `SyncFailure`

Potential value objects:

- `StorageObjectRef`
- `SourceImageRef`
- `CaptureTimestamp`

Rules to move into domain:

- start/finish/fail lifecycle
- image counters
- error accumulation policy
- valid job transitions

#### Detection Context

Aggregate root:

- `Image`

Children:

- `Detection`

Potential value objects:

- `BoundingBox`
- `Confidence`
- `ClassName`
- `ModelVersion`
- `RoiSettings`

Rules to move into domain:

- image lifecycle transitions
- detection replacement strategy
- result acceptance/rejection
- ROI filtering policy
- quality policy

#### Plan Context

Aggregate root:

- `ConstructionPlan`

Children:

- `PlanMilestone`

Potential value objects:

- `WeekNumber`
- `MilestoneAssessment`
- `ExpectedState`
- `ActualState`

Rules to move into domain:

- plan status lifecycle
- milestone update rules
- milestone evaluation rules
- delayed milestone escalation policy

#### Report Context

Aggregate root:

- `ProgressReport`

Potential value objects:

- `ReportPeriod`
- `ReportType`
- `ReportContent`

Rules to move into domain:

- valid report generation period
- summary derivation
- metadata completeness

#### Metrics Context

Aggregate root candidates:

- `DailyMetric`
- `WeeklyMetric`

Potential value objects:

- `ActivityIndex`
- `ProgressDelta`
- `RiskLevel`

Rules to move into domain:

- metric rollup formulas
- risk classification policy
- activity baseline policy

#### Alert Context

Aggregate root:

- `Alert`

Potential value objects:

- `AlertType`
- `AlertSeverity`
- `AlertStatus`
- `RecommendedAction`

Rules to move into domain:

- valid status transitions
- open-alert deduplication policy
- auto-resolution policy

### 2. Replace Primitive Obsession

Introduce value objects where they reduce ambiguity and Sonar complaints.

Priority list:

- `ProjectId`
- `CameraId`
- `SyncJobId`
- `ReportId`
- `MilestoneId`
- `DateRange`
- `StorageObjectRef`
- `DropboxPath`
- `RoiPolygon`
- `BoundingBox`
- `AlertType`
- `ReportType`

Rule:

- do not create value objects for everything
- introduce them for concepts with rules, validation, formatting, or repeated misuse risk

### 3. Push Rules Into Domain Methods

Examples of target style:

- `syncJob.start()`
- `syncJob.recordImportedImage()`
- `syncJob.fail(error)`
- `image.markProcessing()`
- `image.markDetected(modelVersion, detections, processedAt)`
- `constructionPlan.markReady()`
- `milestone.applyAssessment(assessment)`
- `alert.resolve()`

## Repositories And Persistence

### 1. Domain Repositories Must Be Interfaces Only

The domain/application layers should depend on repository ports only.

All Spring Data repositories and JPA entities must remain infrastructure concerns.

### 2. JPA Entities Must Not Become Business Models

JPA entities should be treated as persistence records only.

Rules:

- JPA entities stay in `infrastructure.persistence`
- domain objects stay in `domain.model`
- mapping between them must be explicit
- avoid leaking JPA entity types outside infrastructure

### 3. Repositories Should Operate On Aggregates

Avoid repository methods that expose internals in a way that bypasses aggregate rules.

Examples of problems to eliminate:

- updating child entities directly from multiple contexts
- ad-hoc query methods that let callers mutate parts of an aggregate without going through the root

### 4. Persistence Strategy Guidelines

- Keep Hibernate/JPA, but hide it behind adapters.
- Keep Flyway for schema change management.
- Avoid lazy-loading surprises in application code.
- Prefer explicit fetch design.
- Keep transactional logic out of entities.

## Application Layer Work Needed

### 1. One Use Case = One Intent

Use case names should clearly express behavior.

Good:

- `TriggerProjectSyncUseCase`
- `RunProjectAnalysisUseCase`
- `UploadConstructionPlanUseCase`
- `GenerateProgressReportUseCase`

Avoid:

- generic manager/service/facade names
- classes that expose unrelated methods

### 2. Split Commands And Queries Clearly

Commands:

- change state
- define transaction boundary
- return explicit result

Queries:

- read only
- no transactional write behavior
- use dedicated read models where appropriate

### 3. No Business Decisions In Controllers

Controllers may:

- parse HTTP input
- delegate to use case
- map result to response DTO

Controllers may not:

- validate business rules that belong to the domain
- perform orchestration
- query multiple repositories

### 4. Use Explicit Result Objects

Eliminate internal `Map<String, Object>` use.

Rules:

- every use case should return a dedicated result type
- every query should return dedicated read models
- mapping to generic JSON shape should happen only in the controller if legacy API compatibility still requires it

## Event-Driven Boundaries

To reach fuller DDD, direct cross-context calls should be reduced and replaced where appropriate with domain/application events.

### Recommended First Internal Events

- `ProjectSyncStarted`
- `ProjectSyncCompleted`
- `ImageImported`
- `ImageDetectionCompleted`
- `MilestoneEvaluated`
- `MilestoneDelayed`
- `MetricsRolledUp`
- `AlertRaised`
- `AlertResolved`
- `ProgressReportGenerated`

### How To Introduce Them

Phase 1:

- in-process events only
- Spring application events or a lightweight internal event bus adapter

Phase 2:

- if needed later, promote selected events to integration events

### Event Design Rules

- events must be immutable
- events should use domain language
- event payloads should contain identifiers and key facts, not full object graphs
- events must not contain web DTOs

## Read Model Strategy

Full DDD should not force API queries through aggregates when projections are more appropriate.

### Rule

For dashboards and reporting-style reads, use dedicated read models.

Good fit for read models:

- daily metrics listing
- weekly metrics listing
- activity heatmap
- sync status
- report summaries
- snapshot dates

### Requirements

- locate read models in application or infrastructure by context
- keep them read-only
- do not reuse write aggregates for convenience

## Transaction Strategy

This is required for correctness and Sonar-friendly code.

### Rules

1. Put transactions on application command use cases or persistence adapters where isolation is intentionally narrow.
2. Do not wrap network I/O in long database transactions.
3. Keep query use cases read-only.
4. Use `REQUIRES_NEW` only where isolation is explicitly required and documented.
5. Do not call transactional methods through `this`.
6. Keep transaction scopes small and intention-revealing.

### Context-Specific Guidance

#### Sync

- one high-level sync execution should not be one giant DB transaction
- per-job state updates may be isolated
- per-image persistence may be isolated when failure tolerance is required

#### Detection

- claim phase and persistence phase should be isolated carefully
- external inference call must stay outside long DB transaction

#### Plan

- PDF parsing and OpenAI milestone extraction must stay outside DB transaction where possible
- milestone persistence can be transactional

#### Report

- evidence gathering and OpenAI report generation must stay outside DB transaction
- report persistence should be transactional

#### Metrics

- project-level analysis can be one command use case
- avoid holding a transaction while doing heavy calculation if persistence can be batched more safely

## Testing Strategy

Full DDD requires stronger tests at multiple layers.

### 1. Domain Tests

For each aggregate and value object:

- transitions
- invariants
- edge cases
- invalid operations

These should be pure unit tests without Spring.

### 2. Application Use Case Tests

Mock ports and verify orchestration:

- correct repository calls
- correct event publication
- correct error handling
- correct transaction assumptions

### 3. Infrastructure Adapter Tests

Test:

- JPA mapping correctness
- custom queries
- external client mapping/parsing

Use integration tests for these.

### 4. API Contract Tests

Verify:

- controller mappings
- request validation
- response DTO shapes
- error handling

### 5. End-To-End Pipeline Tests

Minimal but critical:

- sync -> detection -> metrics
- plan upload -> plan check
- report generation

## Sonar And Code Quality Rules

These are mandatory architecture rules, not cleanup extras.

### General Rules

- eliminate dead code immediately after refactors
- no duplicate logic across contexts
- no magic strings for statuses and types
- no long methods with mixed responsibilities
- no deeply nested conditionals when a policy or value object is clearer
- no broad `Exception` catch unless rethrowing with a clear purpose
- no ignored exceptions
- no commented-out code
- no use of `Optional` as fields or DTO properties
- no direct field injection
- no mutable static shared state

### Spring Rules

- use constructor injection only
- do not inject beans that are not needed
- keep controllers thin
- avoid generic `@Service` god classes
- prefer package-private helpers when public visibility is unnecessary
- do not expose entities from controllers

### JPA Rules

- entities should not contain business logic unrelated to persistence representation
- avoid accidental N+1 query behavior
- use explicit repository methods
- document non-trivial native queries

### Lombok Rules

- use `@RequiredArgsConstructor` for components
- use `@Getter`/`@Setter`/`@EqualsAndHashCode`/`@ToString` deliberately
- for entities:
  - `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`
  - `@ToString(onlyExplicitlyIncluded = true)`
  - include only safe identity fields and small business identifiers

### Logging Rules

- use `@Slf4j` only where logs add operational value
- log state transitions and integration boundaries
- do not log secrets or full payloads
- use structured message arguments, not string concatenation

### Readability Rules

- prefer small focused classes
- keep one class on one responsibility
- keep methods short enough to be scannable
- name methods by business intent, not technical implementation
- avoid generic names like `process`, `handle`, `doStuff` unless the context is already explicit

## Packaging And Naming Rules

### 1. Remove Generic Integration Bucket Over Time

Current generic `integration` usage should be phased out into context infrastructure packages.

### 2. Use Context Language

Prefer:

- `PlanIntelligenceGateway`
- `ReportEvidenceImageProvider`
- `DetectionMetricsReadModel`

Avoid:

- `Utils`
- `Helper`
- `Manager`
- `Facade`
- `CommonService`

### 3. Keep Shared Code Truly Shared

Anything in `common` must satisfy all of these:

- used by multiple contexts
- domain-neutral
- not hiding accidental coupling

If it is only used by one context, move it back to that context.

## Migration Phases

### Phase 0: Freeze Public API Shape

Tasks:

- document all current REST contracts
- replace remaining legacy map-heavy API contracts with DTOs
- ensure Swagger is accurate

Done when:

- external API is explicit and stable

### Phase 1: Formalize Bounded Contexts

Tasks:

- define each context in an ADR or architecture note
- document aggregates, invariants, ports, events
- remove generic cross-context utility code

Done when:

- each context has a published ownership model

### Phase 2: Rich Domain Modeling

Tasks:

- move state transitions into aggregates
- add missing enums and value objects
- eliminate primitive obsession in core flows

Done when:

- use cases mostly coordinate rather than decide business rules

### Phase 3: Repository And Adapter Hardening

Tasks:

- move all JPA entities under infrastructure persistence
- ensure repositories operate on domain models only
- eliminate remaining entity leakage

Done when:

- application and domain layers do not reference JPA entities

### Phase 4: CQRS-Lite Separation

Tasks:

- formalize query models
- separate read and write packages consistently
- remove write-side reuse for read concerns

Done when:

- reads no longer distort aggregate design

### Phase 5: Event-Driven Cross-Context Integration

Tasks:

- introduce internal domain/application events
- move cross-context reactions behind handlers
- reduce direct orchestration coupling

Done when:

- core workflows no longer require direct service-to-service context coupling for every step

### Phase 6: Test Hardening

Tasks:

- add domain tests
- add use case tests
- add integration tests for adapters
- add contract tests for controllers

Done when:

- critical domain behavior is protected independently of Spring wiring

### Phase 7: Sonar And Maintainability Pass

Tasks:

- remove duplication
- reduce complexity hotspots
- tighten visibility
- remove stale helpers and transitional adapters
- review logs, exception handling, validation, naming

Done when:

- no architectural debt remains from the migration itself

## Recommended Execution Order

Use this order:

1. HTTP boundary cleanup
2. formal bounded-context ADRs
3. project aggregate hardening
4. sync aggregate hardening
5. detection aggregate hardening
6. plan aggregate hardening
7. report aggregate hardening
8. metrics/alert policy extraction
9. internal events
10. test hardening
11. Sonar cleanup pass

Reason:

- `project`, `sync`, and `detection` are the operational core
- `plan` and `report` depend on them
- `metrics` and `alert` should stabilize after the operational data flow is clearly modeled

## Concrete Deliverables By Area

### Architecture

- ADR for each bounded context
- aggregate and invariant catalog
- event catalog
- transaction boundary catalog

### Code

- no remaining generic façade services
- no remaining domain-layer dependency on web or JPA
- no remaining weakly typed core application flows
- no remaining persistence entities outside infrastructure

### Quality

- Sonar warning baseline reviewed and tracked
- domain test coverage added for critical aggregates
- package rules enforced in code review

## Definition Of Done

The system can be considered fully DDD-oriented when:

- all major business rules are owned by domain models or domain services
- aggregate roots and boundaries are explicit
- repositories operate on domain models, not JPA entities
- HTTP DTOs remain outside the domain
- external systems are behind ports
- read models are separated from write models where needed
- cross-context behavior uses events or well-defined application orchestration
- transaction boundaries are explicit and intentional
- code passes compile, tests, and quality checks without architectural exceptions

## Immediate Next Step

The most useful next artifact after this plan is not code. It is a short architecture note per context documenting:

- aggregate root
- child entities
- invariants
- commands
- queries
- events
- repository port
- external ports

Start with:

1. `project`
2. `sync`
3. `detection`

These three contexts define the backbone of the system. If they are modeled correctly, the rest of the codebase becomes much easier to bring to full DDD.
