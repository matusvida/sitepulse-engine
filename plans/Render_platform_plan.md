# Render Platform Migration Plan

## Goal

Migrate the current backend deployment from GCP Cloud Run + Cloud SQL + GCS to:

- Render Web Service for the Spring Boot backend
- Render Postgres for the application database
- Cloudflare R2 for image/object storage

The migration should also remove obsolete GCP-specific code, dependencies, scripts, and deployment assets from the repository.

## Repository Assessment

### Current strengths

- The backend is already Dockerized and can be built from the existing `Dockerfile`.
- PostgreSQL is already abstracted behind a single `POSTGRES_DSN` property.
- Object storage is abstracted behind the `ObjectStorage` port.
- The application already exposes actuator health endpoints suitable for a Render health check.
- Flyway is already used as the schema migration mechanism.

### Current GCP coupling

The repo still contains direct GCP-specific implementation and deployment assets:

- `sitepulse-engine-app/src/main/java/com/sitepulse/engine/common/infrastructure/external/storage/GcsObjectStorage.java`
- `sitepulse-engine-app/pom.xml`
  - `com.google.cloud:google-cloud-storage`
  - `com.google.cloud.sql:postgres-socket-factory`
- `cloudbuild.yaml`
- `GCP_GITHUB_DEPLOYMENT.md`
- `scripts/Start-CloudSqlProxy.ps1`
- GCS-related configuration in `application.yml`

### Current storage model

The application currently supports:

- `storage-provider=minio` using the MinIO Java SDK
- `storage-provider=gcs` using the GCS Java SDK

The MinIO adapter is already S3-compatible in practice and is the natural base for Cloudflare R2. The current naming is misleading for production, because Cloudflare R2 is not MinIO even though it is S3-compatible.

### Current runtime caveat

Detection is not purely independent from the Python YOLO service. Even when the configured detection provider is OpenAI, the application falls back to YOLO on repeated OpenAI failures. That means production still depends on `python-yolo` unless the fallback behavior is changed.

## Migration Principles

1. Keep domain and application logic unchanged where possible.
2. Treat Cloudflare R2 as the only production object storage target.
3. Remove GCP-specific code instead of keeping dead dual-mode production logic.
4. Keep local development support with Docker Compose and local MinIO unless there is a strong reason to replace it.
5. Make production deployment explicit and reproducible with Render-managed configuration.

## Target Runtime Architecture

### Production

- Render Web Service
  - runs the Spring Boot backend from the repository `Dockerfile`
- Render Postgres
  - primary application database
- Cloudflare R2
  - stores raw images, visualization outputs, and any storage-backed report artifacts

### Optional internal service

One of these must be chosen before cutover:

- deploy `python-yolo` as a private/internal Render service
- remove or disable the YOLO fallback path for production

Without this decision, production behavior remains ambiguous.

## Required Code Changes

### 1. Replace GCS production support with a single S3-compatible production path

Recommended change:

- remove `GcsObjectStorage`
- remove `sitepulse.gcs.*` configuration
- replace the current `minio` production naming with a generic storage mode such as `s3` or `r2`

Suggested result:

- local development may still use MinIO through the same S3-compatible adapter
- production uses Cloudflare R2 through the same adapter

The implementation should stop expressing production storage as `minio`, because that makes the configuration misleading and encourages future environment-specific drift.

### 2. Refactor storage configuration

Current production-facing storage variables:

- `STORAGE_PROVIDER`
- `STORAGE_DEFAULT_BUCKET`
- `MINIO_ENDPOINT`
- `MINIO_PUBLIC_ENDPOINT`
- `MINIO_ACCESS_KEY`
- `MINIO_SECRET_KEY`

Recommended production-neutral variables:

- `STORAGE_PROVIDER=s3`
- `STORAGE_DEFAULT_BUCKET=<r2-bucket-name>`
- `S3_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com`
- `S3_PUBLIC_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com`
- `S3_ACCESS_KEY=<r2-access-key-id>`
- `S3_SECRET_KEY=<r2-secret-access-key>`
- `S3_REGION=auto`

If the team wants to minimize code churn, the existing `MINIO_*` names can be kept temporarily and only documented as R2-compatible. That is acceptable as a short-term bridge, but not ideal as the final production design.

### 3. Remove automatic bucket creation in production startup

The current MinIO adapter creates the default bucket on startup if it does not exist.

That behavior is acceptable for local Docker Compose, but it should not be the production default for managed storage. The production path should:

- expect the bucket to exist
- fail clearly if it does not

Possible approach:

- keep bucket auto-create only in a local development profile
- disable it for Render production

### 4. Make server port Render-compatible

Render injects a `PORT` environment variable. The application currently hardcodes:

- `server.port: 8080`

Required change:

- set `server.port: ${PORT:8080}`

This is mandatory for a reliable Render deployment.

### 5. Verify and simplify database configuration

The current DSN-based datasource config is compatible with Render Postgres as long as Render provides a standard Postgres connection string.

What should be done:

- keep `POSTGRES_DSN`
- remove the unused Cloud SQL socket factory dependency
- verify the DSN parser accepts the exact Render Postgres format used in deployment

If Render provides query parameters such as SSL settings, the parser and JDBC URL conversion must preserve them correctly.

### 6. Resolve YOLO production strategy

This is the main architectural decision still open.

#### Option A: keep YOLO in production

- deploy `python-yolo` as a private Render service
- set `PYTHON_YOLO_BASE_URL` to the internal Render URL
- keep current fallback behavior

Pros:

- behavior stays closest to current implementation
- less application code change

Cons:

- adds another service to operate on Render
- increases deployment complexity

#### Option B: make production OpenAI-only

- remove or disable fallback from OpenAI to YOLO in production
- treat YOLO as a local/dev-only or optional capability

Pros:

- simpler platform footprint
- easier Render deployment

Cons:

- changes runtime behavior
- needs explicit product/engineering approval

Recommendation:

- decide this before writing the final migration pull request

### 7. Remove GCP deployment assets

After the Render path is in place, remove:

- `cloudbuild.yaml`
- `GCP_GITHUB_DEPLOYMENT.md`
- `scripts/Start-CloudSqlProxy.ps1`
- any GCP-only local helper material that is no longer used

If some GCP migration history should be preserved, move it to an `archive/` docs folder instead of leaving it as active operational guidance.

### 8. Add Render deployment definition

Add a `render.yaml` blueprint that defines:

- the web service
- the Render Postgres database
- environment variables
- health check path

The blueprint should become the primary deployment artifact for this repository.

## Required Configuration for Render

### Backend service environment variables

Minimum expected variables:

- `POSTGRES_DSN`
- `STORAGE_PROVIDER`
- `STORAGE_DEFAULT_BUCKET`
- `STORAGE_PRESIGN_TTL_MINUTES`
- `S3_ENDPOINT` or temporary `MINIO_ENDPOINT`
- `S3_PUBLIC_ENDPOINT` or temporary `MINIO_PUBLIC_ENDPOINT`
- `S3_ACCESS_KEY` or temporary `MINIO_ACCESS_KEY`
- `S3_SECRET_KEY` or temporary `MINIO_SECRET_KEY`
- `CORS_ORIGINS`
- `DROPBOX_TOKEN` or OAuth refresh-token-based Dropbox credentials
- `DROPBOX_APP_KEY`
- `DROPBOX_APP_SECRET`
- `DROPBOX_REFRESH_TOKEN`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `DETECTION_PROVIDER`
- `PYTHON_YOLO_BASE_URL` if YOLO remains enabled

### Render health check

Recommended path:

- `/actuator/health`

### Render build/runtime mode

Recommended approach:

- keep the current Docker deployment model
- let Render build from the existing `Dockerfile`

No native runtime migration is needed.

## Required Configuration for Cloudflare R2

### Bucket setup

Create the production bucket in R2 ahead of cutover.

Requirements:

- bucket name matches the production storage naming expected by the app
- bucket exists before the app starts
- credentials have permission to read, write, and presign

### URL and signing model

The backend should continue generating presigned URLs from the server side.

Requirements:

- signed URLs must target the R2 S3 endpoint
- browser access must be tested from the production frontend origin
- if the frontend reads signed URLs directly, CORS must be configured accordingly

### Object migration

Existing GCS objects must be copied to R2 while preserving:

- bucket identity used by the app
- full object key paths

This is important because the database stores object references by `bucket` and `key`.

## Data Migration Plan

### 1. Database migration

Move from Cloud SQL Postgres to Render Postgres.

Recommended sequence:

1. create Render Postgres
2. restore a snapshot or logical dump from the current production database
3. run the backend against the restored Render database in a staging environment
4. validate Flyway startup and application reads
5. perform final cutover with a fresh final sync/dump before DNS or frontend switch

### 2. Object storage migration

Move from GCS to Cloudflare R2.

Recommended sequence:

1. create the target R2 bucket
2. bulk-copy existing objects from GCS to R2
3. verify object counts and random sample integrity
4. run the backend against R2 in staging
5. perform final delta copy before production cutover

### 3. Cutover ordering

Recommended order:

1. deploy staging backend on Render
2. validate staging against copied DB and R2 data
3. freeze or minimize production writes
4. run final DB sync and final object delta copy
5. deploy production backend on Render
6. switch frontend/backend integration to Render
7. monitor schedulers, storage access, detection flow, and report generation

## Verification Checklist

### Startup and infrastructure

- app boots successfully on Render
- Flyway completes on Render Postgres
- `/actuator/health` is healthy
- CORS works for the frontend origin

### Storage

- sync jobs can upload new objects to R2
- snapshot URLs are generated correctly
- browser can load presigned URLs
- visualization outputs are written successfully
- report/plan evidence image loading still works

### Database and schedulers

- existing projects, cameras, images, detections, plans, reports, and alerts load correctly
- ShedLock works correctly against Render Postgres
- scheduled sync, detection sweep, and nightly analysis all run correctly

### Detection

- on-demand detection works
- scheduled detection works
- OpenAI detection works with production credentials
- YOLO integration works if retained

### Business flows

- Dropbox sync imports images
- project snapshots load
- plan upload and milestone parsing work
- progress reports generate
- alerts continue to raise and resolve correctly

## Cleanup Tasks

### Code cleanup

- remove GCS adapter
- remove GCS config properties
- remove GCP Maven dependencies
- remove misleading production naming where feasible

### Documentation cleanup

- update `README.md` to make Render + Render Postgres + R2 the primary production story
- add Render deployment instructions
- remove or archive GCP deployment docs
- keep local Docker Compose docs for development

### Security cleanup

There are committed credentials in development configuration that should be treated as exposed.

Required actions:

- rotate Dropbox credentials
- rotate any other leaked secrets found in tracked files
- replace real secrets in tracked files with placeholders

This should happen immediately and not wait for the full platform migration.

## Recommended Execution Backlog

### Phase 1: safety and preparation

1. rotate exposed secrets
2. remove tracked secrets from config files
3. confirm Render and R2 accounts/projects are ready

### Phase 2: application refactor

1. add `PORT` support
2. remove GCS adapter and GCP dependencies
3. refactor storage config to generic S3/R2 naming
4. adjust bucket initialization behavior
5. decide and implement YOLO production strategy

### Phase 3: deployment assets

1. add `render.yaml`
2. update `README.md`
3. remove `cloudbuild.yaml`
4. remove GCP-specific docs and scripts

### Phase 4: staging migration

1. provision Render Postgres
2. provision R2 bucket and credentials
3. copy DB and object data into staging equivalents
4. deploy to Render staging
5. validate all critical flows

### Phase 5: production cutover

1. final DB migration
2. final object delta copy
3. deploy production on Render
4. switch traffic
5. monitor and verify scheduled jobs and storage behavior

## Deliverables

The final migration work should produce at least:

- Render-compatible Spring Boot config
- R2-compatible production storage config
- removal of GCP runtime code and dependencies
- `render.yaml`
- updated `README.md`
- removal or archival of GCP deployment assets
- a tested cutover runbook for DB and object migration

## Summary

This migration is straightforward from an application architecture perspective. The core backend is already portable. The work is mostly:

- removing GCP-specific code and deployment residue
- making the S3-compatible storage path the only production storage implementation
- adding Render-native deployment configuration
- deciding whether YOLO remains a deployed internal service

The only nontrivial architectural decision is the future of the Python YOLO service in production. Everything else is standard platform migration and cleanup work.
