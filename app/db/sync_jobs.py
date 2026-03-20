"""Sync job repository."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import text

from app.db.engine import get_engine
from app.db.tables import sync_jobs


def create_sync_job(project_id: int) -> int:
    """Create a sync_jobs record with status=RUNNING, return its id."""
    engine = get_engine()
    now = datetime.now(timezone.utc)
    with engine.begin() as conn:
        result = conn.execute(
            sync_jobs.insert()
            .values(
                project_id=project_id,
                status="RUNNING",
                images_found=0,
                images_synced=0,
                started_at=now,
            )
            .returning(sync_jobs.c.id)
        )
        return result.scalar_one()


def finish_sync_job(
    job_id: int,
    status: str,
    images_found: int,
    images_synced: int,
    error: Optional[str] = None,
) -> None:
    """Update a sync_jobs record with final counts and status."""
    engine = get_engine()
    now = datetime.now(timezone.utc)
    with engine.begin() as conn:
        conn.execute(
            text(
                "UPDATE sync_jobs SET status = :s, images_found = :found, "
                "images_synced = :synced, error = :err, finished_at = :fin "
                "WHERE id = :jid"
            ),
            {
                "s": status, "found": images_found, "synced": images_synced,
                "err": error, "fin": now, "jid": job_id,
            },
        )
