"""Project and camera repository — CRUD helpers."""

from __future__ import annotations

import json as _json
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from sqlalchemy import text

from app.db.engine import get_engine
from app.db.tables import cameras, projects


# ── Projects ─────────────────────────────────────────────────────────────────

def create_project(
    name: str,
    location: Optional[str] = None,
    dropbox_path: Optional[str] = None,
) -> Dict[str, Any]:
    engine = get_engine()
    with engine.begin() as conn:
        row = conn.execute(
            projects.insert()
            .values(
                name=name,
                location=location,
                dropbox_path=dropbox_path,
                created_at=datetime.now(timezone.utc),
            )
            .returning(
                projects.c.id,
                projects.c.name,
                projects.c.location,
                projects.c.dropbox_path,
                projects.c.created_at,
            )
        ).fetchone()
    return _project_row_to_dict(row)


def get_project(project_id: int) -> Optional[Dict[str, Any]]:
    engine = get_engine()
    with engine.begin() as conn:
        row = conn.execute(
            text(
                "SELECT p.id, p.name, p.location, p.dropbox_path, p.created_at, "
                "  (SELECT COUNT(*) FROM cameras c WHERE c.project_id = p.id) AS camera_count, "
                "  (SELECT MAX(i.captured_at) FROM images i WHERE i.project_id = p.id AND i.status = 'DONE') AS last_snapshot_at "
                "FROM projects p WHERE p.id = :pid"
            ),
            {"pid": project_id},
        ).fetchone()
    if row is None:
        return None
    return {
        "id": str(row[0]),
        "name": row[1],
        "location": row[2] or "",
        "dropbox_path": row[3],
        "camera_count": row[5],
        "last_snapshot_at": row[6].isoformat() if row[6] else None,
        "created_at": row[4].isoformat() if row[4] else None,
    }


def list_projects() -> List[Dict[str, Any]]:
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT p.id, p.name, p.location, p.dropbox_path, p.created_at, "
                "  (SELECT COUNT(*) FROM cameras c WHERE c.project_id = p.id) AS camera_count, "
                "  (SELECT MAX(i.captured_at) FROM images i WHERE i.project_id = p.id AND i.status = 'DONE') AS last_snapshot_at "
                "FROM projects p ORDER BY p.id"
            )
        ).fetchall()
    return [
        {
            "id": str(r[0]),
            "name": r[1],
            "location": r[2] or "",
            "dropbox_path": r[3],
            "camera_count": r[5],
            "last_snapshot_at": r[6].isoformat() if r[6] else None,
            "created_at": r[4].isoformat() if r[4] else None,
        }
        for r in rows
    ]


def update_project(
    project_id: int,
    name: Optional[str] = None,
    location: Optional[str] = None,
    dropbox_path: Optional[str] = None,
) -> Optional[Dict[str, Any]]:
    updates = {}
    if name is not None:
        updates["name"] = name
    if location is not None:
        updates["location"] = location
    if dropbox_path is not None:
        updates["dropbox_path"] = dropbox_path
    if not updates:
        return get_project(project_id)

    engine = get_engine()
    set_clause = ", ".join(f"{k} = :{k}" for k in updates)
    updates["pid"] = project_id
    with engine.begin() as conn:
        conn.execute(
            text(f"UPDATE projects SET {set_clause} WHERE id = :pid"),
            updates,
        )
    return get_project(project_id)


# ── Cameras ──────────────────────────────────────────────────────────────────

def create_camera(
    project_id: int,
    name: str,
    key_prefix: Optional[str] = None,
    roi_polygon: Optional[list] = None,
    drop_outside: bool = True,
) -> Dict[str, Any]:
    engine = get_engine()
    with engine.begin() as conn:
        row = conn.execute(
            cameras.insert()
            .values(
                project_id=project_id,
                name=name,
                key_prefix=key_prefix,
                roi_polygon=roi_polygon,
                drop_outside=drop_outside,
                created_at=datetime.now(timezone.utc),
            )
            .returning(
                cameras.c.id,
                cameras.c.project_id,
                cameras.c.name,
                cameras.c.roi_polygon,
                cameras.c.drop_outside,
                cameras.c.key_prefix,
                cameras.c.created_at,
            )
        ).fetchone()
    return _camera_row_to_dict(row)


def get_camera(camera_id: int) -> Optional[Dict[str, Any]]:
    """Fetch a camera row by id."""
    engine = get_engine()
    with engine.begin() as conn:
        row = conn.execute(
            text(
                "SELECT id, project_id, name, roi_polygon, drop_outside, key_prefix, created_at "
                "FROM cameras WHERE id = :cid"
            ),
            {"cid": camera_id},
        ).fetchone()
    if row is None:
        return None
    return _camera_row_to_dict(row)


def get_cameras_for_project(project_id: int) -> List[Dict[str, Any]]:
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT id, project_id, name, roi_polygon, drop_outside, key_prefix, created_at "
                "FROM cameras WHERE project_id = :pid ORDER BY id"
            ),
            {"pid": project_id},
        ).fetchall()
    return [_camera_row_to_dict(r) for r in rows]


def find_camera_by_key(project_id: int, key: str) -> Optional[Dict[str, Any]]:
    """Find a camera whose key_prefix matches the given S3 key."""
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT id, project_id, name, roi_polygon, drop_outside, key_prefix, created_at "
                "FROM cameras WHERE project_id = :pid AND key_prefix IS NOT NULL "
                "ORDER BY length(key_prefix) DESC"
            ),
            {"pid": project_id},
        ).fetchall()
    for row in rows:
        prefix = row[5]
        if prefix and key.startswith(prefix):
            return _camera_row_to_dict(row)
    return None


def update_camera_roi(
    camera_id: int,
    roi_polygon: Optional[list] = None,
    drop_outside: Optional[bool] = None,
) -> Optional[Dict[str, Any]]:
    updates: Dict[str, Any] = {}
    if roi_polygon is not None:
        updates["roi"] = _json.dumps(roi_polygon)
    if drop_outside is not None:
        updates["drop"] = drop_outside
    if not updates:
        return None

    engine = get_engine()
    parts = []
    params: Dict[str, Any] = {"cid": camera_id}
    if "roi" in updates:
        parts.append("roi_polygon = :roi::jsonb")
        params["roi"] = updates["roi"]
    if "drop" in updates:
        parts.append("drop_outside = :drop")
        params["drop"] = updates["drop"]

    with engine.begin() as conn:
        conn.execute(text(f"UPDATE cameras SET {', '.join(parts)} WHERE id = :cid"), params)
        row = conn.execute(
            text(
                "SELECT id, project_id, name, roi_polygon, drop_outside, key_prefix, created_at "
                "FROM cameras WHERE id = :cid"
            ),
            {"cid": camera_id},
        ).fetchone()
    if row is None:
        return None
    return _camera_row_to_dict(row)


# ── Private helpers ──────────────────────────────────────────────────────────

def _project_row_to_dict(row) -> Dict[str, Any]:
    return {
        "id": str(row[0]),
        "name": row[1],
        "location": row[2] or "",
        "dropbox_path": row[3],
        "created_at": row[4].isoformat() if row[4] else None,
    }


def _camera_row_to_dict(row) -> Dict[str, Any]:
    return {
        "id": row[0],
        "project_id": row[1],
        "name": row[2],
        "roi_polygon": row[3],
        "drop_outside": row[4],
        "key_prefix": row[5],
        "created_at": row[6].isoformat() if row[6] else None,
    }
