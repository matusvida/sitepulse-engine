# sitepulse-engine

Backend for the SitePulse construction monitoring platform.

The system is now split into:

- `sitepulse-engine-http-api`: public HTTP contracts, request DTOs, response DTOs
- `sitepulse-engine-app`: runnable Spring Boot application with business logic, persistence, schedulers, and integrations
- `python-yolo`: internal Python service used only for YOLO inference

Spring Boot is the public backend. Python is not a public API anymore.

## Architecture

```text
Dropbox
  -> Spring sync pipeline
  -> MinIO raw image storage
  -> image registration in PostgreSQL
  -> scheduled/manual detection
  -> Python YOLO inference
  -> detections + metrics + alerts in PostgreSQL
  -> plan evaluation + AI reporting
  -> REST API + Swagger from Spring Boot
```

## End-To-End Pipeline

### 1. Project Setup

- a project is created in the Spring API
- cameras are assigned to the project
- each camera can define `keyPrefix`, ROI polygon, and `dropOutside`
- the project stores the Dropbox source path used for ingestion

### 2. Dropbox Sync

Main code:

- [SyncService.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\sync\application\SyncService.java)
- [SyncProjectExecutor.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\sync\application\SyncProjectExecutor.java)
- [DropboxClientService.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\integration\dropbox\DropboxClientService.java)
- [StorageService.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\integration\storage\StorageService.java)

Flow:

1. scheduler or user triggers project sync
2. Spring lists Dropbox date folders and files
3. each image is downloaded from Dropbox
4. the image is uploaded into MinIO
5. an `images` row is created in PostgreSQL with project, key, bucket, capture time, and status
6. a `sync_jobs` row tracks the overall job status

Dropbox is only the ingestion source. After sync, MinIO becomes the internal source of truth for image binaries.

### 3. Detection

Main code:

- [DetectionService.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\detection\application\DetectionService.java)
- [YoloFeignClient.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\integration\yolo\YoloFeignClient.java)
- [main.py](C:\workspace\learning\progress-tracker\sitepulse-engine\python-yolo\app\main.py)

Flow:

1. Spring claims `NEW` images from PostgreSQL
2. Spring downloads the image bytes from MinIO
3. Spring sends the image to the internal Python YOLO service through `/infer`
4. Spring applies post-processing:
   - confidence thresholds
   - min box area
   - ROI filtering
   - image quality warnings
5. Spring persists detections in PostgreSQL
6. image status is updated to `DONE` or `FAILED`

The Python service does only inference. All orchestration and persistence stay in Spring.

### 4. Metrics And Alerts

Main code:

- [AnalysisService.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\metrics\application\AnalysisService.java)
- [AlertService.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\alert\application\AlertService.java)

Flow:

1. Spring reads detections and processed images from PostgreSQL
2. it computes daily and weekly metrics
3. it derives activity summaries and risks
4. it creates or resolves alerts

### 5. Construction Plan Tracking

Main code:

- [PlanService.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\plan\application\PlanService.java)
- [PdfTextExtractor.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\integration\pdf\PdfTextExtractor.java)
- [OpenAiService.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\integration\openai\OpenAiService.java)

Flow:

1. user uploads a construction plan PDF
2. Spring extracts text from the PDF
3. OpenAI is used to parse milestones
4. milestones are stored in PostgreSQL
5. plan checks compare recent site images against expected progress
6. delayed milestones can create schedule alerts

### 6. Reporting

Main code:

- [ReportService.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\report\application\ReportService.java)

Flow:

1. Spring loads selected images from MinIO
2. Spring loads metrics and milestone context from PostgreSQL
3. OpenAI generates Markdown report content
4. the report is stored in PostgreSQL
5. report summaries and details are exposed through the REST API

### 7. Visualization

Main code:

- [VisualizationService.java](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\java\com\sitepulse\engine\visualization\application\VisualizationService.java)

Flow:

1. Spring loads original images from MinIO
2. Spring loads persisted detections from PostgreSQL
3. bounding boxes are rendered onto the image
4. the generated visualization is uploaded back to MinIO under a derived key

## Project Structure

```text
sitepulse-engine/
|-- pom.xml
|-- Dockerfile
|-- docker-compose.yml
|-- sitepulse-engine-http-api/
|   |-- pom.xml
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
|       |   `-- dto/
|       `-- common/
|           `-- dto/
|-- sitepulse-engine-app/
|   |-- pom.xml
|   `-- src/main/
|       |-- java/com/sitepulse/engine/
|       |   |-- config/
|       |   |-- common/
|       |   |-- project/
|       |   |-- sync/
|       |   |-- detection/
|       |   |-- metrics/
|       |   |-- alert/
|       |   |-- plan/
|       |   |-- report/
|       |   |-- visualization/
|       |   |-- integration/
|       |   |-- scheduler/
|       |   `-- root/
|       `-- resources/
|           |-- application.yml
|           |-- application-development.yml
|           `-- db/migration/
|-- python-yolo/
|   |-- Dockerfile
|   |-- requirements.txt
|   `-- app/
|-- seed.sql
|-- roi_config.json
|-- http-api-rework-plan.md
`-- ddd-refactor-plan.md
```

## Tech Stack

### Spring application

- Java 25
- Spring Boot
- Spring Web
- Spring Data JPA with Hibernate
- Flyway
- OpenFeign
- springdoc OpenAPI / Swagger UI
- PostgreSQL
- MinIO
- ShedLock
- Lombok

### YOLO service

- Python 3.12
- FastAPI
- Uvicorn
- Ultralytics YOLO
- OpenCV
- NumPy

## Running Locally

### Option 1: Recommended for development

Run infrastructure and YOLO in Docker, run Spring locally.

1. Set Java 25.
   ```powershell
   $env:JAVA_HOME='C:\Users\matus\.jdks\openjdk-25.0.1'
   $env:Path="$env:JAVA_HOME\bin;$env:Path"
   ```
2. Start infrastructure and YOLO.
   ```powershell
   docker compose up -d postgres minio python-yolo
   ```
3. Run Spring from the app module:
   ```powershell
   cd sitepulse-engine-app
   mvn spring-boot:run "-Dspring-boot.run.profiles=development"
   ```

You can also run from the repo root:

```powershell
mvn -pl sitepulse-engine-app spring-boot:run "-Dspring-boot.run.profiles=development"
```

### Option 2: Full Docker Compose

```powershell
docker compose up --build
```

## Local Endpoints

- Spring API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Python YOLO health: `http://localhost:8000/health`
- MinIO API: `http://localhost:9001`
- MinIO console: `http://localhost:9091`
- PostgreSQL: `localhost:5432`

## Configuration

Main Spring configuration files:

- [application.yml](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\resources\application.yml)
- [application-development.yml](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\resources\application-development.yml)

Important properties:

- `POSTGRES_DSN`
- `MINIO_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`
- `MINIO_BUCKET_DEFAULT`
- `DROPBOX_TOKEN`
- `DROPBOX_APP_KEY`
- `DROPBOX_APP_SECRET`
- `DROPBOX_REFRESH_TOKEN`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `PYTHON_YOLO_BASE_URL`
- `SYNC_CRON`
- `DETECTION_SWEEP_CRON`
- `CORS_ORIGINS`

Recommended local mode:

- use the `development` Spring profile
- point Spring to `localhost` services
- keep the Docker profile using internal hostnames like `python-yolo:8000`

## Database And Flyway

Flyway is the only migration tool now.

- migrations live in [db/migration](C:\workspace\learning\progress-tracker\sitepulse-engine\sitepulse-engine-app\src\main\resources\db\migration)
- empty database: Flyway runs the baseline migration
- existing Python-era database: Flyway baselining adopts the schema and continues from there

Going forward:

- do not use Alembic
- add new schema changes only as Flyway migrations such as `V2__...sql`, `V3__...sql`

## API Design Notes

- public REST contracts live in `sitepulse-engine-http-api`
- Spring controllers in `sitepulse-engine-app` implement those contracts
- project endpoints are under `/api/projects/...`
- detection health and on-demand detection are served by Spring
- the Python YOLO service is internal and should not be treated as a public backend

## Development Notes

- keep controllers thin
- keep business logic in application/domain services, not in HTTP adapters
- keep integration DTOs inside the app module
- keep HTTP DTOs and API interfaces inside the HTTP API module
- use enums instead of raw status strings where possible
- prefer explicit result objects over `Map<String, Object>` in new code

## Current Refactor Direction

Two planning documents describe the intended architecture work:

- [http-api-rework-plan.md](C:\workspace\learning\progress-tracker\sitepulse-engine\http-api-rework-plan.md)
- [ddd-refactor-plan.md](C:\workspace\learning\progress-tracker\sitepulse-engine\ddd-refactor-plan.md)

The current code is modularized at the HTTP boundary and is being prepared for a gradual DDD-lite refactor, starting with the `sync` context.
