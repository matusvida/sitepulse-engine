# Cloud Plan Phase 1 and 2

## Summary
- Phase 1 adds provider-based storage in `sitepulse-engine` with a new `GcsObjectStorage` while preserving current behavior, including the legacy byte snapshot endpoint and the newer signed snapshot endpoint.
- Phase 2 switches the GCP deployment path to GCS-backed storage while keeping local development on MinIO.
- Phase 3 snapshot work is already mostly in place, so this plan preserves and carries it forward rather than replacing it.

## Backend Changes
### Phase 1: `GcsObjectStorage`, behavior preserved
- Split the current MinIO-only storage adapter behind `ObjectStorage` into provider-specific implementations:
  - `MinioObjectStorage`
  - `GcsObjectStorage`
- Introduce provider-neutral storage configuration:
  - `storage-provider=minio|gcs`
  - `storage-default-bucket`
  - `storage-presign-ttl-minutes`
  - keep provider-specific sections for MinIO and GCS
- Preserve existing `ObjectStorage` semantics:
  - `download`
  - `exists`
  - `upload`
  - `defaultBucket`
  - `presign(bucket, key, Duration)`
- Add `GcsObjectStorage` using the Google Cloud Storage Java client:
  - object upload with content type
  - object download
  - existence check
  - signed URL generation with TTL
  - default bucket access
- Keep current callers unchanged at the use-case level:
  - Dropbox sync upload path
  - detection image download path
  - report/plan evidence image reads
  - legacy byte snapshot endpoint
  - signed snapshot metadata endpoint
- Preserve current persisted `bucket` + `key` model in the database. No schema migration in Phase 1.
- Generalize the remaining MinIO/S3-specific edge in detection input paths:
  - stop depending on `properties.minioBucketDefault()`
  - replace `s3Url` naming/validation with storage-neutral naming or explicit `bucket + key`
  - if backward compatibility is needed, accept `s3://...` only as a legacy alias

### Phase 2: GCP deployment switches to GCS
- Add deployment config for GCS-backed runtime:
  - `STORAGE_PROVIDER=gcs`
  - `STORAGE_DEFAULT_BUCKET=<gcs bucket>`
  - GCS signing configuration via the Cloud Run service account
- Keep local development on MinIO:
  - `STORAGE_PROVIDER=minio`
  - local/private MinIO endpoint for backend access
  - local/public MinIO endpoint for presigned URLs
- Add `cloudbuild.yaml` for:
  - building the Spring image
  - pushing to Artifact Registry
  - deploying to Cloud Run
- Use managed GCP services:
  - Cloud Run for `sitepulse-engine`
  - Cloud SQL for PostgreSQL
  - Cloud Storage bucket for images
- Configure the Cloud Run service account with minimum required IAM:
  - object read/write on the storage bucket
  - ability to sign GCS URLs
  - Cloud SQL access if using Cloud SQL
- Define production env/secrets mapping:
  - database connection or Cloud SQL config
  - Dropbox credentials
  - OpenAI credentials
  - GCS bucket/provider config
  - YOLO service base URL if still external
- Add bucket CORS for the FE origin because `/snapshots` must keep working after the GCS switch.
- Preserve the legacy byte snapshot endpoint in production after cutover.

## Sequencing
1. Refactor provider-neutral storage config and split the MinIO adapter.
2. Add `GcsObjectStorage`.
3. Generalize MinIO-specific bucket and S3 assumptions in detection input paths.
4. Verify no regressions against MinIO locally.
5. Add Cloud Build, Artifact Registry, Cloud Run, Cloud SQL, and GCS deployment config.
6. Deploy to staging with `STORAGE_PROVIDER=gcs`.
7. Verify sync uploads, byte snapshots, signed snapshots, detection reads, and evidence image reads.
8. Cut production over to GCS-backed deployment.

## Test Plan
- Unit tests for both `MinioObjectStorage` and `GcsObjectStorage`
- Compatibility tests for all `ObjectStorage` methods
- Regression tests for:
  - Dropbox sync uploads
  - legacy byte snapshot endpoint
  - signed snapshot metadata endpoint
  - detection image download path
- Targeted tests for generalized detection target parsing if `s3Url` handling changes
- Staging deploy smoke tests on Cloud Run:
  - image upload to GCS after sync
  - `GET /snapshot` still returns bytes
  - `GET /snapshots` returns signed GCS URLs that load in browser
  - plan/report evidence image flows still work
- Operational checks:
  - local Docker Compose still works with MinIO
  - Cloud Run service account permissions are sufficient
  - bucket CORS allows the deployed FE origin

## Assumptions
- Phase 3 snapshot delivery remains the forward path and must not regress.
- Local development continues to use MinIO.
- GCP production uses Cloud Run + Cloud SQL + Cloud Storage.
- The database schema remains unchanged for Phase 1 and Phase 2.
