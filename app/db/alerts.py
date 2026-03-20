"""Alert repository — queries used by the analysis engine."""

from __future__ import annotations

import json
from datetime import datetime, timezone
from typing import List

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
