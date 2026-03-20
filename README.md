# sitepulse-engine — Construction Site Object Detection

YOLO-based object detection and analysis service for construction-site
imagery.  Syncs images from Dropbox, runs inference via YOLOv8x, persists
results to Postgres, and generates daily/weekly activity metrics and alerts.

## Architecture

```
Dropbox ──▶ Sync Service ──▶ MinIO ──▶ Detection Worker ──▶ Postgres
                                                               │
                                                     Nightly Analysis
                                                               │
                                                   daily_metrics / alerts
                                                               │
                                                        REST API ──▶ Frontend
```

## Project layout

```
sitepulse-engine/
├── app/
│   ├── main.py                    # FastAPI factory + lifespan
│   ├── __main__.py                # python -m app → uvicorn
│   │
│   ├── core/                      # Configuration & logging
│   │   ├── settings.py            # Pydantic-settings (env vars)
│   │   └── logging.py            # Shared structlog configuration
│   │
│   ├── db/                        # Database layer
│   │   ├── engine.py              # Engine singleton + Alembic runner
│   │   ├── tables.py             # All SQLAlchemy Table definitions
│   │   ├── images.py             # Image CRUD & status transitions
│   │   ├── detections.py         # Detection record inserts
│   │   ├── projects.py           # Project + Camera CRUD
│   │   ├── alerts.py             # Alert queries & auto-resolve
│   │   └── sync_jobs.py          # Sync job tracking
│   │
│   ├── detection/                 # ML inference pipeline
│   │   ├── model.py              # YOLO loading & inference
│   │   ├── postprocess.py        # Confidence / area / ROI filtering
│   │   ├── quality.py            # Blur & brightness heuristics
│   │   └── schemas.py            # Pydantic request/response models
│   │
│   ├── api/                       # HTTP endpoints
│   │   ├── __init__.py            # Router aggregation
│   │   └── detect.py             # /detect, /health routes
│   │
│   ├── services/                  # External integrations & business logic
│   │   ├── storage.py            # MinIO/S3 client (boto3)
│   │   ├── dropbox.py            # Dropbox shared-link API client
│   │   ├── sync.py               # Dropbox → MinIO → DB orchestrator
│   │   └── analysis.py           # Daily/weekly aggregation + alerts
│   │
│   └── worker/                    # Background processing
│       ├── processor.py           # Image detection processing
│       └── scheduler.py          # APScheduler entry point
│
├── migrations/                    # Alembic schema migrations
│   ├── env.py
│   └── versions/
│       └── 001_initial_schema.py
│
├── roi_config.json                # File-based ROI fallback
├── alembic.ini
├── requirements.txt
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── .gitignore
```

## Quick start (local, no Docker)

```bash
cd sitepulse-engine
python -m venv .venv && .venv\Scripts\activate   # Windows
# source .venv/bin/activate                       # Linux/Mac

pip install -r requirements.txt
cp .env.example .env   # edit as needed

uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

## Quick start (Docker Compose)

```bash
cd sitepulse-engine
cp .env.example .env

# Brings up Postgres + MinIO + API + Worker
docker compose up --build

# API: http://localhost:8080
# MinIO console: http://localhost:9091 (admin / password123)
```

## API usage

```bash
# Health check
curl http://localhost:8080/health

# Detect objects (by key, using default bucket)
curl -X POST http://localhost:8080/detect \
  -H "Content-Type: application/json" \
  -d '{"key": "2026-03-04/2026-03-04 11_32_04.jpg"}'

# Detect objects (by S3 URL)
curl -X POST http://localhost:8080/detect \
  -H "Content-Type: application/json" \
  -d '{"s3_url": "s3://tower-tl/2026-03-04/2026-03-04 11_32_04.jpg"}'
```

## Running the worker / scheduler

The scheduler runs Dropbox sync, detection sweeps, and nightly analysis:

```bash
# Via Docker Compose (automatic)
docker compose up --build

# Standalone
ENABLE_DB=true python -m app.worker.scheduler

# Standalone detection worker (no scheduler)
ENABLE_DB=true python -m app.worker.processor
```

## Configuration

All settings via environment variables (see `.env.example`):

| Variable | Default | Description |
|---|---|---|
| `MINIO_ENDPOINT` | `http://localhost:9000` | S3-compatible endpoint |
| `MINIO_ACCESS_KEY` | `admin` | S3 access key |
| `MINIO_SECRET_KEY` | `password123` | S3 secret key |
| `MINIO_BUCKET_DEFAULT` | `tower-tl` | Default bucket |
| `YOLO_MODEL_PATH` | `yolov8x.pt` | YOLO weights path/name |
| `CONF_THRESHOLD` | `0.35` | Global confidence threshold |
| `PER_CLASS_THRESHOLDS_JSON` | `{}` | Per-class threshold JSON |
| `MIN_BOX_AREA` | `400` | Min detection area (px²) |
| `ENABLE_ROI` | `false` | Enable file-based ROI filtering |
| `ENABLE_DB` | `false` | Enable Postgres integration |
| `POSTGRES_DSN` | `postgresql://sitepulse:sitepulse@…` | Postgres DSN |
| `DROPBOX_TOKEN` | `` | Dropbox API access token |
| `SYNC_SCHEDULE_MINUTES` | `60` | Dropbox sync interval |
| `ANALYSIS_HOUR` | `2` | Nightly analysis hour (UTC) |
| `MIN_DETECTIONS_ACTIVE_HOUR` | `3` | Min detections to count active hour |
| `WORKER_POLL_INTERVAL` | `5` | Seconds between detection sweeps |

## Database tables

| Table | Purpose |
|---|---|
| `projects` | Construction site projects |
| `cameras` | Per-project cameras with ROI polygons |
| `images` | Synced/detected image records |
| `detections` | Individual YOLO detections |
| `daily_metrics` | Aggregated daily activity stats |
| `weekly_metrics` | Weekly rollups with risk levels |
| `alerts` | Generated alerts (stall, anomaly, schedule risk) |
| `sync_jobs` | Dropbox sync job tracking |
