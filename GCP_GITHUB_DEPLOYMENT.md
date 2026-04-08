# SitePulse Engine: GitHub to GCP Deployment Guide

This guide explains how to deploy `sitepulse-engine` from GitHub using Google Cloud Build in:

- project: `sitepulse-engine`
- region: `europe-central2`

It is written for the current backend setup in this repository:

- Spring Boot backend
- `cloudbuild.yaml` builds the backend image, pushes it, and deploys Cloud Run
- local development keeps MinIO
- GCP deployment uses a GCS-backed storage provider

## What This Setup Does

The current `cloudbuild.yaml` performs the full deploy pipeline:

1. push code to GitHub
2. Cloud Build trigger runs
3. Cloud Build builds and pushes the image
4. Cloud Run is updated with the new image

## Before You Start

Make sure the following are ready in Google Cloud:

- a GCP project selected: `sitepulse-engine`
- billing enabled
- permissions to create Cloud Build triggers, Artifact Registry repositories, Cloud Run services, Cloud Storage buckets, and Secret Manager secrets
- your GitHub repository available to connect as a Cloud Build source

## Enable Required APIs

Enable these Google Cloud APIs in the project:

- Cloud Build API
- Artifact Registry API
- Cloud Run Admin API
- Cloud Storage API
- Secret Manager API
- Cloud SQL Admin API

If you are setting up everything from the console, enable the APIs first before creating repositories or triggers.

## Create Artifact Registry

Create a Docker repository for images in `europe-central2`.

Suggested values:

- repository name: `sitepulse-engine`
- format: Docker
- location: `europe-central2`

The build file in this repo tags images like:

`europe-central2-docker.pkg.dev/$PROJECT_ID/sitepulse-engine/sitepulse-engine:$COMMIT_SHA`

## Create Storage For Images

For production, use Google Cloud Storage instead of MinIO.

Create:

- a GCS bucket for uploaded images
- a GCS service account key or workload identity setup for the application runtime

Suggested bucket behavior:

- keep the bucket private
- allow signed URL access for the frontend
- configure CORS for the frontend origin if the browser loads images directly from signed URLs

## Cloud SQL For PostgreSQL

Create a Cloud SQL for PostgreSQL instance for the backend database.

Suggested setup:

- region: `europe-central2`
- database name: `sitepulse`
- user: `sitepulse`

The backend still expects the database connection through `POSTGRES_DSN`.

## Secrets And Environment Variables

Store runtime secrets in Secret Manager where possible.

Minimum backend variables to configure in Cloud Run:

- `POSTGRES_DSN`
- `STORAGE_PROVIDER=gcs`
- `STORAGE_DEFAULT_BUCKET=<your-gcs-bucket>`
- `STORAGE_PRESIGN_TTL_MINUTES=60`
- `GCS_PROJECT_ID=<gcp-project-id>`
- `GCS_CREDENTIALS_PATH=/secrets/gcp/service-account.json`
- `DROPBOX_TOKEN` or `DROPBOX_REFRESH_TOKEN`
- `DROPBOX_APP_KEY`
- `DROPBOX_APP_SECRET`
- `OPENAI_API_KEY`
- `OPENAI_MODEL`
- `PYTHON_YOLO_BASE_URL`
- `CORS_ORIGINS`

For local development, keep using MinIO:

- `STORAGE_PROVIDER=minio`
- `MINIO_ENDPOINT=http://minio:9000`
- `MINIO_PUBLIC_ENDPOINT=http://localhost:9001`

## GitHub To Cloud Build

Use a Cloud Build trigger connected to your GitHub repository.

Recommended trigger setup:

- source: GitHub repository
- branch filter: `main`
- config file: `cloudbuild.yaml`
- region: `europe-central2`

When the trigger runs, Cloud Build will:

1. read the repository contents from GitHub
2. execute the build config from `cloudbuild.yaml`
3. build the Docker image
4. push the image to Artifact Registry
5. deploy the image to Cloud Run

### GitHub Connection Notes

Use the Cloud Build GitHub integration in the console to connect the repository.

The Cloud Build trigger should watch the branch you use for production deploys, usually `main`.

If you want pull request validation later, create a second trigger for PR events.

## Manual Console Flow

If you prefer to create everything manually in the console, follow this sequence:

1. Open the Google Cloud console for the project.
2. Create or verify the Artifact Registry Docker repository.
3. Connect Cloud Build to your GitHub repository.
4. Create a Cloud Build trigger for the desired branch.
5. Confirm the trigger uses this repo's `cloudbuild.yaml`.
6. Push a commit to the branch.
7. Open Cloud Build history to confirm the image build and deployment succeeded.
8. Open Cloud Run to confirm the service revision is healthy.

## Cloud Build Trigger Settings

Suggested trigger settings:

- name: `sitepulse-engine-main`
- branch: `^main$`
- build config: `cloudbuild.yaml`
- substitutions:
  - `_REGION=europe-central2`
  - `_ARTIFACT_REPOSITORY=sitepulse-engine`
  - `_IMAGE_NAME=sitepulse-engine`

If the trigger dialog asks for service account permissions, ensure the build service account can:

- push to Artifact Registry
- read the repository source
- deploy to Cloud Run
- act as the Cloud Run runtime service account if you specify one for the service

## Cloud Run Deployment

The build pipeline deploys to Cloud Run in `europe-central2`.

Suggested Cloud Run settings:

- service name: `sitepulse-engine`
- region: `europe-central2`
- platform: Cloud Run
- ingress: whatever matches your app access policy
- container port: `8080`

The container should receive the same runtime variables listed above.

The Cloud Build deploy step in this repository currently updates the image only. Set the runtime environment variables and secrets on the Cloud Run service itself, or extend the deploy step later if you want Cloud Build to manage them too.

If you need a manual rollback, redeploy an earlier image digest from Artifact Registry rather than rebuilding `latest`.

## Runtime Storage Behavior

This repository is set up so the backend can run in two storage modes:

- local development: MinIO
- GCP deployment: GCS

That means the same backend codebase can be deployed in both environments without changing the frontend contract.

For GCP, the backend uses:

- `GcsObjectStorage`
- signed URLs generated for browser access
- private bucket objects

For local development, the backend uses:

- `MinioObjectStorage`
- browser-reachable public MinIO endpoint for signed URLs

## What To Verify After Deployment

After a push and deploy, verify these endpoints and flows:

- `/actuator/health` returns healthy
- the application starts with the production `POSTGRES_DSN`
- project pages load
- image uploads/sync jobs can write to GCS
- signed snapshot URLs load in the browser
- the legacy byte snapshot endpoint still returns image bytes

## Troubleshooting

If the trigger runs but the deploy fails:

- check the build logs in Cloud Build
- verify the Artifact Registry repository exists in the same region
- confirm the Cloud Run runtime service account can read the container image
- confirm `STORAGE_PROVIDER=gcs` is paired with valid GCS credentials
- confirm the PostgreSQL connection string is reachable from Cloud Run

If signed image URLs fail in the browser:

- check bucket CORS
- confirm the URL was generated for the correct bucket and key
- confirm the URL expiration is still valid
- confirm the backend runtime has access to sign URLs

## Current Repository Files That Matter

- [`cloudbuild.yaml`](/C:/workspace/learning/progress-tracker/sitepulse-engine/cloudbuild.yaml)
- [`Dockerfile`](/C:/workspace/learning/progress-tracker/sitepulse-engine/Dockerfile)
- [`sitepulse-engine-app/src/main/resources/application.yml`](/C:/workspace/learning/progress-tracker/sitepulse-engine/sitepulse-engine-app/src/main/resources/application.yml)
- [`sitepulse-engine-app/src/main/resources/application-development.yml`](/C:/workspace/learning/progress-tracker/sitepulse-engine/sitepulse-engine-app/src/main/resources/application-development.yml)
