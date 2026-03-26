"""Weekly plan-vs-reality tracker.

Evaluates non-completed milestones against recent site photos using
GPT-4o Vision and generates schedule alerts when delays are detected.
"""

from __future__ import annotations

from typing import Any, Dict, List

import structlog
from sqlalchemy import text

from app.db.alerts import create_alert, has_open_alert
from app.db.engine import get_engine
from app.services.llm import evaluate_milestone
from app.services.storage import download_image_bytes

logger = structlog.get_logger(__name__)


def check_plan_progress(project_id: int) -> List[Dict[str, Any]]:
    """Evaluate all non-completed milestones for a project.

    For each milestone, fetches recent site images and asks GPT-4o
    to assess whether the milestone is on track.  Updates the milestone
    status and actual_state in the database.

    Returns a list of evaluation results.
    """
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
        logger.info("plan_tracker_skip", project_id=project_id, reason="no_plan")
        return []

    with engine.begin() as conn:
        milestones = conn.execute(
            text(
                "SELECT id, title, expected_state FROM plan_milestones "
                "WHERE plan_id = :pid AND status != 'completed' "
                "ORDER BY week_number ASC"
            ),
            {"pid": plan_row[0]},
        ).fetchall()

    if not milestones:
        logger.info("plan_tracker_skip", project_id=project_id, reason="all_completed")
        return []

    recent_images = _get_recent_images(project_id, limit=5)
    if not recent_images:
        logger.warning("plan_tracker_skip", project_id=project_id, reason="no_images")
        return []

    results: List[Dict[str, Any]] = []

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
                "milestone_id": m_id,
                "title": m_title,
                "status": new_status,
                "actual_state": actual_state,
            })
            logger.info(
                "plan_tracker_evaluated",
                milestone_id=m_id,
                title=m_title,
                status=new_status,
            )
        except Exception as exc:
            logger.error("plan_tracker_eval_failed", milestone_id=m_id, error=str(exc))
            results.append({
                "milestone_id": m_id,
                "title": m_title,
                "status": "error",
                "error": str(exc),
            })

    return results


def generate_schedule_alerts(project_id: int) -> int:
    """Create alerts for milestones that are marked as delayed.

    Skips if there is already an open schedule alert for the project.
    Returns the number of new alerts created.
    """
    engine = get_engine()

    with engine.begin() as conn:
        delayed = conn.execute(
            text(
                "SELECT pm.id, pm.week_number, pm.title, pm.actual_state "
                "FROM plan_milestones pm "
                "JOIN construction_plans cp ON cp.id = pm.plan_id "
                "WHERE pm.project_id = :pid "
                "  AND pm.status = 'delayed' "
                "  AND cp.status = 'ready' "
                "ORDER BY pm.week_number ASC"
            ),
            {"pid": project_id},
        ).fetchall()

    if not delayed:
        return 0

    created = 0
    for m_id, week_num, title, actual_state in delayed:
        alert_key = f"schedule_milestone_{m_id}"
        if has_open_alert(project_id, "schedule"):
            continue

        summary = f"Schedule delay: Week {week_num} — {title}"
        details = (
            f"Milestone '{title}' (Week {week_num}) is behind schedule.\n"
            f"Current assessment: {actual_state or 'No assessment available.'}"
        )
        actions = [
            "Review the milestone expectations against current site conditions",
            "Consider reallocating resources to catch up",
            "Update the construction plan if the timeline needs adjustment",
            "Schedule a site visit to verify AI assessment",
        ]

        create_alert(
            project_id=project_id,
            alert_type="schedule",
            severity="high",
            summary=summary,
            details=details,
            recommended_actions=actions,
        )
        created += 1
        logger.info("plan_tracker_alert_created", milestone_id=m_id, title=title)

    return created


def run_plan_check_all() -> None:
    """Run plan check + alert generation for all projects that have a plan."""
    engine = get_engine()

    with engine.begin() as conn:
        project_ids = conn.execute(
            text(
                "SELECT DISTINCT project_id FROM construction_plans WHERE status = 'ready'"
            )
        ).fetchall()

    for (pid,) in project_ids:
        logger.info("plan_tracker_run", project_id=pid)
        try:
            check_plan_progress(pid)
            generate_schedule_alerts(pid)
        except Exception:
            logger.exception("plan_tracker_failed", project_id=pid)


def _get_recent_images(project_id: int, limit: int = 5) -> List[bytes]:
    """Fetch the most recent site images as raw bytes from MinIO."""
    engine = get_engine()

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
            logger.warning("plan_tracker_image_failed", key=key, error=str(exc))

    return images
