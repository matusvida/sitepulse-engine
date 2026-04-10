# SitePulse Engine Spring Boot Migration Plan

## Goal

Move all REST API, database access, scheduling, storage integration, Dropbox sync, reporting, plan tracking, and business logic from Python to Spring Boot. Keep Python only as an internal YOLO inference service.

## Migration Principles

1. Reach feature parity before redesigning the data model.
2. Keep the current HTTP contract stable for the frontend in phase 1.
3. Reuse the current PostgreSQL schema first, then refactor later if needed.
4. Make Spring Boot the only public backend. The Python service should be internal-only.
5. Remove Python dependencies on Postgres, Dropbox, OpenAI, APScheduler, and MinIO business flows.

## Recommended Target Architecture

### Spring Boot app

- Java 25
- Spring Boot 3.x
- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-actuator`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-scheduling`
- `spring-boot-starter-aop`
- `spring-cloud-starter-openfeign`
- Flyway for migrations
- Hibernate as the JPA provider
- Lombok
- `springdoc-openapi` for API docs
- MinIO Java SDK
- Dropbox Java SDK or direct HTTP client wrapper
- Feign clients for third-party APIs, including OpenAI/ChatGPT
- Apache PDFBox for PDF text extraction
- ShedLock for single-instance scheduled jobs if multiple app instances are expected

### Python app

- Keep only model loading and inference
- Expose internal endpoints such as:
  - `GET /health`
  - `POST /infer`
- Input should be raw image bytes or multipart upload
- Output should be raw detections plus metadata only
- No DB writes
- No scheduler
- No Dropbox access
- No MinIO business logic
- No OpenAI calls

## Recommended Repository Layout

```text
sitepulse-engine/
  spring-app/
    src/main/java/com/sitepulse/engine/
    src/main/resources/
  python-yolo/
    app/
    requirements.txt
  docker-compose.yml
```

## Engineering Constraints

- Prefer clean architecture with clear boundaries between controller, application/service, domain, infrastructure, and integration layers.
- Optimize for readability and maintainability over cleverness.
- Keep methods short, responsibilities narrow, and names explicit.
- Avoid god services, anemic package dumps, and mixed transport/business/persistence concerns.
- Use appropriate packaging by feature and layer, not a flat util-heavy structure.
- Expose the public REST API with Swagger/OpenAPI.
- Use Feign clients for third-party integrations such as OpenAI and Dropbox-facing HTTP integrations where appropriate.
- Use Lombok selectively and consistently.
- For JPA entities:
  - use Lombok annotations intentionally
  - keep `@ToString` limited to relevant scalar fields only
  - keep `@EqualsAndHashCode` limited to stable identity fields only
  - do not include large associations, blobs, or mutable collections in `toString`, `equals`, or `hashCode`

## Current Python Responsibilities To Move

Move these Python areas into Spring Boot:

- `app/api/*` -> REST controllers
- `app/db/*` -> JDBC repositories
- `app/services/sync.py` -> Dropbox sync service
- `app/services/storage.py` -> object storage adapter
- `app/services/analysis.py` -> metrics and alerts services
- `app/services/plan_tracker.py` -> weekly plan evaluation service
- `app/services/llm.py` -> OpenAI client and prompt services
- `app/services/pdf_parser.py` -> PDF extraction service
- `app/services/visualize.py` -> annotated image generation service
- `app/worker/*` -> scheduled jobs and async workers
- `app/main.py` -> Spring Boot app bootstrap

Keep in Python only the YOLO-specific logic currently in:

- `app/detection/model.py`

Move these out of Python as well:

- `app/detection/postprocess.py`
- `app/detection/quality.py`
- `app/detection/schemas.py`

## Phase Plan

### Phase 0: Freeze and map the existing behavior

- List every existing endpoint and response shape.
- Capture the current env vars and runtime dependencies from `.env`, `app/core/settings.py`, `Dockerfile`, and `docker-compose.yml`.
- Record current table/column semantics from `app/db/tables.py` and Alembic migrations.
- Note compatibility quirks that must stay initially:
  - camelCase JSON responses
  - `projectId` often serialized as string
  - `images.status` values like `NEW`, `DONE`, `FAILED`
  - `detections.bbox_xyxy` stored as JSON text
  - `detections.in_roi` stored as string-like value

### Phase 1: Bootstrap the Spring Boot app

- Create `spring-app` with package structure:
  - `common`
  - `config`
  - `project`
  - `detection`
  - `sync`
  - `metrics`
  - `alert`
  - `plan`
  - `report`
  - `visualization`
  - `integration`
  - `scheduler`
- Within each feature package, separate by responsibility as needed:
  - `web`
  - `application`
  - `domain`
  - `persistence`
  - `integration`
- Add centralized configuration properties mirroring the current env vars.
- Add structured logging, global exception handling, validation error mapping, and actuator health endpoints.
- Enable OpenAPI/Swagger UI.
- Enable Feign clients.
- Configure Flyway.
- Baseline Flyway against the current schema at Alembic revision `002` instead of changing the schema immediately.

### Phase 2: Build infrastructure adapters in Java

- Implement Postgres persistence with Spring Data JPA + Hibernate.
- Model entities carefully against the existing schema and avoid leaking entities directly to API contracts.
- Add DTOs/mappers so REST payloads stay decoupled from persistence models.
- Implement MinIO client for download, upload, existence checks, and optional presign support.
- Implement Dropbox client for shared-link traversal, folder listing, and file download.
- Implement Feign clients for:
  - OpenAI/ChatGPT
  - Python YOLO service
  - any additional HTTP-based third-party integrations
- Implement OpenAI integration for:
  - plan milestone extraction
  - progress report generation
  - milestone evaluation
- Implement PDF extraction using PDFBox.
- Implement resilient outbound client behavior:
  - timeouts
  - retries where appropriate
  - error mapping
  - request/response logging with sensitive data excluded

### Phase 3: Define the Python inference contract

- Replace the current Python API with a minimal inference service.
- Suggested response payload for `POST /infer`:
  - `modelVersion`
  - `imageWidth`
  - `imageHeight`
  - `inferenceMs`
  - `rawDetections[]`
    - `classId`
    - `className`
    - `score`
    - `bboxXyxy`
- Let Spring own:
  - quality checks
  - confidence thresholds
  - min box area filtering
  - ROI filtering
  - DB persistence
  - error translation
- Keep the Python service private inside Docker or cluster networking.

### Phase 4: Port domain logic to Spring services

- `ProjectService`
  - project CRUD
  - camera CRUD
- `DetectionService`
  - `/detect` orchestration
  - image download from MinIO
  - call Python YOLO service
  - apply quality checks and post-processing
  - optionally persist detections
- `SyncService`
  - Dropbox listing
  - upload new images to MinIO
  - create `images` records
  - create and finish `sync_jobs`
- `MetricsService`
  - daily aggregation
  - weekly rollup
  - activity heatmap
- `AlertService`
  - stall detection
  - anomaly detection
  - schedule risk alerts
  - alert status updates
- `PlanService`
  - PDF upload
  - text extraction
  - OpenAI milestone parsing
  - milestone updates
  - plan check execution
- `ReportService`
  - collect images and metrics
  - generate report markdown with OpenAI
  - persist `progress_reports`
- `VisualizationService`
  - draw detection boxes in Java
  - upload annotated images back to MinIO

### Phase 5: Port the HTTP API

- Recreate these endpoints in Spring Boot with the same paths and JSON contract:
  - `/`
  - `/health`
  - `/detect`
  - `/api/projects`
  - `/api/projects/{projectId}`
  - `/api/projects/{projectId}/cameras`
  - `/api/projects/{projectId}/metrics/daily`
  - `/api/projects/{projectId}/metrics/weekly`
  - `/api/projects/{projectId}/metrics/generate`
  - `/api/projects/{projectId}/alerts`
  - `/api/projects/{projectId}/sync/status`
  - `/api/projects/{projectId}/sync/trigger`
  - `/api/projects/{projectId}/activity/heatmap`
  - `/api/projects/{projectId}/snapshot/dates`
  - `/api/projects/{projectId}/snapshot`
  - `/api/projects/{projectId}/visualize`
  - `/api/projects/{projectId}/plan/upload`
  - `/api/projects/{projectId}/plan`
  - `/api/projects/{projectId}/plan/milestones`
  - `/api/projects/{projectId}/plan/milestones/{milestoneId}`
  - `/api/projects/{projectId}/plan/check`
  - `/api/projects/{projectId}/reports/generate`
  - `/api/projects/{projectId}/reports`
  - `/api/projects/{projectId}/reports/{reportId}`
- Preserve raw image responses for snapshot and visualization-related endpoints.

### Phase 6: Replace APScheduler and thread-based background work

- Replace `app/worker/scheduler.py` with Spring scheduled jobs.
- Replace ad-hoc Python threads from API handlers with Spring async execution or a DB-backed job executor.
- Implement jobs for:
  - Dropbox sync
  - detection sweep for `NEW` images
  - nightly analysis
  - weekly plan check
- Ensure only one scheduler instance runs each recurring job in multi-node deployments.

### Phase 7: Update deployment and local development

- Replace the Python API container with the Spring Boot container.
- Keep a separate Python YOLO container.
- Update `docker-compose.yml` to run:
  - `postgres`
  - `minio`
  - `spring-app`
  - `python-yolo`
- Move startup migration responsibility to Flyway in the Spring app.
- Remove old Python worker startup paths after parity is verified.

### Phase 8: Testing and verification

- Add repository integration tests against Postgres.
- Add controller tests for request/response compatibility.
- Add contract tests for the Spring-to-Python inference API.
- Add end-to-end tests for:
  - sync flow
  - detection flow
  - metrics generation
  - plan upload and parsing
  - report generation
- Run side-by-side comparisons between Python and Spring for key scenarios before cutover.

### Phase 9: Cutover and cleanup

- Deploy Spring Boot with the old Python backend still available for rollback.
- Switch frontend and automation to the Spring Boot base URL.
- Monitor job execution, DB writes, and generated outputs.
- After stable validation:
  - remove FastAPI controllers
  - remove SQLAlchemy DB layer
  - remove APScheduler worker
  - remove Dropbox/OpenAI/MinIO business services from Python
- Keep only the minimal Python inference service and its model/runtime dependencies.

## Important Design Decisions

- Do not redesign the database in the first pass.
- Do not expose the Python YOLO service publicly.
- Do not let Spring and Python both write the same business data during steady state.
- Keep all business rules in Java, even if some of them are easy to leave in Python.
- Use Hibernate/JPA with disciplined entity design, not entity-driven controller contracts.
- Use Lombok to reduce boilerplate, but keep entity `toString` and `equals/hashCode` tightly scoped.
- Expose API documentation through Swagger/OpenAPI from the Spring Boot app.

## Definition of Done

- Spring Boot serves all current REST endpoints.
- Spring Boot owns all DB writes and all scheduled/background processing.
- Spring Boot owns Dropbox, MinIO, OpenAI, PDF, metrics, alerts, plans, reports, and visualization logic.
- Python only loads YOLO and returns inference results.
- Docker local setup works with the new split architecture.
- Existing frontend flows continue working without API-breaking changes.
