"""Image record repository — CRUD and status transitions."""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from sqlalchemy import text

from app.db.engine import get_engine
from app.db.tables import images


def insert_image_record(
    bucket: str,
    key: str,
    status: str = "DONE",
    project_id: Optional[int] = None,
    camera_id: Optional[int] = None,
    captured_at: Optional[datetime] = None,
) -> int:
    """Insert a row into ``images`` and return the generated id."""
    engine = get_engine()
    now = datetime.now(timezone.utc)
    with engine.begin() as conn:
        result = conn.execute(
            images.insert()
            .values(
                bucket=bucket,
                key=key,
                status=status,
                project_id=project_id,
                camera_id=camera_id,
                captured_at=captured_at,
                created_at=now,
                updated_at=now,
            )
            .returning(images.c.id)
        )
        return result.scalar_one()


def fetch_new_images(limit: int = 10) -> List[Dict[str, Any]]:
    """Return up to *limit* images with status='NEW', atomically setting
    them to 'PROCESSING'."""
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "UPDATE images SET status='PROCESSING', updated_at=NOW() "
                "WHERE id IN ("
                "  SELECT id FROM images WHERE status='NEW' ORDER BY id LIMIT :lim "
                "  FOR UPDATE SKIP LOCKED"
                ") RETURNING id, bucket, key, project_id, camera_id"
            ),
            {"lim": limit},
        ).fetchall()
    return [
        {"id": r[0], "bucket": r[1], "key": r[2], "project_id": r[3], "camera_id": r[4]}
        for r in rows
    ]


def mark_done(image_id: int) -> None:
    engine = get_engine()
    with engine.begin() as conn:
        conn.execute(
            text("UPDATE images SET status='DONE', updated_at=NOW() WHERE id=:id"),
            {"id": image_id},
        )


def mark_failed(image_id: int, reason: str) -> None:
    engine = get_engine()
    with engine.begin() as conn:
        conn.execute(
            text("UPDATE images SET status='FAILED', updated_at=NOW() WHERE id=:id"),
            {"id": image_id},
        )


def image_key_exists(bucket: str, key: str) -> bool:
    """Check if an image with this bucket+key already exists."""
    engine = get_engine()
    with engine.begin() as conn:
        row = conn.execute(
            text("SELECT 1 FROM images WHERE bucket = :b AND key = :k LIMIT 1"),
            {"b": bucket, "k": key},
        ).fetchone()
    return row is not None
