# sitepulse-engine

Backend for the SitePulse construction monitoring platform, built with Domain-Driven Design.

## Modules

| Module | Purpose |
|--------|---------|
| `sitepulse-engine-http-api` | Public HTTP contracts — API interfaces, request DTOs, response DTOs |
| `sitepulse-engine-app` | Runnable Spring Boot application — domain models, use cases, persistence, schedulers, integrations |
| `python-yolo` | Optional local Python FastAPI service used for YOLO object-detection inference |

Spring Boot is the public backend. The Python service is an internal dependency, not a public API.

## Architecture

```text
Dropbox
  → Spring sync pipeline
  → Cloudflare R2 raw image storage in production
  → MinIO raw image storage in local development
  → image registration in PostgreSQL
  → scheduled/manual detection via OpenAI
  → optional local YOLO inference service for development-only runs
  → detections + metrics + alerts in PostgreSQL
  → plan evaluation + AI reporting via OpenAI
  → REST API + Swagger UI from Spring Boot
```

### Domain-Driven Design

The codebase follows a Ports & Adapters (hexagonal) architecture organized by bounded context. Each context owns its domain model, application use cases, infrastructure adapters, and web controllers.

**Principles:**

- **Bounded Context First** — each context is a top-level package with its own layered structure
- **Domain Owns Behavior** — aggregates enforce invariants through guarded state transitions
- **Application Layer Orchestrates** — use cases coordinate domain objects and ports; they never leak HTTP types
- **Infrastructure Stays Replaceable** — persistence, external services, and schedulers sit behind domain ports
- **CQRS-Lite** — read models are separated from write repositories where queries benefit from it
- **Typed Domain Events** — cross-context communication uses `DomainEvent` published through Spring's `ApplicationEventPublisher`

## Bounded Contexts

| Context | Responsibility |
|---------|---------------|
| `project` | Project and camera CRUD, snapshot retrieval |
| `sync` | Dropbox ingestion, image import into object storage, sync job tracking |
| `detection` | Object detection orchestration, OpenAI primary path, optional YOLO gateway |
| `metrics` | Daily/weekly metric aggregation, activity heatmaps |
| `alert` | Alert creation, acknowledgment, resolution |
| `plan` | Construction plan upload, milestone parsing, plan-vs-progress checking |
| `report` | AI-powered progress report generation |
| `visualization` | Detection overlay rendering |
| `common` | Shared kernel — `ObjectStorage` port, `DomainEvent` marker, `DomainEventPublisher`, exception hierarchy |
| `config` | Spring configuration beans (`SitePulseProperties`, `ShedLockConfig`, `CorsConfig`) |
| `root` | Health/info root endpoint |

## Package Structure Per Context

Each bounded context follows this layered package structure:

```text
{context}/
├── domain/
│   ├── model/        Aggregates, entities, value objects, enums
│   ├── port/         Repository interfaces, read model interfaces, gateway interfaces
│   ├── service/      Domain services (pure business logic, no framework dependencies)
│   ├── policy/       Domain policies (classification, detection rules)
│   └── event/        Domain event classes
├── application/
│   ├── usecase/      Use cases (commands) and queries
│   ├── command/      Command records (input to use cases)
│   └── result/       Result records (output from use cases, never HTTP DTOs)
├── infrastructure/
│   ├── persistence/  JPA entities, Spring Data repositories, adapter implementations
│   ├── external/     External service adapters (Dropbox, object storage, YOLO, OpenAI)
│   ├── scheduler/    ShedLock-backed scheduled tasks
│   └── event/        Spring event listeners
└── web/
    └── Controller    Implements the HTTP API interface, maps HTTP ↔ application types
```

Not every context uses every sub-package. For example, `visualization` has no domain layer, and only `metrics` has `domain/policy/`.

## End-to-End Pipeline

### 1. Project Setup

- A project is created via the REST API with a name, location, and optional `storageKeyPrefix`
- Cameras are assigned to the project, each with a camera-specific `dropboxPath`, optional `keyPrefix`, ROI polygon, and `dropOutside` flag

### 2. Dropbox Sync

**Key classes:** `RunProjectSyncUseCase`, `RunScheduledSyncUseCase`, `SyncFileParser`, `SyncJob`

1. Scheduler or user triggers a project sync
2. `SyncJob.start()` creates a RUNNING job
3. `SyncSource` (Dropbox adapter) lists date folders and image files per camera Dropbox path
4. `SyncFileParser` extracts capture timestamps from folder/file names
5. Each image is downloaded from Dropbox and uploaded to object storage via `ObjectStorage`
6. Stored object keys are built as `{project.storageKeyPrefix}/{camera.keyPrefix}/{dateFolder}/{fileName}`
7. An `images` row is registered in PostgreSQL through `ImageCatalogRepository`
8. `SyncJob` tracks found/synced counts; `finish()` determines final DONE or FAILED status
9. `ProjectSyncCompletedEvent` is published

### 3. Detection

**Key classes:** `ProcessPendingImagesUseCase`, `RunOnDemandDetectionUseCase`, `DetectionImage`, `DetectionPostProcessor`

1. `DetectionImage` transitions: `NEW → PROCESSING → DONE | FAILED`
2. Spring claims pending images and downloads bytes from object storage
3. Images are sent to OpenAI for primary detection; the Python YOLO service is optional and local-only
4. `DetectionPostProcessor` applies confidence thresholds, min box area, ROI filtering, and quality warnings
5. Detections are persisted; image status is updated
6. `ImageDetectionCompletedEvent` is published

### 4. Metrics and Alerts

**Key classes:** `RunProjectAnalysisUseCase`, `DailyActivityAggregator`, `WeeklyRollupCalculator`, `RiskClassificationPolicy`, `StallDetectionPolicy`, `DeclineDetectionPolicy`

1. Detection activity samples are loaded from the read model
2. `DailyActivityAggregator` computes people/vehicle counts and active hours per day
3. `WeeklyRollupCalculator` computes progress deltas, activity indices, and risk levels per week
4. `StallDetectionPolicy` checks for consecutive low-activity days → raises `stall` alerts
5. `DeclineDetectionPolicy` checks for consecutive negative-progress weeks → raises `schedule` alerts
6. Alerts auto-resolve when conditions clear

### 5. Construction Plan Tracking

**Key classes:** `UploadConstructionPlanUseCase`, `EvaluateMilestoneUseCase`, `ConstructionPlan`, `PlanMilestone`

1. User uploads a construction plan PDF
2. `PlanDocumentTextExtractor` extracts raw text
3. `PlanIntelligenceGateway` (OpenAI) parses milestones from the text
4. `ConstructionPlan` transitions: `PROCESSING → READY | FAILED`
5. Milestone checks compare site images against expected progress
6. `PlanMilestone.applyAssessment()` updates status; delayed milestones generate alerts

### 6. Reporting

**Key classes:** `GenerateProgressReportUseCase`, `ReportContextProvider`, `ReportGenerator`

1. `ReportEvidenceImageProvider` gathers sample images from object storage
2. `ReportContextProvider` assembles metrics and milestone summaries (decoupled from other contexts)
3. `ReportGenerator` (OpenAI) produces Markdown content
4. `ProgressReport.create()` validates content and date ranges
5. `ProgressReportGeneratedEvent` is published

### 7. Visualization

**Key classes:** `GenerateDetectionVisualizationUseCase`

1. Original images are loaded from object storage
2. Persisted detections are loaded from PostgreSQL
3. Bounding boxes are rendered onto images
4. Visualizations are uploaded back to object storage

## HTTP API

All API interfaces live in the `sitepulse-engine-http-api` module. Each bounded context has its own interface and controller.

| Interface | Controller | Base Path | Endpoints |
|-----------|-----------|-----------|-----------|
| `ProjectApi` | `ProjectController` | `/api` | Project/camera CRUD, snapshots |
| `SyncApi` | `SyncController` | `/api` | Sync status, trigger sync |
| `DetectionApi` | `DetectionController` | — | Health check, on-demand detection |
| `MetricsApi` | `MetricsController` | `/api` | Daily/weekly metrics, generate, heatmap |
| `AlertApi` | `AlertController` | `/api` | List alerts, update alert status |
| `PlanApi` | `PlanController` | `/api` | Plan upload, milestones, plan checks |
| `ReportApi` | `ReportController` | `/api` | Generate report, list reports, report detail |
| `VisualizationApi` | `VisualizationController` | `/api` | Generate detection visualizations |
| `RootApi` | `RootController` | `/` | App info |

## Domain Events

Events implement the `DomainEvent` marker interface and are published through `DomainEventPublisher` (backed by Spring's `ApplicationEventPublisher`).

| Event | Context | Triggered When |
|-------|---------|---------------|
| `ProjectSyncStartedEvent` | sync | A sync job begins |
| `ProjectSyncCompletedEvent` | sync | A sync job finishes |
| `ImageDetectionCompletedEvent` | detection | An image completes detection |
| `MetricsRolledUpEvent` | metrics | Daily/weekly metrics are computed |
| `AlertRaisedEvent` | alert | A new alert is created |
| `AlertResolvedEvent` | alert | An alert is auto-resolved |
| `MilestoneEvaluatedEvent` | plan | A milestone assessment is applied |
| `MilestoneDelayedEvent` | plan | A milestone is marked delayed |
| `ProgressReportGeneratedEvent` | report | A report is generated |

## Value Objects

| Value Object | Context | Invariant |
|-------------|---------|-----------|
| `BoundingBox` | detection | Exactly 4 coordinates, immutable |
| `Confidence` | detection | Value between 0.0 and 1.0 |
| `RoiPolygon` | project | At least 3 points |
| `DropboxPath` | project | Must not be blank |
| `StorageObjectRef` | sync | Non-blank bucket and key |
| `ReportPeriod` | report | Start date not after end date |
| `RiskLevel` | metrics | Enum: LOW, MEDIUM, HIGH |
| `ReportType` | report | Enum: CUSTOM, WEEKLY, DAILY |

## Domain Policies

| Policy | Context | Logic |
|--------|---------|-------|
| `RiskClassificationPolicy` | metrics | Classifies risk based on activity drop vs rolling average |
| `StallDetectionPolicy` | metrics | Detects 3+ consecutive low-activity days |
| `DeclineDetectionPolicy` | metrics | Detects 2+ consecutive negative-progress weeks |

## Project Layout

```text
sitepulse-engine/
├── pom.xml                          Parent POM (Java 25, Spring Boot 3.4.4)
├── Dockerfile
├── docker-compose.yml
├── sitepulse-engine-http-api/
│   ├── pom.xml
│   └── src/main/java/com/sitepulse/engine/http/
│       ├── project/   api/ dto/
│       ├── sync/      api/
│       ├── detection/  api/ dto/
│       ├── metrics/   api/ dto/
│       ├── alert/     api/ dto/
│       ├── plan/      api/ dto/
│       ├── report/    api/ dto/
│       ├── visualization/ api/ dto/
│       ├── root/      api/ dto/
│       └── common/    dto/
├── sitepulse-engine-app/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/sitepulse/engine/
│       │   │   ├── project/       domain/ application/ infrastructure/ web/
│       │   │   ├── sync/          domain/ application/ infrastructure/ web/
│       │   │   ├── detection/     domain/ application/ infrastructure/ web/
│       │   │   ├── metrics/       domain/ application/ infrastructure/ web/
│       │   │   ├── alert/         domain/ application/ infrastructure/ web/
│       │   │   ├── plan/          domain/ application/ infrastructure/ web/
│       │   │   ├── report/        domain/ application/ infrastructure/ web/
│       │   │   ├── visualization/ application/ web/
│       │   │   ├── common/        domain/ application/ infrastructure/ web/
│       │   │   ├── config/
│       │   │   └── root/          web/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-development.yml
│       │       └── db/migration/
│       └── test/java/com/sitepulse/engine/
│           ├── sync/domain/       SyncJobTest, SyncFileParserTest
│           ├── detection/domain/  DetectionImageTest, BoundingBoxTest
│           ├── alert/domain/      AlertTest
│           ├── plan/domain/       ConstructionPlanTest, PlanMilestoneTest
│           └── metrics/domain/    RiskClassificationPolicyTest, StallDetectionPolicyTest
└── python-yolo/
    ├── Dockerfile
    ├── requirements.txt
    └── app/main.py
```

## Tech Stack

### Spring Application

- **Java 25** with virtual threads enabled
- **Spring Boot 3.4.4**
- Spring Web, Spring Data JPA (Hibernate), Spring AOP
- Flyway for database migrations
- OpenFeign for HTTP clients
- springdoc OpenAPI 2.8.6 / Swagger UI
- PostgreSQL
- Cloudflare R2 for production object storage, MinIO for local development
- ShedLock 5.16.0 for distributed scheduler locking
- Lombok 1.18.44

### YOLO Service

- Python 3.12
- FastAPI + Uvicorn
- Ultralytics YOLO
- OpenCV, NumPy

## Running Locally

### Option 1: Development mode (recommended)

Run infrastructure in Docker, Spring locally.

1. Set Java 25:
   ```powershell
   $env:JAVA_HOME='C:\Users\matus\.jdks\openjdk-25.0.1'
   $env:Path="$env:JAVA_HOME\bin;$env:Path"
   ```
2. Start infrastructure and the optional local YOLO service:
   ```powershell
   docker compose up -d postgres minio python-yolo
   ```
3. Run Spring from the app module:
   ```powershell
   cd sitepulse-engine-app
   mvn spring-boot:run "-Dspring-boot.run.profiles=development"
   ```

   Or from the repo root:
   ```powershell
   mvn -pl sitepulse-engine-app spring-boot:run "-Dspring-boot.run.profiles=development"
   ```

### Option 2: Full Docker Compose

```powershell
docker compose up --build
```

## Local Endpoints

| Service | URL |
|---------|-----|
| Spring API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Optional local YOLO health | `http://localhost:8000/health` |
| MinIO API | `http://localhost:9001` |
| MinIO console | `http://localhost:9091` |
| PostgreSQL | `localhost:5432` |

## Configuration

Main configuration files:

- `sitepulse-engine-app/src/main/resources/application.yml`
- `sitepulse-engine-app/src/main/resources/application-development.yml`

### Environment Variables

| Variable | Purpose |
|----------|---------|
| `POSTGRES_DSN` | PostgreSQL JDBC URL |
| `STORAGE_PROVIDER` | Storage backend provider (`minio`, `r2`, or `s3`) |
| `STORAGE_DEFAULT_BUCKET` | Default object storage bucket |
| `STORAGE_PRESIGN_TTL_MINUTES` | Signed URL expiration in minutes |
| `STORAGE_ENDPOINT` | Storage endpoint URL |
| `STORAGE_PUBLIC_ENDPOINT` | Storage public endpoint used for browser-accessible signed URLs |
| `STORAGE_ACCESS_KEY` | Storage access key |
| `STORAGE_SECRET_KEY` | Storage secret key |
| `STORAGE_REGION` | Storage region, use `auto` for R2 |
| `DROPBOX_TOKEN` | Dropbox OAuth token |
| `DROPBOX_APP_KEY` | Dropbox app key |
| `DROPBOX_APP_SECRET` | Dropbox app secret |
| `DROPBOX_REFRESH_TOKEN` | Dropbox refresh token |
| `OPENAI_API_KEY` | OpenAI API key |
| `OPENAI_MODEL` | OpenAI model name |
| `PYTHON_YOLO_BASE_URL` | Optional URL of the local-only Python YOLO service |
| `SYNC_CRON` | Cron expression for Dropbox sync (default: every 10 min) |
| `DETECTION_SWEEP_CRON` | Cron expression for detection sweep (default: every 10 min, offset 5) |
| `ANALYSIS_CRON` | Cron expression for nightly analysis (default: 2 AM UTC) |
| `CORS_ORIGINS` | Allowed CORS origins |

### Scheduled Tasks

| Scheduler | Cron Property | Lock Name | Purpose |
|-----------|--------------|-----------|---------|
| `SyncScheduler` | `sitepulse.sync-cron` | `dropboxSyncJob` | Periodic Dropbox sync |
| `DetectionScheduler` | `sitepulse.detection-sweep-cron` | `detectionSweepJob` | Process pending images |
| `MetricsScheduler` | `sitepulse.analysis-cron` | `nightlyAnalysisJob` | Nightly metrics rollup + alerts |

All schedulers use ShedLock for distributed locking.

## Database and Flyway

Flyway is the only migration tool. Migrations live in `src/main/resources/db/migration/`.

- Empty database: Flyway runs from the baseline migration
- Existing database: Flyway baselining adopts the schema and continues

New schema changes must be added as Flyway migrations (`V2__...sql`, `V3__...sql`, etc.).

## Testing

Domain unit tests cover aggregate state machines, value object validation, domain services, and policies. Tests are pure JUnit 5 with no Spring context.

| Test Class | What It Covers |
|-----------|---------------|
| `SyncJobTest` | SyncJob lifecycle — start, record, finish, error summary (14 tests) |
| `AlertTest` | Alert state machine — create, acknowledge, resolve (9 tests) |
| `DetectionImageTest` | Image status transitions — NEW → PROCESSING → DONE/FAILED (8 tests) |
| `ConstructionPlanTest` | Plan lifecycle — upload, markReady, markFailed (5 tests) |
| `PlanMilestoneTest` | Milestone assessment and detail updates (4 tests) |
| `RiskClassificationPolicyTest` | Risk classification thresholds (5 tests) |
| `StallDetectionPolicyTest` | Consecutive low-activity day detection (3 tests) |
| `BoundingBoxTest` | Bounding box validation and immutability (4 tests) |
| `SyncFileParserTest` | Date folder parsing, timestamp extraction, content type (8 tests) |

Run tests:

```powershell
mvn -pl sitepulse-engine-app test
```

## Development Guidelines

- **Controllers are thin** — they map HTTP types to application commands/results and back
- **Use cases never see HTTP types** — they accept command records and return result records
- **Domain models enforce their own invariants** — guarded state transitions, validated construction
- **Cross-context communication uses domain events or ports** — never direct JPA repository imports across contexts
- **Read models are separate from write repositories** — query-optimized adapters implement read model ports
- **New code should include domain unit tests** — test aggregate behavior, not infrastructure wiring
