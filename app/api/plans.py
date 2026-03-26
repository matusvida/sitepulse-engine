"""REST API for construction plan management.

Endpoints for uploading a PDF plan, viewing milestones, editing them,
and triggering an AI-powered plan-vs-reality check.
"""

from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

import structlog
from fastapi import APIRouter, HTTPException, UploadFile, File
from pydantic import BaseModel
from sqlalchemy import text

from app.db.engine import get_engine
from app.db.projects import get_project
from app.services.pdf_parser import extract_text
from app.services.llm import parse_plan_milestones, evaluate_milestone
from app.services.storage import download_image_bytes

logger = structlog.get_logger(__name__)
router = APIRouter(prefix="/api")

MAX_PDF_SIZE = 20 * 1024 * 1024  # 20 MB


def _ensure_project(project_id: int) -> None:
    if get_project(project_id) is None:
        raise HTTPException(status_code=404, detail="Project not found")


# ── Upload ────────────────────────────────────────────────────────────────────

@router.post("/projects/{project_id}/plan/upload", status_code=201)
async def upload_plan(project_id: int, file: UploadFile = File(...)):
    """Upload a PDF construction plan.

    Extracts text, sends to GPT-4o for milestone parsing, and stores
    the plan + milestones in the database.
    """
    _ensure_project(project_id)

    if not file.filename or not file.filename.lower().endswith(".pdf"):
        raise HTTPException(status_code=400, detail="Only PDF files are accepted")

    pdf_bytes = await file.read()
    if len(pdf_bytes) > MAX_PDF_SIZE:
        raise HTTPException(status_code=400, detail="File too large (max 20 MB)")

    raw_text = extract_text(pdf_bytes)
    if not raw_text.strip():
        raise HTTPException(status_code=422, detail="Could not extract text from PDF")

    engine = get_engine()

    with engine.begin() as conn:
        result = conn.execute(
            text(
                "INSERT INTO construction_plans (project_id, filename, raw_text, status) "
                "VALUES (:pid, :fname, :txt, 'processing') RETURNING id"
            ),
            {"pid": project_id, "fname": file.filename, "txt": raw_text},
        )
        plan_id = result.scalar_one()

    try:
        milestones = parse_plan_milestones(raw_text)
    except Exception as exc:
        logger.error("plan_parse_failed", plan_id=plan_id, error=str(exc))
        with engine.begin() as conn:
            conn.execute(
                text("UPDATE construction_plans SET status = 'error', updated_at = NOW() WHERE id = :id"),
                {"id": plan_id},
            )
        raise HTTPException(status_code=502, detail=f"LLM parsing failed: {exc}")

    with engine.begin() as conn:
        for m in milestones:
            conn.execute(
                text(
                    "INSERT INTO plan_milestones "
                    "(plan_id, project_id, week_number, title, description, expected_state, status) "
                    "VALUES (:plan_id, :pid, :wk, :title, :desc, :expected, 'not_started')"
                ),
                {
                    "plan_id": plan_id,
                    "pid": project_id,
                    "wk": m.get("week_number", 0),
                    "title": m.get("title", "Untitled"),
                    "desc": m.get("description", ""),
                    "expected": m.get("expected_state", ""),
                },
            )
        conn.execute(
            text("UPDATE construction_plans SET status = 'ready', updated_at = NOW() WHERE id = :id"),
            {"id": plan_id},
        )

    return {
        "planId": plan_id,
        "filename": file.filename,
        "milestonesCreated": len(milestones),
        "status": "ready",
    }


# ── Read ──────────────────────────────────────────────────────────────────────

@router.get("/projects/{project_id}/plan")
async def get_plan(project_id: int):
    """Return the latest construction plan with all its milestones."""
    _ensure_project(project_id)
    engine = get_engine()

    with engine.begin() as conn:
        plan_row = conn.execute(
            text(
                "SELECT id, filename, status, created_at "
                "FROM construction_plans "
                "WHERE project_id = :pid ORDER BY created_at DESC LIMIT 1"
            ),
            {"pid": project_id},
        ).fetchone()

    if plan_row is None:
        return {"plan": None, "milestones": []}

    milestones = _get_milestones(plan_row[0])

    return {
        "plan": {
            "id": plan_row[0],
            "filename": plan_row[1],
            "status": plan_row[2],
            "createdAt": str(plan_row[3]) if plan_row[3] else None,
        },
        "milestones": milestones,
    }


@router.get("/projects/{project_id}/plan/milestones")
async def list_milestones(project_id: int):
    """Return milestones for the latest plan."""
    _ensure_project(project_id)
    engine = get_engine()

    with engine.begin() as conn:
        plan_row = conn.execute(
            text(
                "SELECT id FROM construction_plans "
                "WHERE project_id = :pid ORDER BY created_at DESC LIMIT 1"
            ),
            {"pid": project_id},
        ).fetchone()

    if plan_row is None:
        return []

    return _get_milestones(plan_row[0])


def _get_milestones(plan_id: int) -> List[Dict[str, Any]]:
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT id, week_number, title, description, expected_state, "
                "actual_state, status, checked_at, created_at "
                "FROM plan_milestones WHERE plan_id = :pid ORDER BY week_number ASC"
            ),
            {"pid": plan_id},
        ).fetchall()

    return [
        {
            "id": r[0],
            "weekNumber": r[1],
            "title": r[2],
            "description": r[3],
            "expectedState": r[4],
            "actualState": r[5],
            "status": r[6],
            "checkedAt": str(r[7]) if r[7] else None,
            "createdAt": str(r[8]) if r[8] else None,
        }
        for r in rows
    ]


# ── Update milestone ─────────────────────────────────────────────────────────

class MilestoneUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    expectedState: Optional[str] = None
    status: Optional[str] = None


@router.patch("/projects/{project_id}/plan/milestones/{milestone_id}")
async def update_milestone(project_id: int, milestone_id: int, body: MilestoneUpdate):
    """Manually update a milestone's fields."""
    _ensure_project(project_id)
    engine = get_engine()

    with engine.begin() as conn:
        existing = conn.execute(
            text("SELECT id FROM plan_milestones WHERE id = :mid AND project_id = :pid"),
            {"mid": milestone_id, "pid": project_id},
        ).fetchone()

    if existing is None:
        raise HTTPException(status_code=404, detail="Milestone not found")

    sets: list[str] = []
    params: Dict[str, Any] = {"mid": milestone_id}

    if body.title is not None:
        sets.append("title = :title")
        params["title"] = body.title
    if body.description is not None:
        sets.append("description = :desc")
        params["desc"] = body.description
    if body.expectedState is not None:
        sets.append("expected_state = :expected")
        params["expected"] = body.expectedState
    if body.status is not None:
        valid = ("not_started", "on_track", "delayed", "completed")
        if body.status not in valid:
            raise HTTPException(status_code=400, detail=f"Invalid status. Must be one of: {valid}")
        sets.append("status = :status")
        params["status"] = body.status

    if not sets:
        raise HTTPException(status_code=400, detail="No fields to update")

    with engine.begin() as conn:
        conn.execute(
            text(f"UPDATE plan_milestones SET {', '.join(sets)} WHERE id = :mid"),
            params,
        )

    return {"ok": True}


# ── Plan check (AI evaluation) ───────────────────────────────────────────────

@router.post("/projects/{project_id}/plan/check")
async def trigger_plan_check(project_id: int):
    """Evaluate all non-completed milestones against recent site photos.

    This calls GPT-4o Vision for each milestone and updates their status.
    """
    _ensure_project(project_id)
    engine = get_engine()

    with engine.begin() as conn:
        plan_row = conn.execute(
            text(
                "SELECT id FROM construction_plans "
                "WHERE project_id = :pid AND status = 'ready' "
                "ORDER BY created_at DESC LIMIT 1"
            ),
            {"pid": project_id},
        ).fetchone()

    if plan_row is None:
        raise HTTPException(status_code=404, detail="No ready plan found for this project")

    with engine.begin() as conn:
        milestones = conn.execute(
            text(
                "SELECT id, title, expected_state FROM plan_milestones "
                "WHERE plan_id = :pid AND status != 'completed' "
                "ORDER BY week_number ASC"
            ),
            {"pid": plan_row[0]},
        ).fetchall()

    recent_images = _get_recent_images(project_id, limit=5)
    if not recent_images:
        raise HTTPException(status_code=422, detail="No recent images available for evaluation")

    results = []
    for m_id, m_title, m_expected in milestones:
        try:
            assessment = evaluate_milestone(m_title, m_expected or "", recent_images)
            new_status = assessment.get("status", "not_started")
            actual_state = assessment.get("actual_state", "")

            with engine.begin() as conn:
                conn.execute(
                    text(
                        "UPDATE plan_milestones "
                        "SET status = :status, actual_state = :actual, checked_at = NOW() "
                        "WHERE id = :mid"
                    ),
                    {"status": new_status, "actual": actual_state, "mid": m_id},
                )

            results.append({
                "milestoneId": m_id,
                "title": m_title,
                "status": new_status,
                "actualState": actual_state,
            })
        except Exception as exc:
            logger.error("milestone_check_failed", milestone_id=m_id, error=str(exc))
            results.append({
                "milestoneId": m_id,
                "title": m_title,
                "status": "error",
                "error": str(exc),
            })

    return {"milestonesChecked": len(results), "results": results}


def _get_recent_images(project_id: int, limit: int = 5) -> List[bytes]:
    """Fetch the most recent site images as raw bytes from MinIO."""
    from app.core import get_settings

    engine = get_engine()
    cfg = get_settings()

    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT bucket, key FROM images "
                "WHERE project_id = :pid AND status = 'DONE' "
                "ORDER BY captured_at DESC LIMIT :lim"
            ),
            {"pid": project_id, "lim": limit},
        ).fetchall()

    images: List[bytes] = []
    for bucket, key in rows:
        try:
            data = download_image_bytes(bucket, key)
            images.append(data)
        except Exception as exc:
            logger.warning("image_download_failed", key=key, error=str(exc))

    return images
