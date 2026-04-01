# SitePulse HTTP API Rework Plan

## Goal

Rework `sitepulse-engine` from a single-module Spring Boot application into a structure similar to `C:\workspace\dynamic-pricing\product-promotion-service`, where HTTP API contracts are separated from application implementation.

Target intent:

- keep `sitepulse-engine` as the runnable Spring Boot application
- introduce `sitepulse-engine-http-api` as a contract module containing HTTP interfaces, request DTOs, response DTOs, and API enums
- optionally prepare for a future `sitepulse-engine-event-api` module if event contracts become important later
- keep Python YOLO as a separate runtime service; it is unrelated to the HTTP API modularization

This is an API-contract modularization, not full DDD. It moves the project toward a cleaner boundary between:

- public API contract
- controller implementation
- business logic and persistence

## Reference Pattern Observed

From `C:\workspace\dynamic-pricing\product-promotion-service`:

- root is a Maven aggregator with multiple modules
- `pps-http-api` contains:
  - `...http.{domain}.api.*Api`
  - `...http.{domain}.dto.*`
- `pps-app` contains:
  - controllers implementing the API interfaces
  - services, repositories, entities, mappers, config

Useful examples:

- API contract interface:
  - `C:\workspace\dynamic-pricing\product-promotion-service\pps-http-api\src\main\java\group\rohlik\pps\http\productpromotionforecast\api\ProductPromotionForecastApi.java`
- Controller implementing the contract:
  - `C:\workspace\dynamic-pricing\product-promotion-service\pps-app\src\main\java\group\rohlik\pps\app\productpromotionforecast\controller\ProductPromotionForecastController.java`
- documented structure:
  - `C:\workspace\dynamic-pricing\product-promotion-service\docs\01-PROJECT-STRUCTURE.md`

## Proposed Target Structure

```text
sitepulse-engine/
|-- pom.xml                              # aggregator / dependency management
|-- sitepulse-engine-http-api/
|   `-- src/main/java/com/sitepulse/engine/http/
|       |-- detection/
|       |   |-- api/
|       |   `-- dto/
|       |-- project/
|       |   |-- api/
|       |   `-- dto/
|       |-- plan/
|       |   |-- api/
|       |   `-- dto/
|       |-- report/
|       |   |-- api/
|       |   `-- dto/
|       |-- alert/
|       |   |-- api/
|       |   `-- dto/
|       `-- common/
|           `-- dto/
|-- sitepulse-engine/
|   |-- pom.xml
|   `-- src/main/java/com/sitepulse/engine/app/
|       |-- config/
|       |-- common/
|       |-- detection/
|       |   |-- controller/
|       |   |-- application/
|       |   |-- domain/
|       |   `-- persistence/
|       |-- project/
|       |-- plan/
|       |-- report/
|       |-- metrics/
|       |-- alert/
|       |-- sync/
|       |-- visualization/
|       |-- integration/
|       `-- scheduler/
|-- python-yolo/
|-- docker-compose.yml
`-- docs/
```

## Design Rules

Adopt these rules during the rework:

1. `sitepulse-engine-http-api` must not depend on the app module.
2. `sitepulse-engine` may depend on `sitepulse-engine-http-api`.
3. Controllers in the app module should implement the HTTP API interfaces from the contract module.
4. Request/response DTOs used by public REST endpoints should live in the HTTP API module.
5. JPA entities, repositories, application services, schedulers, Feign clients, MinIO/Dropbox/OpenAI adapters, and Flyway migrations stay in the app module.
6. Internal integration DTOs for OpenAI, YOLO, Dropbox, MinIO do not belong in the HTTP API module.
7. Do not move persistence entities into the API module.
8. Keep package-by-feature inside both modules.

## What Should Move to `sitepulse-engine-http-api`

Move public REST contracts only.

### Detection

- `detect`
- `health`
- public request/response DTOs currently under:
  - `src/main/java/com/sitepulse/engine/detection/web/dto`

### Project

- project CRUD endpoints
- camera endpoints
- metrics trigger and query endpoints if those are public
- snapshot and visualization endpoint request/response DTOs
- DTOs currently under:
  - `src/main/java/com/sitepulse/engine/project/web/dto`
  - `src/main/java/com/sitepulse/engine/metrics/web/dto`
  - `src/main/java/com/sitepulse/engine/visualization/web/dto`
  - `src/main/java/com/sitepulse/engine/alert/web/dto`

### Plan

- plan upload/check/milestone update API contracts
- DTOs currently under:
  - `src/main/java/com/sitepulse/engine/plan/web/dto`

### Report

- report generation/list/detail contracts
- DTOs currently under:
  - `src/main/java/com/sitepulse/engine/report/web/dto`

## What Must Stay in `sitepulse-engine`

- all JPA entities in `...domain`
- repositories in `...persistence`
- business logic in `...application`
- scheduler classes
- Flyway migrations
- configuration classes
- exception handlers
- OpenAI, Dropbox, MinIO, PDF, YOLO integration clients and internal DTOs
- sync orchestration and persistence helpers

## Recommended Module Names

Use names aligned to the reference style but adapted to your project:

- `sitepulse-engine-http-api`
- `sitepulse-engine`

Optional later:

- `sitepulse-engine-event-api`

## Migration Phases

### Phase 1: Maven Restructure

1. Convert current root `pom.xml` into an aggregator POM with `packaging=pom`.
2. Add modules:
   - `sitepulse-engine-http-api`
   - `sitepulse-engine`
3. Move current runnable application code and resources into `sitepulse-engine`.
4. Keep shared dependency versions in the root `pom.xml`.
5. Make `sitepulse-engine` depend on `sitepulse-engine-http-api`.

### Phase 2: Create HTTP API Contract Module

1. Create package root:
   - `com.sitepulse.engine.http`
2. For each public area create:
   - `...api`
   - `...dto`
3. Introduce API interfaces similar to the reference:
   - `ProjectApi`
   - `DetectionApi`
   - `PlanApi`
   - `ReportApi`
   - `AlertApi`
4. Put `@RequestMapping`, endpoint method signatures, validation annotations, and OpenAPI annotations on those interfaces.
5. Move public request/response DTOs into the contract module.

### Phase 3: Adapt Controllers in App Module

1. Move controller implementations under:
   - `com.sitepulse.engine.app.{feature}.controller`
2. Make controllers implement interfaces from `sitepulse-engine-http-api`.
3. Remove duplicated request mapping and method signatures from controllers where the interface already defines them.
4. Keep controllers thin:
   - validate
   - delegate to service
   - map response

### Phase 4: Separate Public DTOs from Internal DTOs

1. Keep public REST DTOs in `sitepulse-engine-http-api`.
2. Keep integration DTOs in app:
   - `integration.openai.dto`
   - `integration.yolo.dto`
   - any Dropbox/MinIO/internal payloads
3. If an existing DTO mixes external/public semantics with internal processing semantics, split it.

### Phase 5: Package Cleanup in App Module

Restructure app code under `com.sitepulse.engine.app`:

- `app.project`
- `app.detection`
- `app.plan`
- `app.report`
- `app.metrics`
- `app.alert`
- `app.sync`
- `app.visualization`
- `app.integration`
- `app.scheduler`
- `app.config`
- `app.common`

Inside each feature keep:

- `controller`
- `application`
- `domain`
- `persistence`

Use `mapper` only where mapping complexity is real.

### Phase 6: OpenAPI and Swagger Alignment

1. Ensure OpenAPI annotations live primarily on API interfaces in `sitepulse-engine-http-api`.
2. Keep app-level OpenAPI configuration in `sitepulse-engine`.
3. Verify Swagger still renders correctly from the implemented interfaces.

### Phase 7: Testing Rework

1. Add contract-focused tests for the HTTP API module where useful:
   - serialization
   - validation
   - enum stability
2. Keep controller/service/repository integration tests in the app module.
3. Add an architectural test to prevent app internals from leaking into the API module.

## Concrete Refactor Order for This Codebase

Recommended order to minimize breakage:

1. Create aggregator POM and two modules.
2. Move current app unchanged into `sitepulse-engine`.
3. Create `sitepulse-engine-http-api`.
4. Move DTO classes first.
5. Introduce API interfaces feature by feature.
6. Make controllers implement the interfaces.
7. Update imports and package names.
8. Run compile and fix.
9. Run Swagger and endpoint smoke tests.
10. Only after that, do deeper package cleanup under `com.sitepulse.engine.app`.

## Risks

### Package Renaming Churn

Large import changes will create noisy diffs.

Mitigation:

- do module split first
- do package renames after compile is stable

### Swagger Regression

Moving annotations to interfaces may affect generated docs if not done consistently.

Mitigation:

- move one controller to interface-based style first
- verify Swagger before repeating the pattern

### DTO Leakage

It is easy to move internal integration DTOs into the public API module by mistake.

Mitigation:

- only move DTOs used directly in controller signatures
- keep OpenAI/YOLO/Dropbox/MinIO DTOs in app

### Over-Modularization

Splitting too aggressively can slow development if boundaries are artificial.

Mitigation:

- start with one dedicated HTTP API module only
- postpone event module unless there is a real consumer

## Suggested First Milestone

Implement the split for one feature first, preferably `project`.

Pilot:

1. create `sitepulse-engine-http-api`
2. move project DTOs there
3. create `ProjectApi`
4. make `ProjectController` implement `ProjectApi`
5. verify compile, Swagger, and one endpoint

If that works cleanly, repeat for:

1. detection
2. plan
3. report
4. alert
5. metrics/visualization

## Definition of Done

The rework is complete when:

- root is a multi-module Maven project
- public REST contracts live in `sitepulse-engine-http-api`
- app controllers implement those API interfaces
- app internals remain in `sitepulse-engine`
- Swagger still works
- Spring Boot app starts normally
- existing REST behavior remains unchanged
- internal integration DTOs are not exposed through the API module
