"""REST API for projects, cameras, metrics, alerts, sync, and heatmap.

All endpoints are prefixed with ``/api`` and produce camelCase JSON
matching the frontend TypeScript interfaces.
"""

from __future__ import annotations

from datetime import date, timedelta
from typing import Any, Dict, List, Optional

import structlog
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy import text

from app.db.alerts import get_alert, list_alerts, update_alert_status
from app.db.engine import get_engine
from app.db.projects import (
    create_camera,
    create_project,
    get_cameras_for_project,
    get_project,
    list_projects,
    update_camera_roi,
    update_project,
)
from app.db.sync_jobs import get_latest_sync_job

logger = structlog.get_logger(__name__)
router = APIRouter(prefix="/api")


# ── Request bodies ───────────────────────────────────────────────────────────

class ProjectCreate(BaseModel):
    name: str
    location: str = ""
    dropbox_path: Optional[str] = None


class ProjectUpdate(BaseModel):
    name: Optional[str] = None
    location: Optional[str] = None
    dropbox_path: Optional[str] = None


class CameraCreate(BaseModel):
    name: str
    key_prefix: Optional[str] = None
    roi_polygon: Optional[list] = None
    drop_outside: bool = True


class CameraUpdate(BaseModel):
    roi_polygon: Optional[list] = None
    drop_outside: Optional[bool] = None


class AlertStatusUpdate(BaseModel):
    status: str


# ── Projects ─────────────────────────────────────────────────────────────────

@router.get("/projects")
async def api_list_projects():
    projects = list_projects()
    return [_format_project(p) for p in projects]


@router.get("/projects/{project_id}")
async def api_get_project(project_id: int):
    project = get_project(project_id)
    if project is None:
        raise HTTPException(status_code=404, detail="Project not found")
    return _format_project(project)


@router.post("/projects", status_code=201)
async def api_create_project(body: ProjectCreate):
    project = create_project(
        name=body.name,
        location=body.location or None,
        dropbox_path=body.dropbox_path,
    )
    full = get_project(int(project["id"]))
    return _format_project(full or project)


@router.patch("/projects/{project_id}")
async def api_update_project(project_id: int, body: ProjectUpdate):
    existing = get_project(project_id)
    if existing is None:
        raise HTTPException(status_code=404, detail="Project not found")
    updated = update_project(
        project_id,
        name=body.name,
        location=body.location,
        dropbox_path=body.dropbox_path,
    )
    return _format_project(updated)


# ── Cameras ──────────────────────────────────────────────────────────────────

@router.get("/projects/{project_id}/cameras")
async def api_list_cameras(project_id: int):
    _ensure_project(project_id)
    return get_cameras_for_project(project_id)


@router.post("/projects/{project_id}/cameras", status_code=201)
async def api_create_camera(project_id: int, body: CameraCreate):
    _ensure_project(project_id)
    return create_camera(
        project_id=project_id,
        name=body.name,
        key_prefix=body.key_prefix,
        roi_polygon=body.roi_polygon,
        drop_outside=body.drop_outside,
    )


@router.patch("/projects/{project_id}/cameras/{cam_id}")
async def api_update_camera(project_id: int, cam_id: int, body: CameraUpdate):
    _ensure_project(project_id)
    updated = update_camera_roi(
        cam_id,
        roi_polygon=body.roi_polygon,
        drop_outside=body.drop_outside,
    )
    if updated is None:
        raise HTTPException(status_code=404, detail="Camera not found")
    return updated


# ── Metrics ──────────────────────────────────────────────────────────────────

@router.get("/projects/{project_id}/metrics/daily")
async def api_daily_metrics(project_id: int, days: int = Query(28, ge=1, le=365)):
    _ensure_project(project_id)
    cutoff = date.today() - timedelta(days=days)
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT date, people_count, vehicle_count, active_hours "
                "FROM daily_metrics "
                "WHERE project_id = :pid AND date >= :cutoff "
                "ORDER BY date ASC"
            ),
            {"pid": project_id, "cutoff": cutoff},
        ).fetchall()
    return [
        {
            "date": str(r[0]),
            "peopleCount": r[1] or 0,
            "vehicleCount": r[2] or 0,
            "activeHours": r[3] or 0.0,
        }
        for r in rows
    ]


@router.get("/projects/{project_id}/metrics/weekly")
async def api_weekly_metrics(project_id: int, weeks: int = Query(12, ge=1, le=52)):
    _ensure_project(project_id)
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT week_start, progress_delta, activity_index, active_hours, risk_level "
                "FROM weekly_metrics "
                "WHERE project_id = :pid "
                "ORDER BY week_start DESC LIMIT :n"
            ),
            {"pid": project_id, "n": weeks},
        ).fetchall()
    return [
        {
            "weekStart": str(r[0]),
            "progressDelta": r[1] or 0.0,
            "activityIndex": r[2] or 0.0,
            "activeHours": r[3] or 0.0,
            "riskLevel": r[4] or "Low",
        }
        for r in reversed(rows)
    ]


# ── Alerts ───────────────────────────────────────────────────────────────────

@router.get("/projects/{project_id}/alerts")
async def api_list_alerts(
    project_id: int,
    type: Optional[str] = None,
    severity: Optional[str] = None,
    status: Optional[str] = None,
):
    _ensure_project(project_id)
    return list_alerts(
        project_id,
        alert_type=type,
        severity=severity,
        status=status,
    )


@router.patch("/projects/{project_id}/alerts/{alert_id}")
async def api_update_alert(project_id: int, alert_id: int, body: AlertStatusUpdate):
    _ensure_project(project_id)
    existing = get_alert(alert_id)
    if existing is None:
        raise HTTPException(status_code=404, detail="Alert not found")
    if body.status not in ("open", "acknowledged", "resolved"):
        raise HTTPException(status_code=400, detail="Invalid status. Must be open, acknowledged, or resolved")
    updated = update_alert_status(alert_id, body.status)
    return updated


# ── Sync ─────────────────────────────────────────────────────────────────────

@router.get("/projects/{project_id}/sync/status")
async def api_sync_status(project_id: int):
    _ensure_project(project_id)
    job = get_latest_sync_job(project_id)
    if job is None:
        return {"status": "never_run", "message": "No sync jobs have been run for this project"}
    return job


@router.post("/projects/{project_id}/sync/trigger", status_code=202)
async def api_trigger_sync(project_id: int):
    project = get_project(project_id)
    if project is None:
        raise HTTPException(status_code=404, detail="Project not found")
    if not project.get("dropbox_path"):
        raise HTTPException(status_code=400, detail="Project has no dropbox_path configured")

    import threading
    from app.services.sync import sync_project
    thread = threading.Thread(target=sync_project, args=(project,), daemon=True)
    thread.start()
    logger.info("sync_triggered", project_id=project_id)

    return {"status": "accepted", "message": "Sync job started in background"}


# ── Activity heatmap ─────────────────────────────────────────────────────────

@router.get("/projects/{project_id}/activity/heatmap")
async def api_activity_heatmap(project_id: int):
    """Hourly activity by day of week (Mon=0 .. Sun=6, hour 0..23).

    Returns a grid of detection counts bucketed by (day_of_week, hour).
    """
    _ensure_project(project_id)
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT EXTRACT(DOW FROM i.captured_at)::int AS dow, "
                "       EXTRACT(HOUR FROM i.captured_at)::int AS hr, "
                "       COUNT(d.id) AS cnt "
                "FROM detections d "
                "JOIN images i ON d.image_id = i.id "
                "WHERE d.project_id = :pid AND i.captured_at IS NOT NULL "
                "GROUP BY dow, hr "
                "ORDER BY dow, hr"
            ),
            {"pid": project_id},
        ).fetchall()

    # Postgres DOW: 0=Sunday, 1=Monday, ... 6=Saturday
    # Frontend expects: 0=Monday, ... 6=Sunday
    grid: List[Dict[str, Any]] = []
    for r in rows:
        pg_dow = r[0]
        frontend_dow = (pg_dow - 1) % 7  # shift Sunday(0) → 6, Monday(1) → 0
        grid.append({
            "dayOfWeek": frontend_dow,
            "hour": r[1],
            "count": r[2],
        })

    return grid


# ── Helpers ──────────────────────────────────────────────────────────────────

def _ensure_project(project_id: int) -> None:
    if get_project(project_id) is None:
        raise HTTPException(status_code=404, detail="Project not found")


def _format_project(p: Dict[str, Any]) -> Dict[str, Any]:
    """Shape a project dict to match the frontend Project interface."""
    return {
        "id": str(p["id"]),
        "name": p["name"],
        "location": p.get("location", ""),
        "coveragePercent": 0,
        "cameraCount": p.get("camera_count", 0),
        "lastSnapshotAt": p.get("last_snapshot_at") or "",
        "dropboxPath": p.get("dropbox_path"),
        "createdAt": p.get("created_at"),
    }
