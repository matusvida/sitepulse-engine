"""Draw detection bounding boxes onto image copies and upload to MinIO.

The annotated images are stored under a ``detection/`` prefix in the same
bucket, preserving the original date/filename structure::

    tower-tl/2026-03-20/image.jpg          ← original
    tower-tl/detection/2026-03-20/image.jpg ← annotated copy
"""

from __future__ import annotations

import json
from datetime import date, timedelta
from typing import Any, Dict, List, Tuple

import cv2
import numpy as np
import structlog
from sqlalchemy import text

from app.core import get_settings
from app.db.engine import get_engine
from app.services.storage import download_image_bytes, upload_bytes

logger = structlog.get_logger(__name__)

CLASS_COLORS: Dict[str, Tuple[int, int, int]] = {
    "person": (0, 120, 255),
    "car": (255, 80, 0),
    "truck": (255, 0, 80),
    "bus": (200, 0, 200),
    "motorcycle": (0, 200, 200),
    "bicycle": (0, 200, 100),
}
DEFAULT_COLOR = (0, 255, 0)


def _color_for(class_name: str) -> Tuple[int, int, int]:
    return CLASS_COLORS.get(class_name, DEFAULT_COLOR)


def _fetch_images_with_detections(
    project_id: int, date_from: date, date_to: date
) -> List[Dict[str, Any]]:
    """Fetch images (with at least one detection) in the date range."""
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT DISTINCT i.id, i.bucket, i.key "
                "FROM images i "
                "JOIN detections d ON d.image_id = i.id "
                "WHERE i.project_id = :pid "
                "  AND i.status = 'DONE' "
                "  AND i.captured_at >= :d1 "
                "  AND i.captured_at < :d2 "
                "ORDER BY i.id"
            ),
            {
                "pid": project_id,
                "d1": date_from.isoformat(),
                "d2": (date_to + timedelta(days=1)).isoformat(),
            },
        ).fetchall()
    return [{"id": r[0], "bucket": r[1], "key": r[2]} for r in rows]


def _fetch_detections_for_image(image_id: int) -> List[Dict[str, Any]]:
    engine = get_engine()
    with engine.begin() as conn:
        rows = conn.execute(
            text(
                "SELECT class_name, score, bbox_xyxy, in_roi "
                "FROM detections WHERE image_id = :iid"
            ),
            {"iid": image_id},
        ).fetchall()
    results = []
    for r in rows:
        bbox = json.loads(r[2])
        results.append({
            "class_name": r[0],
            "score": r[1],
            "bbox": bbox,
            "in_roi": r[3],
        })
    return results


def _draw_detections(img_bytes: bytes, detections: List[Dict[str, Any]]) -> bytes:
    """Draw bounding boxes + labels onto an image, return JPEG bytes."""
    arr = np.frombuffer(img_bytes, dtype=np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        raise ValueError("Could not decode image")

    for det in detections:
        x1, y1, x2, y2 = [int(c) for c in det["bbox"]]
        color = _color_for(det["class_name"])
        cv2.rectangle(img, (x1, y1), (x2, y2), color, 2)

        label = f"{det['class_name']} {det['score']:.0%}"
        if det.get("in_roi") == "True":
            label += " [ROI]"

        font_scale = 0.5
        thickness = 1
        (tw, th), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, font_scale, thickness)
        cv2.rectangle(img, (x1, y1 - th - 6), (x1 + tw + 4, y1), color, -1)
        cv2.putText(
            img, label, (x1 + 2, y1 - 4),
            cv2.FONT_HERSHEY_SIMPLEX, font_scale, (255, 255, 255), thickness,
        )

    _, buf = cv2.imencode(".jpg", img, [cv2.IMWRITE_JPEG_QUALITY, 92])
    return buf.tobytes()


def _detection_key(original_key: str) -> str:
    """Build the destination key under the detection/ prefix.

    ``2026-03-20/image.jpg`` → ``detection/2026-03-20/image.jpg``
    """
    return f"detection/{original_key}"


def visualize_detections(project_id: int, date_from: date, date_to: date) -> Dict[str, Any]:
    """Main entry point: annotate images in [date_from, date_to] and upload copies.

    Returns a summary with counts.
    """
    cfg = get_settings()
    bucket = cfg.minio_bucket_default

    images = _fetch_images_with_detections(project_id, date_from, date_to)
    logger.info("visualize_start", project_id=project_id, images=len(images),
                date_from=str(date_from), date_to=str(date_to))

    processed = 0
    errors = []

    for img_row in images:
        image_id = img_row["id"]
        key = img_row["key"]
        try:
            img_bytes = download_image_bytes(bucket, key)
            detections = _fetch_detections_for_image(image_id)
            if not detections:
                continue

            annotated = _draw_detections(img_bytes, detections)
            dest_key = _detection_key(key)
            upload_bytes(bucket, dest_key, annotated)
            processed += 1
            logger.info("visualize_ok", key=key, dest=dest_key, detections=len(detections))
        except Exception as exc:
            errors.append(f"{key}: {exc}")
            logger.error("visualize_fail", key=key, error=str(exc))

    logger.info("visualize_complete", processed=processed, errors=len(errors))
    return {
        "imagesFound": len(images),
        "imagesProcessed": processed,
        "errors": errors,
    }
