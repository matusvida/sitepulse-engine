"""REST API for AI-generated progress reports."""

from __future__ import annotations

from datetime import date, timedelta
from typing import Any, Dict, List

import structlog
from fastapi import APIRouter, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy import text

from app.core import get_settings
from app.db.engine import get_engine
from app.db.projects import get_project
from app.services.llm import generate_progress_report, _encode_image
from app.services.storage import download_image_bytes

logger = structlog.get_logger(__name__)
router = APIRouter(prefix="/api")


def _ensure_project(project_id: int) -> None:
    if get_project(project_id) is None:
        raise HTTPException(status_code=404, detail="Project not found")


# ── Request models ────────────────────────────────────────────────────────────

class GenerateReportRequest(BaseModel):
    dateFrom: str
    dateTo: str


# ── Generate ──────────────────────────────────────────────────────────────────

@router.post("/projects/{project_id}/reports/generate", status_code=201)
async def api_generate_report(project_id: int, body: GenerateReportRequest):
    """Generate an AI progress report for the given date range.

    Gathers site photos + metrics + plan milestones and calls GPT-4o Vision.
    """
    _ensure_project(project_id)

    try:
        d_from = date.fromisoformat(body.dateFrom)
        d_to = date.fromisoformat(body.dateTo)
    except ValueError:
        raise HTTPException(status_code=400, detail="Invalid date format, use YYYY-MM-DD")

    if d_from > d_to:
        raise HTTPException(status_code=400, detail="dateFrom must be <= dateTo")

    image_data = _gather_images(project_id, d_from, d_to, max_images=8)
    metrics_ctx = _build_metrics_context(project_id, d_from, d_to)
    milestones_ctx = _build_milestones_context(project_id)

    if not image_data:
        raise HTTPException(
            status_code=422,
            detail="No images found in the given date range. Run a sync first.",
        )

    try:
        report_md = generate_progress_report(image_data, metrics_ctx, milestones_ctx)
    except Exception as exc:
        logger.error("report_generation_failed", error=str(exc))
        raise HTTPException(status_code=502, detail=f"Report generation failed: {exc}")

    summary = report_md[:300].split("\n")[0] if report_md else ""

    engine = get_engine()
    cfg = get_settings()
    with engine.begin() as conn:
        result = conn.execute(
            text(
                "INSERT INTO progress_reports "
                "(project_id, report_type, content_md, summary, "
                " date_range_start, date_range_end, image_count, model_used) "
                "VALUES (:pid, 'custom', :content, :summary, :d1, :d2, :img_cnt, :model) "
                "RETURNING id, created_at"
            ),
            {
                "pid": project_id,
                "content": report_md,
                "summary": summary,
                "d1": d_from.isoformat(),
                "d2": d_to.isoformat(),
                "img_cnt": len(image_data),
                "model": cfg.openai_model,
            },
        )
        row = result.fetchone()

    return {
        "id": row[0],
        "projectId": str(project_id),
        "reportType": "custom",
        "summary": summary,
        "contentMd": report_md,
        "dateRangeStart": body.dateFrom,
        "dateRangeEnd": body.dateTo,
        "imageCount": len(image_data),
        "modelUsed": cfg.openai_model,
        "createdAt": str(row[1]) if row[1] else None,
    }


# ── List reports ──────────────────────────────────────────────────────────────

@router.get("/projects/{project_id}/reports")
async def api_list_reports(
    project_id: int,
    limit: int = Query(20, ge=1, le=100),
    offset: int = Query(0, ge=0),
):
    """List past progress reports, newest first."""
    _ensure_project(project_id)
    engine = get_engine()

    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT id, report_type, summary, date_range_start, date_range_end, "
                "image_count, model_used, created_at "
                "FROM progress_reports "
                "WHERE project_id = :pid "
                "ORDER BY created_at DESC LIMIT :lim OFFSET :off"
            ),
            {"pid": project_id, "lim": limit, "off": offset},
        ).fetchall()

    return [
        {
            "id": r[0],
            "reportType": r[1],
            "summary": r[2],
            "dateRangeStart": str(r[3]) if r[3] else None,
            "dateRangeEnd": str(r[4]) if r[4] else None,
            "imageCount": r[5],
            "modelUsed": r[6],
            "createdAt": str(r[7]) if r[7] else None,
        }
        for r in rows
    ]


# ── Single report detail ─────────────────────────────────────────────────────

@router.get("/projects/{project_id}/reports/{report_id}")
async def api_get_report(project_id: int, report_id: int):
    """Get a single report with full markdown content."""
    _ensure_project(project_id)
    engine = get_engine()

    with engine.begin() as conn:
        row = conn.execute(
            text(
                "SELECT id, report_type, content_md, summary, "
                "date_range_start, date_range_end, image_count, model_used, created_at "
                "FROM progress_reports "
                "WHERE id = :rid AND project_id = :pid"
            ),
            {"rid": report_id, "pid": project_id},
        ).fetchone()

    if row is None:
        raise HTTPException(status_code=404, detail="Report not found")

    return {
        "id": row[0],
        "projectId": str(project_id),
        "reportType": row[1],
        "contentMd": row[2],
        "summary": row[3],
        "dateRangeStart": str(row[4]) if row[4] else None,
        "dateRangeEnd": str(row[5]) if row[5] else None,
        "imageCount": row[6],
        "modelUsed": row[7],
        "createdAt": str(row[8]) if row[8] else None,
    }


# ── Helpers ───────────────────────────────────────────────────────────────────

def _gather_images(
    project_id: int,
    date_from: date,
    date_to: date,
    max_images: int = 8,
) -> List[Dict[str, Any]]:
    """Sample images spread across the date range and encode as base64."""
    engine = get_engine()

    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT bucket, key, DATE(captured_at) AS d "
                "FROM images "
                "WHERE project_id = :pid AND status = 'DONE' "
                "  AND captured_at >= :d1 AND captured_at < :d2 "
                "ORDER BY captured_at ASC"
            ),
            {
                "pid": project_id,
                "d1": date_from.isoformat(),
                "d2": (date_to + timedelta(days=1)).isoformat(),
            },
        ).fetchall()

    if not rows:
        return []

    step = max(1, len(rows) // max_images)
    sampled = rows[::step][:max_images]

    result: List[Dict[str, Any]] = []
    for bucket, key, captured_date in sampled:
        try:
            img_bytes = download_image_bytes(bucket, key)
            result.append({
                "date": str(captured_date),
                "b64": _encode_image(img_bytes),
            })
        except Exception as exc:
            logger.warning("report_image_download_failed", key=key, error=str(exc))

    return result


def _build_metrics_context(project_id: int, date_from: date, date_to: date) -> str:
    """Build a text summary of daily and weekly metrics for the report prompt."""
    engine = get_engine()
    lines: list[str] = []

    with engine.begin() as conn:
        daily_rows = conn.execute(
            text(
                "SELECT date, people_count, vehicle_count, active_hours "
                "FROM daily_metrics "
                "WHERE project_id = :pid AND date >= :d1 AND date <= :d2 "
                "ORDER BY date ASC"
            ),
            {"pid": project_id, "d1": date_from.isoformat(), "d2": date_to.isoformat()},
        ).fetchall()

    if daily_rows:
        lines.append("### Daily Metrics")
        for r in daily_rows:
            lines.append(
                f"- {r[0]}: people={r[1] or 0}, vehicles={r[2] or 0}, "
                f"active_hours={r[3] or 0:.1f}"
            )

    with engine.begin() as conn:
        weekly_rows = conn.execute(
            text(
                "SELECT week_start, progress_delta, activity_index, active_hours, risk_level "
                "FROM weekly_metrics "
                "WHERE project_id = :pid AND week_start >= :d1 AND week_start <= :d2 "
                "ORDER BY week_start ASC"
            ),
            {"pid": project_id, "d1": date_from.isoformat(), "d2": date_to.isoformat()},
        ).fetchall()

    if weekly_rows:
        lines.append("\n### Weekly Metrics")
        for r in weekly_rows:
            lines.append(
                f"- Week of {r[0]}: progress_delta={r[1] or 0:.1f}%, "
                f"activity_index={r[2] or 0:.1f}, active_hours={r[3] or 0:.1f}h, "
                f"risk={r[4] or 'N/A'}"
            )

    return "\n".join(lines) if lines else "No metrics data available for this period."


def _build_milestones_context(project_id: int) -> str:
    """Build a text summary of current plan milestones."""
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
        return "No construction plan uploaded."

    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT week_number, title, expected_state, actual_state, status "
                "FROM plan_milestones "
                "WHERE plan_id = :pid ORDER BY week_number ASC"
            ),
            {"pid": plan_row[0]},
        ).fetchall()

    if not rows:
        return "Plan uploaded but no milestones extracted."

    lines = ["### Construction Plan Milestones"]
    for r in rows:
        line = f"- Week {r[0]}: {r[1]} (status: {r[4]})"
        if r[2]:
            line += f"\n  Expected: {r[2]}"
        if r[3]:
            line += f"\n  Actual: {r[3]}"
        lines.append(line)

    return "\n".join(lines)
