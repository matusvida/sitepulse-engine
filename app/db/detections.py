"""Detection record repository."""

from __future__ import annotations

import json
from typing import Optional

from app.db.engine import get_engine
from app.db.tables import detections


def insert_detections(
    image_id: int,
    model_version: str,
    detection_list: list,
    project_id: Optional[int] = None,
) -> None:
    if not detection_list:
        return
    engine = get_engine()
    rows = [
        {
            "image_id": image_id,
            "project_id": project_id,
            "model_version": model_version,
            "class_id": d.class_id,
            "class_name": d.class_name,
            "score": round(d.score, 4),
            "bbox_xyxy": json.dumps([round(c, 1) for c in d.bbox_xyxy]),
            "in_roi": str(d.in_roi) if d.in_roi is not None else None,
        }
        for d in detection_list
    ]
    with engine.begin() as conn:
        conn.execute(detections.insert(), rows)
