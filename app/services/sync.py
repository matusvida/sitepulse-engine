"""Dropbox → MinIO sync orchestrator.

For each project with a ``dropbox_path`` (a Dropbox shared link URL),
discovers new images, uploads them to MinIO, and creates DB records with
status=NEW so the detection worker picks them up.

Dropbox shared folder structure expected::

    <shared-link>/  (may include subfolder like /Tower)
        2026-02-23/
            2026-02-23 11:47:43.jpg
        2026-02-24/
            ...

The S3 key is built as ``<date>/<filename>`` under the default bucket.
"""

from __future__ import annotations

import re
from datetime import datetime, timezone
from typing import Optional

import structlog

from app.core import get_settings
from app.db.images import image_key_exists, insert_image_record
from app.db.projects import find_camera_by_key, list_projects
from app.db.sync_jobs import create_sync_job, finish_sync_job
from app.services.dropbox import (
    DropboxSyncError,
    download_file,
    list_files,
    list_subfolders,
)
from app.services.storage import S3UploadError, upload_bytes

logger = structlog.get_logger(__name__)

_DATE_RE = re.compile(r"^(\d{4})[-_]?(\d{2})[-_]?(\d{2})$")


def _parse_date_folder(name: str) -> Optional[datetime]:
    """Try to parse a folder name as a date."""
    m = _DATE_RE.match(name)
    if not m:
        return None
    try:
        return datetime(int(m.group(1)), int(m.group(2)), int(m.group(3)), tzinfo=timezone.utc)
    except ValueError:
        return None


def _parse_captured_at(filename: str, folder_date: datetime) -> datetime:
    """Try to extract a timestamp from the filename, fall back to folder date."""
    m = re.search(r"(\d{4})-(\d{2})-(\d{2})[_ ](\d{2})[_:](\d{2})[_:](\d{2})", filename)
    if m:
        try:
            return datetime(
                int(m.group(1)), int(m.group(2)), int(m.group(3)),
                int(m.group(4)), int(m.group(5)), int(m.group(6)),
                tzinfo=timezone.utc,
            )
        except ValueError:
            pass

    m = re.search(r"(\d{4})(\d{2})(\d{2})[_](\d{2})(\d{2})(\d{2})", filename)
    if m:
        try:
            return datetime(
                int(m.group(1)), int(m.group(2)), int(m.group(3)),
                int(m.group(4)), int(m.group(5)), int(m.group(6)),
                tzinfo=timezone.utc,
            )
        except ValueError:
            pass

    return folder_date


def sync_project(project: dict) -> dict:
    """Sync a single project from Dropbox to MinIO + DB.

    Returns a summary dict with images_found, images_synced, errors.
    """
    project_id = int(project["id"])
    dropbox_url = project["dropbox_path"]
    cfg = get_settings()
    bucket = cfg.minio_bucket_default

    job_id = create_sync_job(project_id)
    images_found = 0
    images_synced = 0
    errors: list[str] = []

    logger.info("sync_start", project_id=project_id, dropbox_url=dropbox_url)

    try:
        date_folders = list_subfolders(dropbox_url)
    except DropboxSyncError as exc:
        msg = f"Failed to list Dropbox folder: {exc}"
        logger.error("sync_list_failed", project_id=project_id, error=msg)
        finish_sync_job(job_id, "FAILED", 0, 0, error=msg)
        return {"images_found": 0, "images_synced": 0, "errors": [msg]}

    for folder_name in date_folders:
        folder_date = _parse_date_folder(folder_name)
        if folder_date is None:
            logger.debug("sync_skip_folder", folder=folder_name, reason="not a date folder")
            continue

        try:
            files = list_files(dropbox_url, folder_name)
        except DropboxSyncError as exc:
            errors.append(f"List {folder_name}: {exc}")
            continue

        for file_entry in files:
            images_found += 1
            s3_key = f"{folder_name}/{file_entry.name}"

            if image_key_exists(bucket, s3_key):
                continue

            try:
                data = download_file(dropbox_url, file_entry.path)
            except DropboxSyncError as exc:
                errors.append(f"Download {file_entry.name}: {exc}")
                continue

            try:
                upload_bytes(bucket, s3_key, data)
            except S3UploadError as exc:
                errors.append(f"Upload {s3_key}: {exc}")
                continue

            captured_at = _parse_captured_at(file_entry.name, folder_date)
            cam = find_camera_by_key(project_id, s3_key)
            camera_id = cam["id"] if cam else None

            insert_image_record(
                bucket=bucket,
                key=s3_key,
                status="NEW",
                project_id=project_id,
                camera_id=camera_id,
                captured_at=captured_at,
            )
            images_synced += 1

    status = "DONE"
    if images_found == 0 and errors:
        status = "FAILED"
    error_text = "; ".join(errors) if errors else None

    finish_sync_job(job_id, status, images_found, images_synced, error=error_text)

    logger.info(
        "sync_complete",
        project_id=project_id,
        images_found=images_found,
        images_synced=images_synced,
        errors=len(errors),
    )
    return {"images_found": images_found, "images_synced": images_synced, "errors": errors}


def run_sync() -> None:
    """Sync all projects that have a dropbox_path configured."""
    projects = list_projects()
    for project in projects:
        if not project.get("dropbox_path"):
            continue
        try:
            sync_project(project)
        except Exception:
            logger.exception("sync_project_failed", project_id=project["id"])
