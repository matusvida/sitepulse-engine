# SitePulse Engine - Agent Guidelines

## Project Overview

Spring Boot backend for a construction site monitoring platform. The Java application serves the public REST API, owns business logic, database operations, scheduled jobs, and third-party integrations. Python remains only as a small optional local-only YOLO inference service in `python-yolo`.

## Tech Stack

- Java 25
- Spring Boot
- Spring Web
- Spring Data JPA with Hibernate
- Flyway for schema migrations
- OpenFeign for third-party and internal HTTP integrations
- Lombok
- springdoc OpenAPI / Swagger UI
- PostgreSQL
- Cloudflare R2 in production, MinIO for local development
- Python FastAPI + Ultralytics YOLO for optional local-only image recognition
- Docker + Docker Compose

## Architecture

The system is split into two applications:

1. **Spring Boot app** at the repository root
   - public REST API
   - business logic
   - database access
   - schedulers
   - integrations with Dropbox, OpenAI, object storage, PDF parsing, and the optional YOLO service
2. **Python YOLO service** in `python-yolo/`
   - private inference endpoint only
   - no public business API
   - no database ownership

## File Layout

```text
src/
  main/
    java/com/sitepulse/engine/
      config/             # Spring config and properties
      common/             # shared exceptions and utilities
      root/               # root endpoints
      project/            # projects and cameras
      detection/          # detection orchestration and API
      metrics/            # daily and weekly metrics
      alert/              # alerts
      sync/               # Dropbox sync jobs
      plan/               # plans and milestones
      report/             # report generation
      visualization/      # image visualization
      integration/        # object storage, Dropbox, OpenAI, YOLO, PDF
      scheduler/          # scheduled jobs
    resources/
      application.yml
      db/migration/       # Flyway migrations
python-yolo/
  app/main.py             # internal YOLO API
docker-compose.yml
Dockerfile
pom.xml
```

## Database

PostgreSQL is owned by the Spring Boot application.

- Use JPA entities and Spring Data repositories
- Use Flyway as the only migration tool
- Baseline support is enabled so pre-existing Python-era databases can be adopted safely
- Add every future schema change as a new file in `src/main/resources/db/migration`

Relevant tables include:

- `projects`
- `cameras`
- `images`
- `detections`
- `daily_metrics`
- `weekly_metrics`
- `alerts`
- `sync_jobs`
- `construction_plans`
- `plan_milestones`
- `progress_reports`

## Configuration

Primary configuration lives in Spring Boot `application.yml` plus environment variables.

Important variables:

- `POSTGRES_DSN`
- `STORAGE_PROVIDER`
- `STORAGE_DEFAULT_BUCKET`
- `STORAGE_ENDPOINT`
- `STORAGE_PUBLIC_ENDPOINT`
- `STORAGE_ACCESS_KEY`
- `STORAGE_SECRET_KEY`
- `STORAGE_REGION`
- `DROPBOX_TOKEN`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `YOLO_MODEL_PATH`
- `PYTHON_YOLO_BASE_URL` for the local-only YOLO service
- `CORS_ORIGINS`

The Python service should keep only inference-related configuration.

Storage layout conventions:

- `projects.storage_key_prefix` defines the top-level object key prefix, for example `danubius`
- `cameras.dropbox_path` defines the Dropbox source for a specific camera
- `cameras.key_prefix` defines the camera segment under the project prefix, for example `cam1`
- synced image keys are built as `{project.storage_key_prefix}/{camera.key_prefix}/{date-folder}/{filename}`

## API Design

- Public API belongs to Spring Boot
- Use controllers only for HTTP mapping and validation
- Put business logic into application services
- Keep package structure feature-oriented
- Swagger/OpenAPI must stay enabled
- Prefer DTOs for request and response boundaries

## Key Conventions

- Use feature-based packaging under `com.sitepulse.engine`
- Keep code readable and maintainable first
- Use Hibernate/JPA for persistence, not raw SQL scattered through controllers
- Use Feign clients for external HTTP integrations
- Use Lombok, but on entities keep `@ToString` and `@EqualsAndHashCode` limited to relevant fields only
- Keep the YOLO service narrow: inference in, detections out
- Keep Spring controllers thin

## Adding New Features

1. **New table or schema change** - add a Flyway migration and update the relevant entity and repository
2. **New endpoint** - add a controller and delegate logic to a service
3. **New integration** - add a client in `integration/`, prefer Feign where appropriate
4. **New scheduled job** - add it in the Spring scheduling layer
5. **YOLO-related change** - keep it in `python-yolo` unless it belongs to orchestration rather than inference

## Do Not

- Do not reintroduce the old FastAPI backend as the main application
- Do not use Alembic for schema management
- Do not put business logic directly in controllers
- Do not let Python own database or public API behavior again
- Do not hardcode secrets or environment-specific endpoints
- Do not broaden the Python service beyond YOLO inference without a strong reason

## Running Locally

```powershell
docker compose up -d postgres minio python-yolo
$env:JAVA_HOME='C:\Users\matus\.jdks\openjdk-25.0.1'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn spring-boot:run
```

Services:

- Spring API at `localhost:8080`
- Swagger UI at `localhost:8080/swagger-ui.html`
- MinIO console at `localhost:9091`
- PostgreSQL at `localhost:5432`
- Python YOLO at `localhost:8000`
