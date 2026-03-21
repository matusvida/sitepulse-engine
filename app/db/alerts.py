"""Alert repository — queries used by the analysis engine and REST API."""

from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from sqlalchemy import text

from app.db.engine import get_engine


def has_open_alert(project_id: int, alert_type: str) -> bool:
    engine = get_engine()
    with engine.begin() as conn:
        row = conn.execute(
            text(
                "SELECT 1 FROM alerts "
                "WHERE project_id = :pid AND type = :t AND status = 'open' LIMIT 1"
            ),
            {"pid": project_id, "t": alert_type},
        ).fetchone()
    return row is not None


def create_alert(
    project_id: int,
    alert_type: str,
    severity: str,
    summary: str,
    details: str,
    recommended_actions: List[str],
) -> None:
    engine = get_engine()
    now = datetime.now(timezone.utc)
    with engine.begin() as conn:
        conn.execute(
            text(
                "INSERT INTO alerts "
                "(project_id, type, severity, status, summary, details, recommended_actions, created_at) "
                "VALUES (:pid, :t, :sev, 'open', :sum, :det, :ra::jsonb, :now)"
            ),
            {
                "pid": project_id,
                "t": alert_type,
                "sev": severity,
                "sum": summary,
                "det": details,
                "ra": json.dumps(recommended_actions),
                "now": now,
            },
        )


def auto_resolve(project_id: int, alert_type: str) -> int:
    """Resolve all open alerts of a given type. Returns number resolved."""
    engine = get_engine()
    now = datetime.now(timezone.utc)
    with engine.begin() as conn:
        result = conn.execute(
            text(
                "UPDATE alerts SET status = 'resolved', updated_at = :now "
                "WHERE project_id = :pid AND type = :t AND status = 'open'"
            ),
            {"pid": project_id, "t": alert_type, "now": now},
        )
        return result.rowcount


def list_alerts(
    project_id: int,
    *,
    alert_type: Optional[str] = None,
    severity: Optional[str] = None,
    status: Optional[str] = None,
) -> List[Dict[str, Any]]:
    """Fetch alerts for a project with optional filters."""
    engine = get_engine()
    clauses = ["project_id = :pid"]
    params: Dict[str, Any] = {"pid": project_id}

    if alert_type:
        clauses.append("type = :t")
        params["t"] = alert_type
    if severity:
        clauses.append("severity = :sev")
        params["sev"] = severity
    if status:
        clauses.append("status = :st")
        params["st"] = status

    where = " AND ".join(clauses)
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                f"SELECT id, project_id, type, severity, status, summary, details, "
                f"recommended_actions, created_at, updated_at "
                f"FROM alerts WHERE {where} ORDER BY created_at DESC"
            ),
            params,
        ).fetchall()

    return [_alert_row_to_dict(r) for r in rows]


def get_alert(alert_id: int) -> Optional[Dict[str, Any]]:
    engine = get_engine()
    with engine.begin() as conn:
        row = conn.execute(
            text(
                "SELECT id, project_id, type, severity, status, summary, details, "
                "recommended_actions, created_at, updated_at "
                "FROM alerts WHERE id = :aid"
            ),
            {"aid": alert_id},
        ).fetchone()
    if row is None:
        return None
    return _alert_row_to_dict(row)


def update_alert_status(alert_id: int, new_status: str) -> Optional[Dict[str, Any]]:
    engine = get_engine()
    now = datetime.now(timezone.utc)
    with engine.begin() as conn:
        conn.execute(
            text("UPDATE alerts SET status = :st, updated_at = :now WHERE id = :aid"),
            {"st": new_status, "now": now, "aid": alert_id},
        )
    return get_alert(alert_id)


def _alert_row_to_dict(row) -> Dict[str, Any]:
    ra = row[7]
    if isinstance(ra, str):
        ra = json.loads(ra)
    return {
        "id": str(row[0]),
        "createdAt": row[8].isoformat() if row[8] else None,
        "type": row[2],
        "severity": row[3],
        "status": row[4],
        "summary": row[5],
        "details": row[6],
        "recommendedActions": ra or [],
    }
