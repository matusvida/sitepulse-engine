# SitePulse Engine DDD Refactor Plan

## Goal

Refactor `sitepulse-engine` from a service-heavy Spring Boot application into a DDD-lite architecture with:

- explicit bounded contexts
- cleaner separation of domain, application, and infrastructure
- domain-led business rules instead of controller/service drift
- stable HTTP contracts kept in `sitepulse-engine-http-api`

This is not a rewrite. The goal is controlled, incremental refactoring while preserving behavior.

## Guiding Principles

1. Keep the existing module split:
   - `sitepulse-engine-http-api`
   - `sitepulse-engine-app`
2. Treat the HTTP module as an external contract, not the core domain.
3. Move toward DDD-lite, not academic DDD.
4. Refactor feature by feature, not package by package.
5. Prefer explicit use cases and aggregates over generic utility services.
6. Keep infrastructure concerns out of the domain model.

## Target Architecture

Use this shape inside `sitepulse-engine-app`:

```text
com.sitepulse.engine
|-- common
|   |-- domain
|   |-- application
|   `-- infrastructure
|-- project
|   |-- domain
|   |-- application
|   `-- infrastructure
|-- sync
|   |-- domain
|   |-- application
|   `-- infrastructure
|-- detection
|   |-- domain
|   |-- application
|   `-- infrastructure
|-- analysis
|   |-- domain
|   |-- application
|   `-- infrastructure
|-- planning
|   |-- domain
|   |-- application
|   `-- infrastructure
|-- reporting
|   |-- domain
|   |-- application
|   `-- infrastructure
|-- integration
|   `-- infrastructure
|-- scheduler
|-- config
`-- boot
```

Notes:

- `web` stays outside the core and should become thin adapters.
- JPA repositories, Feign clients, MinIO, Dropbox, OpenAI, PDF parsing belong in `infrastructure`.
- HTTP DTOs stay in `sitepulse-engine-http-api`.

## Proposed Bounded Contexts

### 1. Project Context

Responsibilities:

- projects
- cameras
- project configuration
- ROI and camera association rules

Core concepts:

- `Project`
- `Camera`
- `ProjectSettings`

### 2. Sync Context

Responsibilities:

- Dropbox traversal
- sync job lifecycle
- image registration
- raw image ingestion into object storage

Core concepts:

- `SyncJob`
- `SyncBatch`
- `ImageImport`
- `SyncFailure`

### 3. Detection Context

Responsibilities:

- image processing lifecycle
- YOLO inference orchestration
- detection persistence
- ROI filtering and quality checks

Core concepts:

- `Image`
- `DetectionResult`
- `Detection`
- `ImageStatus`

### 4. Analysis Context

Responsibilities:

- daily metrics
- weekly metrics
- alert generation
- activity heatmaps

Core concepts:

- `DailyMetrics`
- `WeeklyMetrics`
- `ActivitySummary`
- `Alert`

### 5. Planning Context

Responsibilities:

- plan upload
- milestone tracking
- milestone assessment
- schedule risk detection

Core concepts:

- `ConstructionPlan`
- `Milestone`
- `MilestoneAssessment`

### 6. Reporting Context

Responsibilities:

- progress reports
- report generation requests
- report persistence

Core concepts:

- `ProgressReport`
- `ReportContent`
- `ReportPeriod`

## Explicit Non-Domain Areas

These should remain infrastructure, not domain:

- MinIO object storage
- Dropbox API
- OpenAI API
- YOLO Python service
- PDF extraction
- Flyway
- Spring MVC controllers
- Feign clients

## Current Problems To Fix

### 1. Application services own too much logic

Examples:

- sync orchestration
- state transitions
- DTO mapping
- integration coordination
- persistence decisions

### 2. Persistence models are acting as domain models

Current entities are mostly JPA state containers. They do not enforce domain invariants or lifecycle rules.

### 3. Too many weakly typed responses

There are still many `Map<String, Object>` payloads in application and controller flows. These obscure intent and make refactoring risky.

### 4. Cross-layer leakage

Application services depend directly on:

- HTTP DTOs
- Spring Data repositories
- infrastructure clients

### 5. Domain state is still string-heavy

You already improved `ImageStatus`. The same should happen for:

- sync job status
- alert status
- alert severity
- milestone status
- report type

## Refactor Rules

Apply these consistently:

1. Controllers implement HTTP API interfaces and delegate to application use cases only.
2. Application layer coordinates use cases and transaction boundaries.
3. Domain layer contains business decisions, invariants, state transitions, and value objects.
4. Infrastructure layer implements persistence and external integrations.
5. Domain and application must not depend on Spring MVC or Feign.
6. Domain should not depend on MinIO, Dropbox, OpenAI, or YOLO clients.
7. Avoid returning JPA entities from controller methods.
8. Avoid `Map<String, Object>` in new code.

## Target Layer Responsibilities

### Domain

Contains:

- aggregates
- entities with behavior
- value objects
- enums
- domain services
- domain events
- repository interfaces

Examples:

- `Image.markProcessing()`
- `Image.markDetected(...)`
- `SyncJob.finishSuccess(...)`
- `Milestone.markDelayed(...)`

### Application

Contains:

- use cases
- transaction orchestration
- coordination across repositories and infrastructure ports
- mapping from domain results to output DTOs

Examples:

- `TriggerProjectSyncUseCase`
- `ProcessPendingImagesUseCase`
- `GenerateProjectReportUseCase`
- `RunPlanCheckUseCase`

### Infrastructure

Contains:

- JPA repositories
- entity mappings
- external API clients
- storage adapters
- Flyway
- event publisher adapters

Examples:

- `JpaProjectRepository`
- `MinioObjectStorage`
- `DropboxSyncSource`
- `OpenAiReportGenerator`
- `YoloDetectionGateway`

## Recommended Package Pattern Per Context

Use this pattern for each context:

```text
<context>/
|-- domain/
|   |-- model/
|   |-- service/
|   |-- event/
|   `-- port/
|-- application/
|   |-- usecase/
|   |-- command/
|   |-- query/
|   `-- result/
`-- infrastructure/
    |-- persistence/
    |-- external/
    `-- mapper/
```

Example:

```text
sync/
|-- domain/
|   |-- model/SyncJob.java
|   |-- model/SyncJobStatus.java
|   |-- model/ImageImport.java
|   `-- port/SyncJobRepository.java
|-- application/
|   |-- usecase/TriggerProjectSyncUseCase.java
|   |-- usecase/RunProjectSyncUseCase.java
|   `-- result/SyncStatusResult.java
`-- infrastructure/
    |-- persistence/JpaSyncJobRepository.java
    `-- external/DropboxSyncSource.java
```

## Recommended Refactor Order

Do the refactor in this order:

### Phase 1: Stabilize Contracts

1. Keep `sitepulse-engine-http-api` stable.
2. Replace remaining controller/entity leaks with API DTOs.
3. Reduce `Map<String, Object>` in controller responses.

Definition of done:

- HTTP boundary is stable and typed.

### Phase 2: Refactor Sync Context

Why first:

- it is central to ingestion
- it has clear workflow state
- it already has transaction issues and orchestration complexity

Tasks:

1. Introduce domain model:
   - `SyncJob`
   - `SyncJobStatus`
   - `ImageImport`
2. Introduce ports:
   - `SyncJobRepository`
   - `ImageCatalogRepository`
   - `SyncSource`
   - `ObjectStorage`
3. Move Dropbox integration behind a `SyncSource` port.
4. Move MinIO behind an `ObjectStorage` port.
5. Replace `SyncProjectExecutor` with use cases:
   - `TriggerProjectSyncUseCase`
   - `RunProjectSyncUseCase`
6. Keep each image save isolated in its own transaction where required.
7. Replace string sync statuses with enum.

Definition of done:

- sync use cases depend on ports, not concrete Dropbox/MinIO/JPA classes
- sync job lifecycle is modeled explicitly

### Phase 3: Refactor Detection Context

Tasks:

1. Model `Image` lifecycle explicitly.
2. Keep `ImageStatus` as an enum and move all transitions into domain behavior.
3. Introduce ports:
   - `ImageRepository`
   - `DetectionRepository`
   - `DetectionGateway`
   - `ObjectStorage`
4. Move quality filtering and ROI logic closer to domain or domain service.
5. Replace generic persistence logic in `DetectionService` with:
   - `RunOnDemandDetectionUseCase`
   - `ProcessPendingImagesUseCase`

Definition of done:

- detection workflow is use-case driven
- domain owns image lifecycle transitions

### Phase 4: Refactor Project Context

Tasks:

1. Make `Project` and `Camera` the core aggregate/child relationship.
2. Move camera matching and ROI rules into domain services where appropriate.
3. Introduce repository ports for projects and cameras.
4. Keep project application services focused on use cases:
   - create/update project
   - create/update camera
   - get project details

Definition of done:

- project rules live in project context, not scattered across detection/sync

### Phase 5: Refactor Planning Context

Tasks:

1. Model `ConstructionPlan` and `Milestone` explicitly.
2. Introduce `MilestoneStatus` enum.
3. Move milestone assessment rules out of controller-style map handling.
4. Introduce ports for:
   - plan repository
   - milestone repository
   - plan parsing
   - milestone evaluator

Definition of done:

- milestone lifecycle is typed and explicit

### Phase 6: Refactor Reporting Context

Tasks:

1. Model `ProgressReport` and `ReportPeriod`.
2. Introduce typed application results instead of map payloads.
3. Hide OpenAI behind a reporting port.
4. Keep report generation orchestration in one application use case.

Definition of done:

- reporting is independent from HTTP and infrastructure details

### Phase 7: Refactor Analysis Context

Tasks:

1. Split alert logic from metrics logic if needed.
2. Introduce enums for alert status/severity/type where meaningful.
3. Move alert creation/resolution rules into domain/application logic.
4. Replace raw row maps with typed query results for new code.

Definition of done:

- analysis becomes a coherent context rather than a bag of metric helpers

## Concrete Code Changes To Aim For

### Replace Generic Services With Use Cases

Examples:

- `SyncService` -> `TriggerProjectSyncUseCase`, `GetProjectSyncStatusQuery`
- `DetectionService` -> `RunOnDemandDetectionUseCase`, `ProcessPendingImagesUseCase`
- `ProjectService` -> `CreateProjectUseCase`, `UpdateProjectUseCase`, `ListProjectCamerasQuery`
- `PlanService` -> `UploadPlanUseCase`, `UpdateMilestoneUseCase`, `RunPlanCheckUseCase`
- `ReportService` -> `GenerateProjectReportUseCase`, `ListReportsQuery`

### Introduce Ports

Examples:

- `ObjectStorage`
- `DropboxSource`
- `YoloDetector`
- `ReportGenerator`
- `PlanTextExtractor`
- `ProjectRepository`
- `ImageRepository`

### Move Adapters To Infrastructure

Examples:

- current `StorageService` becomes an adapter implementing `ObjectStorage`
- current Dropbox client becomes an adapter implementing `DropboxSource`
- current OpenAI service becomes adapters for planning/reporting use cases

## Transaction Strategy

Use these rules:

1. Transaction boundaries belong in application use cases.
2. Do not wrap long-running external I/O in a single DB transaction.
3. Keep external calls outside aggregate mutation when practical.
4. Use separate transactions for:
   - sync job state updates
   - per-image persistence during sync if failure isolation is needed
   - batch detection claiming/processing where locking matters

## Domain Events

Introduce internal domain/application events only where useful.

Recommended first events:

- `ProjectSyncStarted`
- `ProjectSyncFinished`
- `ImageImported`
- `ImageDetectionCompleted`
- `MilestoneMarkedDelayed`
- `ReportGenerated`

At first, publish them in-process only.

## Value Objects And Enums To Add

Add these incrementally:

- `SyncJobStatus`
- `AlertStatus`
- `AlertSeverity`
- `MilestoneStatus`
- `ReportType`
- `StorageObjectRef`
- `DateRange`

Use value objects only where they clarify rules or remove ambiguity.

## Things To Avoid

1. Do not rename packages without moving logic.
2. Do not turn simple CRUD into fake domain complexity.
3. Do not expose JPA entities as public API.
4. Do not let domain classes call Feign/MinIO/Dropbox directly.
5. Do not keep `Map<String, Object>` for new application flows.
6. Do not try to complete all contexts in one branch.

## Suggested First Milestone

Start with the `sync` context.

Deliverables:

1. `SyncJobStatus` enum
2. `SyncJob` domain model with explicit lifecycle methods
3. `ObjectStorage` port
4. `DropboxSource` port
5. `TriggerProjectSyncUseCase`
6. `RunProjectSyncUseCase`
7. JPA and Dropbox/MinIO adapters
8. typed sync status result instead of raw map logic

Reason:

- high business value
- clear workflow
- lowest ambiguity
- sets the pattern for the rest of the app

## Definition Of Done

The DDD refactor is successful when:

- HTTP contracts stay stable in `sitepulse-engine-http-api`
- app code is organized by bounded context
- use cases replace generic orchestration services
- domain rules are explicit and typed
- infrastructure is behind ports
- JPA entities are no longer the primary business model
- public API no longer leaks entities or weakly typed structures
- transaction boundaries are intentional and use-case driven

## Recommended Next Step

Implement the first milestone for `sync` only, then compile, run, and verify behavior before touching `detection` or `project`.
