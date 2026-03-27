# SitePulse Engine — Agent Guidelines

## Project Overview

Python/FastAPI backend for a construction site monitoring platform. Syncs timelapse photos from Dropbox, stores them in MinIO (S3-compatible), runs YOLOv8x object detection, computes daily/weekly activity metrics, generates AI progress reports via GPT-4o Vision, and tracks construction plans against milestones.

## Tech Stack

- Python 3.11+, FastAPI, Uvicorn
- Pydantic v2 + pydantic-settings for configuration
- SQLAlchemy 2.0 Core (not ORM), psycopg2, Alembic for migrations
- Ultralytics YOLOv8x, OpenCV (headless), NumPy, Pillow
- boto3 for MinIO/S3
- OpenAI SDK (GPT-4o / GPT-4o Vision)
- pdfplumber for PDF text extraction
- APScheduler 3.x for cron jobs
- Dropbox SDK for cloud sync
- structlog for structured JSON logging
- Docker + Docker Compose for deployment

## Architecture

The system runs as two processes sharing the same codebase:

1. **API** (`app.main:app`) — FastAPI server on port 8000 (exposed as 8080). Handles HTTP requests, serves images, triggers sync/detection.
2. **Worker** (`app.worker.scheduler`) — APScheduler process running cron jobs for Dropbox sync, detection sweeps, nightly analysis, and weekly plan checks.

Both share the same database, MinIO instance, and configuration.

## File Layout

```
app/
  main.py              # FastAPI app factory, lifespan, CORS
  __main__.py           # uvicorn entrypoint
  core/
    settings.py         # Pydantic settings (env vars). Single source of truth for config.
    logging.py          # structlog configuration
  api/
    __init__.py         # Router aggregation — all sub-routers combined here
    detect.py           # POST /detect, GET /health
    projects.py         # /api/projects/* (CRUD, metrics, alerts, sync, snapshots, heatmap, visualize)
    plans.py            # /api/projects/{id}/plan/* (PDF upload, milestones, progress check)
    reports.py           # /api/projects/{id}/reports/* (generate, list, detail)
  db/
    engine.py           # SQLAlchemy engine singleton, Alembic runner
    tables.py           # All table definitions (single source of truth for schema)
    images.py           # Image CRUD queries
    detections.py       # Detection CRUD queries
    projects.py         # Project/camera queries
    alerts.py           # Alert queries
    sync_jobs.py        # Sync job tracking queries
  detection/
    model.py            # YOLO model loader (singleton)
    postprocess.py      # Confidence filtering, ROI polygon, min-area filtering
    quality.py          # Blur/brightness quality checks
    schemas.py          # Pydantic response schemas for /detect
  services/
    storage.py          # MinIO/S3 client wrapper (upload, download, list, presign)
    sync.py             # Dropbox → MinIO sync pipeline
    dropbox.py          # Dropbox API client with refresh token support
    analysis.py         # Nightly analysis: daily/weekly metrics, alert generation
    llm.py              # OpenAI GPT-4o wrapper (parse_plan, generate_report, evaluate_milestone)
    pdf_parser.py       # PDF text extraction via pdfplumber
    plan_tracker.py     # Weekly plan check: milestone evaluation + schedule alerts
    visualize.py        # Draw bounding boxes on images and upload to MinIO
  worker/
    scheduler.py        # APScheduler main loop (Dropbox sync, detection sweep, nightly, plan check)
    processor.py        # Picks NEW images, runs YOLO, persists detections
  scripts/
    dropbox_auth.py     # Helper to obtain Dropbox refresh token
migrations/
  versions/             # Alembic migration scripts (001 = initial, 002 = plans + reports)
  env.py
  alembic.ini
docker-compose.yml      # postgres, api, worker, minio
Dockerfile
requirements.txt
```

## Database

PostgreSQL 16. Tables defined in `app/db/tables.py` using SQLAlchemy Core `Table` objects — no ORM models. All queries use raw `sqlalchemy.text()` or Core `select`/`insert`/`update` constructs.

**Tables:** `projects`, `cameras`, `images`, `detections`, `daily_metrics`, `weekly_metrics`, `alerts`, `sync_jobs`, `construction_plans`, `plan_milestones`, `progress_reports`.

Migrations are in `migrations/versions/`. Run automatically on startup when `ENABLE_DB=true` (via `run_migrations()` in lifespan).

## Configuration

All config is in `app/core/settings.py` via `pydantic-settings`. Every field maps to an environment variable (e.g., `minio_endpoint` → `MINIO_ENDPOINT`). Supports `.env` files.

Key variables: `MINIO_ENDPOINT`, `MINIO_BUCKET_DEFAULT`, `POSTGRES_DSN`, `ENABLE_DB`, `ENABLE_ROI`, `DROPBOX_REFRESH_TOKEN`, `OPENAI_API_KEY`, `OPENAI_MODEL`, `CORS_ORIGINS`.

Access settings anywhere via `from app.core import get_settings; cfg = get_settings()`.

## API Design

- All project-scoped endpoints are under `/api/projects/{project_id}/...`
- The `/detect` and `/health` endpoints are at the root level
- Responses use **camelCase** JSON keys (FastAPI `response_model` with Pydantic `alias_generator`)
- Image endpoints (`/snapshot`, `/visualize`) return raw bytes with `Content-Type: image/jpeg`
- File uploads use `multipart/form-data` (requires `python-multipart`)

## Key Conventions

- Use SQLAlchemy Core, not ORM. Queries go in `app/db/` modules. Do not put raw SQL in API routes.
- Services in `app/services/` contain business logic. API routes should be thin — validate input, call a service, return response.
- The YOLO model is loaded once at startup via `load_model()` and accessed as a module-level singleton in `detection/model.py`.
- MinIO client is a module-level singleton in `services/storage.py`.
- Logging uses `structlog` — always use `logger.info("event_name", key=value)` pattern, never f-strings in log messages.
- ROI polygons are stored as JSONB arrays of `[x, y]` pairs in the `cameras` table. Filtering uses Shapely-like point-in-polygon via `cv2.pointPolygonTest`.

## Adding New Features

1. **New table** — define in `app/db/tables.py`, create a migration in `migrations/versions/`, add query helpers in `app/db/`.
2. **New endpoint** — add to existing router in `app/api/` or create a new router and register it in `app/api/__init__.py`.
3. **New service** — add to `app/services/`. Import in the endpoint or worker that needs it.
4. **New scheduled job** — add to `app/worker/scheduler.py` following the existing pattern.
5. **New dependency** — add to `requirements.txt` with version bounds. Rebuild Docker image.

## Do Not

- Do not use SQLAlchemy ORM (Session, mapped classes). Stick to Core.
- Do not put business logic directly in API route handlers — extract to services.
- Do not hardcode bucket names or paths — use `cfg.minio_bucket_default` and project/camera `key_prefix`.
- Do not create synchronous blocking calls in async endpoints — use `asyncio.to_thread()` for CPU-bound work if needed.
- Do not store secrets in code or `docker-compose.yml` — use `.env` files or environment variables.
- Do not modify the `metadata` object outside of `app/db/tables.py`.

## Running Locally

```bash
docker compose up -d --build
```

Services: API at `localhost:8080`, MinIO console at `localhost:9091`, Postgres at `localhost:5432`.
