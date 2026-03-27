"""Daily analysis engine.

Aggregates raw detections into daily/weekly metrics and generates alerts.
Called nightly by the scheduler at ANALYSIS_HOUR.

Pipeline:
  1. Daily aggregation  — detections → daily_metrics (per project, per day)
  2. Weekly rollup       — daily_metrics → weekly_metrics (Mon–Sun)
  3. Alert generation    — stall / anomaly / schedule-risk detection
"""

from __future__ import annotations

import statistics
from datetime import date, datetime, timedelta, timezone
from typing import Any, Dict, List, Optional, Tuple

import structlog
from sqlalchemy import text

from app.core import get_settings
from app.db.alerts import auto_resolve, create_alert, has_open_alert
from app.db.engine import get_engine
from app.db.projects import list_projects

logger = structlog.get_logger(__name__)

VEHICLE_CLASSES = frozenset({"car", "truck", "bus"})
PERSON_CLASSES = frozenset({"person"})


# ── Daily aggregation ────────────────────────────────────────────────────────

def _aggregate_day(project_id: int, target_date: date) -> Optional[Dict[str, Any]]:
    """Compute people_count, vehicle_count, active_hours for one project+day."""
    cfg = get_settings()
    engine = get_engine()

    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT d.class_name, d.image_id, i.captured_at "
                "FROM detections d "
                "JOIN images i ON d.image_id = i.id "
                "WHERE d.project_id = :pid "
                "  AND i.captured_at::date = :dt "
                "  AND i.status = 'DONE'"
            ),
            {"pid": project_id, "dt": target_date},
        ).fetchall()

    if not rows:
        return None

    people_per_image: Dict[int, int] = {}
    vehicle_per_image: Dict[int, int] = {}
    hours_with_detections: Dict[int, int] = {}

    for class_name, image_id, captured_at in rows:
        if class_name in PERSON_CLASSES:
            people_per_image[image_id] = people_per_image.get(image_id, 0) + 1
        elif class_name in VEHICLE_CLASSES:
            vehicle_per_image[image_id] = vehicle_per_image.get(image_id, 0) + 1

        if captured_at is not None:
            h = captured_at.hour
            hours_with_detections[h] = hours_with_detections.get(h, 0) + 1

    people_count = max(people_per_image.values()) if people_per_image else 0
    vehicle_count = max(vehicle_per_image.values()) if vehicle_per_image else 0

    min_detections_for_active = cfg.min_detections_active_hour
    active_hours = sum(
        1 for count in hours_with_detections.values()
        if count >= min_detections_for_active
    )

    return {
        "people_count": people_count,
        "vehicle_count": vehicle_count,
        "active_hours": float(active_hours),
    }


def _upsert_daily_metrics(project_id: int, target_date: date, metrics: Dict[str, Any]) -> None:
    engine = get_engine()
    now = datetime.now(timezone.utc)
    with engine.begin() as conn:
        existing = conn.execute(
            text("SELECT id FROM daily_metrics WHERE project_id = :pid AND date = :dt"),
            {"pid": project_id, "dt": target_date},
        ).fetchone()

        if existing:
            conn.execute(
                text(
                    "UPDATE daily_metrics "
                    "SET people_count = :pc, vehicle_count = :vc, active_hours = :ah "
                    "WHERE project_id = :pid AND date = :dt"
                ),
                {
                    "pc": metrics["people_count"],
                    "vc": metrics["vehicle_count"],
                    "ah": metrics["active_hours"],
                    "pid": project_id,
                    "dt": target_date,
                },
            )
        else:
            conn.execute(
                text(
                    "INSERT INTO daily_metrics (project_id, date, people_count, vehicle_count, active_hours, created_at) "
                    "VALUES (:pid, :dt, :pc, :vc, :ah, :now)"
                ),
                {
                    "pid": project_id,
                    "dt": target_date,
                    "pc": metrics["people_count"],
                    "vc": metrics["vehicle_count"],
                    "ah": metrics["active_hours"],
                    "now": now,
                },
            )


def aggregate_daily(project_id: int, target_date: date) -> bool:
    """Aggregate detections for one project+day. Returns True if data existed."""
    metrics = _aggregate_day(project_id, target_date)
    if metrics is None:
        return False
    _upsert_daily_metrics(project_id, target_date, metrics)
    logger.info(
        "daily_aggregation_done",
        project_id=project_id,
        date=str(target_date),
        **metrics,
    )
    return True


# ── Weekly rollup ────────────────────────────────────────────────────────────

def _get_daily_metrics_for_week(
    project_id: int, week_start: date,
) -> List[Dict[str, Any]]:
    week_end = week_start + timedelta(days=6)
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT date, people_count, vehicle_count, active_hours "
                "FROM daily_metrics "
                "WHERE project_id = :pid AND date >= :ws AND date <= :we "
                "ORDER BY date"
            ),
            {"pid": project_id, "ws": week_start, "we": week_end},
        ).fetchall()
    return [
        {"date": r[0], "people_count": r[1] or 0, "vehicle_count": r[2] or 0, "active_hours": r[3] or 0.0}
        for r in rows
    ]


def _get_previous_week_activity(project_id: int, week_start: date) -> Optional[float]:
    prev_start = week_start - timedelta(days=7)
    prev_end = prev_start + timedelta(days=6)
    engine = get_engine()
    with engine.begin() as conn:
        row = conn.execute(
            text(
                "SELECT COALESCE(SUM(people_count), 0) + COALESCE(SUM(vehicle_count), 0) "
                "FROM daily_metrics "
                "WHERE project_id = :pid AND date >= :ws AND date <= :we"
            ),
            {"pid": project_id, "ws": prev_start, "we": prev_end},
        ).fetchone()
    val = row[0] if row else None
    return float(val) if val is not None else None


def _get_max_weekly_activity(project_id: int) -> float:
    engine = get_engine()
    with engine.begin() as conn:
        row = conn.execute(
            text(
                "SELECT MAX(week_total) FROM ("
                "  SELECT date_trunc('week', date)::date AS ws, "
                "         SUM(people_count) + SUM(vehicle_count) AS week_total "
                "  FROM daily_metrics WHERE project_id = :pid "
                "  GROUP BY ws"
                ") sub"
            ),
            {"pid": project_id},
        ).fetchone()
    return float(row[0]) if row and row[0] else 1.0


def _get_rolling_avg_activity_index(project_id: int, before_week: date, n_weeks: int = 4) -> Optional[float]:
    engine = get_engine()
    with engine.begin() as conn:
        row = conn.execute(
            text(
                "SELECT AVG(activity_index) FROM ("
                "  SELECT activity_index FROM weekly_metrics"
                "  WHERE project_id = :pid AND week_start < :ws"
                "  ORDER BY week_start DESC LIMIT :n"
                ") sub"
            ),
            {"pid": project_id, "ws": before_week, "n": n_weeks},
        ).fetchone()
    return float(row[0]) if row and row[0] is not None else None


def _upsert_weekly_metrics(project_id: int, week_start: date, metrics: Dict[str, Any]) -> None:
    engine = get_engine()
    now = datetime.now(timezone.utc)
    with engine.begin() as conn:
        existing = conn.execute(
            text("SELECT id FROM weekly_metrics WHERE project_id = :pid AND week_start = :ws"),
            {"pid": project_id, "ws": week_start},
        ).fetchone()

        if existing:
            conn.execute(
                text(
                    "UPDATE weekly_metrics "
                    "SET progress_delta = :pd, activity_index = :ai, "
                    "    active_hours = :ah, risk_level = :rl "
                    "WHERE project_id = :pid AND week_start = :ws"
                ),
                {
                    "pd": metrics["progress_delta"],
                    "ai": metrics["activity_index"],
                    "ah": metrics["active_hours"],
                    "rl": metrics["risk_level"],
                    "pid": project_id,
                    "ws": week_start,
                },
            )
        else:
            conn.execute(
                text(
                    "INSERT INTO weekly_metrics "
                    "(project_id, week_start, progress_delta, activity_index, active_hours, risk_level, created_at) "
                    "VALUES (:pid, :ws, :pd, :ai, :ah, :rl, :now)"
                ),
                {
                    "pid": project_id,
                    "ws": week_start,
                    "pd": metrics["progress_delta"],
                    "ai": metrics["activity_index"],
                    "ah": metrics["active_hours"],
                    "rl": metrics["risk_level"],
                    "now": now,
                },
            )


def rollup_week(project_id: int, week_start: date) -> bool:
    """Compute and upsert weekly_metrics for a Mon–Sun week."""
    daily = _get_daily_metrics_for_week(project_id, week_start)
    if not daily:
        return False

    total_activity = sum(d["people_count"] + d["vehicle_count"] for d in daily)
    total_hours = sum(d["active_hours"] for d in daily)

    prev_activity = _get_previous_week_activity(project_id, week_start)
    if prev_activity and prev_activity > 0:
        progress_delta = round(((total_activity - prev_activity) / prev_activity) * 100, 1)
    elif total_activity > 0:
        progress_delta = 100.0
    else:
        progress_delta = 0.0

    max_activity = _get_max_weekly_activity(project_id)
    activity_index = round(min(100.0, (total_activity / max_activity) * 100), 1)

    rolling_avg = _get_rolling_avg_activity_index(project_id, week_start, n_weeks=4)
    if rolling_avg and rolling_avg > 0:
        drop_pct = ((rolling_avg - activity_index) / rolling_avg) * 100
        if drop_pct > 40:
            risk_level = "High"
        elif drop_pct > 20:
            risk_level = "Medium"
        else:
            risk_level = "Low"
    else:
        risk_level = "Low"

    metrics = {
        "progress_delta": progress_delta,
        "activity_index": activity_index,
        "active_hours": round(total_hours, 1),
        "risk_level": risk_level,
    }
    _upsert_weekly_metrics(project_id, week_start, metrics)
    logger.info("weekly_rollup_done", project_id=project_id, week_start=str(week_start), **metrics)
    return True


# ── Alert generation ─────────────────────────────────────────────────────────

def _get_recent_daily(project_id: int, days: int = 14) -> List[Dict[str, Any]]:
    engine = get_engine()
    cutoff = date.today() - timedelta(days=days)
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
        {"date": r[0], "people_count": r[1] or 0, "vehicle_count": r[2] or 0, "active_hours": r[3] or 0.0}
        for r in rows
    ]


def _get_recent_weekly(project_id: int, weeks: int = 4) -> List[Dict[str, Any]]:
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT week_start, progress_delta, activity_index, risk_level "
                "FROM weekly_metrics "
                "WHERE project_id = :pid "
                "ORDER BY week_start DESC LIMIT :n"
            ),
            {"pid": project_id, "n": weeks},
        ).fetchall()
    return [
        {"week_start": r[0], "progress_delta": r[1], "activity_index": r[2], "risk_level": r[3]}
        for r in reversed(rows)
    ]


def _check_stall(project_id: int, daily: List[Dict[str, Any]]) -> None:
    """Stall: total activity <= threshold for 3+ consecutive days."""
    if len(daily) < 3:
        return

    stall_threshold = 2
    consecutive_low = 0
    for d in reversed(daily):
        total = d["people_count"] + d["vehicle_count"]
        if total <= stall_threshold:
            consecutive_low += 1
        else:
            break

    if consecutive_low >= 3:
        if not has_open_alert(project_id, "stall"):
            create_alert(
                project_id,
                alert_type="stall",
                severity="high",
                summary=f"No significant activity detected for {consecutive_low} consecutive days",
                details=(
                    f"Total detections (people + vehicles) have been at or below {stall_threshold} "
                    f"for the last {consecutive_low} days. This may indicate a work stoppage, "
                    f"material delivery delay, or crew reallocation."
                ),
                recommended_actions=[
                    "Verify with site manager if work has been paused",
                    "Check material delivery schedule",
                    "Review weather logs for possible work stoppages",
                ],
            )
    else:
        resolved = auto_resolve(project_id, "stall")
        if resolved:
            logger.info("alert_auto_resolved", project_id=project_id, type="stall", count=resolved)


def _check_anomaly(project_id: int, daily: List[Dict[str, Any]]) -> None:
    """Anomaly: latest day's people or vehicle count deviates >2σ from rolling avg."""
    if len(daily) < 7:
        return

    history = daily[:-1]
    latest = daily[-1]

    people_vals = [d["people_count"] for d in history]
    vehicle_vals = [d["vehicle_count"] for d in history]

    def _is_anomalous(value: int, values: List[int]) -> Tuple[bool, float, float]:
        if len(values) < 2:
            return False, 0.0, 0.0
        avg = statistics.mean(values)
        stdev = statistics.stdev(values)
        if stdev < 1:
            stdev = 1.0
        z = abs(value - avg) / stdev
        return z > 2.0, avg, stdev

    people_anom, p_avg, p_std = _is_anomalous(latest["people_count"], people_vals)
    vehicle_anom, v_avg, v_std = _is_anomalous(latest["vehicle_count"], vehicle_vals)

    if people_anom or vehicle_anom:
        if not has_open_alert(project_id, "anomaly"):
            parts = []
            if people_anom:
                parts.append(
                    f"People count ({latest['people_count']}) deviates significantly "
                    f"from 14-day avg ({p_avg:.0f} +/- {p_std:.0f})"
                )
            if vehicle_anom:
                parts.append(
                    f"Vehicle count ({latest['vehicle_count']}) deviates significantly "
                    f"from 14-day avg ({v_avg:.0f} +/- {v_std:.0f})"
                )
            create_alert(
                project_id,
                alert_type="anomaly",
                severity="medium",
                summary=f"Unusual activity detected on {latest['date']}",
                details=". ".join(parts) + ". This could indicate unscheduled work, unauthorized access, or a data anomaly.",
                recommended_actions=[
                    "Review camera footage for the flagged period",
                    "Confirm with site manager if activity was scheduled",
                ],
            )
    else:
        resolved = auto_resolve(project_id, "anomaly")
        if resolved:
            logger.info("alert_auto_resolved", project_id=project_id, type="anomaly", count=resolved)


def _check_schedule_risk(project_id: int, weekly: List[Dict[str, Any]]) -> None:
    """Schedule risk: progress_delta negative for 2+ consecutive weeks."""
    if len(weekly) < 2:
        return

    consecutive_negative = 0
    for w in reversed(weekly):
        if w["progress_delta"] is not None and w["progress_delta"] < 0:
            consecutive_negative += 1
        else:
            break

    if consecutive_negative >= 2:
        severity = "high" if consecutive_negative >= 3 else "medium"
        if not has_open_alert(project_id, "schedule"):
            total_decline = sum(
                w["progress_delta"] for w in weekly[-consecutive_negative:]
                if w["progress_delta"] is not None
            )
            create_alert(
                project_id,
                alert_type="schedule",
                severity=severity,
                summary=f"Activity declining for {consecutive_negative} consecutive weeks",
                details=(
                    f"Progress delta has been negative for {consecutive_negative} consecutive weeks "
                    f"(cumulative change: {total_decline:+.1f}%). This trend suggests the project "
                    f"may be falling behind schedule."
                ),
                recommended_actions=[
                    "Review resource allocation and crew availability",
                    "Check for blocking issues (permits, materials, weather)",
                    "Consider schedule adjustments or additional resources",
                    "Update stakeholder timeline if delay persists",
                ],
            )
    else:
        resolved = auto_resolve(project_id, "schedule")
        if resolved:
            logger.info("alert_auto_resolved", project_id=project_id, type="schedule", count=resolved)


def generate_alerts(project_id: int) -> None:
    """Run all alert checks for a project."""
    daily = _get_recent_daily(project_id, days=14)
    weekly = _get_recent_weekly(project_id, weeks=4)

    _check_stall(project_id, daily)
    _check_anomaly(project_id, daily)
    _check_schedule_risk(project_id, weekly)


# ── Main entry point ─────────────────────────────────────────────────────────

def _get_dates_to_process(project_id: int, lookback_days: int = 7) -> List[date]:
    """Return dates with DONE images in the lookback window."""
    engine = get_engine()
    cutoff = date.today() - timedelta(days=lookback_days)

    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT DISTINCT i.captured_at::date AS dt "
                "FROM images i "
                "WHERE i.project_id = :pid AND i.status = 'DONE' "
                "  AND i.captured_at IS NOT NULL "
                "  AND i.captured_at::date >= :cutoff "
                "ORDER BY dt"
            ),
            {"pid": project_id, "cutoff": cutoff},
        ).fetchall()

    return [r[0] for r in rows]


def _get_completed_weeks(project_id: int, since: date) -> List[date]:
    """Return Monday dates of completed weeks that have daily_metrics data."""
    engine = get_engine()
    today = date.today()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT DISTINCT date_trunc('week', date)::date AS ws "
                "FROM daily_metrics "
                "WHERE project_id = :pid AND date >= :since "
                "ORDER BY ws"
            ),
            {"pid": project_id, "since": since},
        ).fetchall()

    return [r[0] for r in rows if r[0] + timedelta(days=6) < today]


def run_analysis_for_project(project_id: int, lookback_days: int = 7) -> Dict[str, Any]:
    """Compute daily/weekly metrics and alerts for one project."""
    logger.info("analysis_start", project_id=project_id, lookback_days=lookback_days)

    dates = _get_dates_to_process(project_id, lookback_days=lookback_days)
    days_processed = 0
    for dt in dates:
        if aggregate_daily(project_id, dt):
            days_processed += 1

    cutoff = date.today() - timedelta(days=lookback_days + 7)
    weeks = _get_completed_weeks(project_id, since=cutoff)
    weeks_processed = 0
    for ws in weeks:
        if rollup_week(project_id, ws):
            weeks_processed += 1

    generate_alerts(project_id)

    result = {
        "project_id": project_id,
        "days_processed": days_processed,
        "weeks_processed": weeks_processed,
        "lookback_days": lookback_days,
    }
    logger.info("analysis_complete", **result)
    return result


def run_analysis() -> None:
    """Main entry point called by the scheduler nightly."""
    projects = list_projects()
    if not projects:
        logger.info("analysis_skipped", reason="no projects")
        return

    for project in projects:
        pid = int(project["id"])
        run_analysis_for_project(pid)
