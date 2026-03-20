"""Image detection processing — picks up NEW images and runs YOLO.

Can be run standalone::

    python -m app.worker.processor

Or used by the scheduler for batch sweeps.
"""

from __future__ import annotations

import sys
import time

import cv2
import numpy as np
import structlog

from app.core import get_settings
from app.core.logging import configure_logging
from app.db.detections import insert_detections
from app.db.images import fetch_new_images, mark_done, mark_failed
from app.db.projects import find_camera_by_key, get_camera
from app.detection.model import get_model_version, load_model, run_inference
from app.detection.postprocess import postprocess
from app.services.storage import S3DownloadError, download_image_bytes

logger = structlog.get_logger(__name__)


def _resolve_camera_roi(row: dict) -> tuple:
    """Resolve ROI polygon and drop_outside from the cameras table.

    Returns (roi_polygon, drop_outside) or (None, None) if no camera match.
    """
    camera_id = row.get("camera_id")
    project_id = row.get("project_id")
    key = row["key"]

    cam = None
    if camera_id:
        cam = get_camera(camera_id)
    elif project_id:
        cam = find_camera_by_key(project_id, key)

    if cam and cam.get("roi_polygon"):
        return cam["roi_polygon"], cam.get("drop_outside", True)
    return None, None


def process_image(row: dict) -> None:
    """Download, detect, postprocess, and persist results for a single image."""
    image_id = row["id"]
    bucket = row["bucket"]
    key = row["key"]
    project_id = row.get("project_id")
    cfg = get_settings()

    logger.info("processing_image", image_id=image_id, bucket=bucket, key=key)

    try:
        raw_bytes = download_image_bytes(bucket, key)
    except S3DownloadError as exc:
        logger.error("download_failed", image_id=image_id, error=str(exc))
        mark_failed(image_id, str(exc))
        return

    arr = np.frombuffer(raw_bytes, dtype=np.uint8)
    image_bgr = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if image_bgr is None:
        mark_failed(image_id, "Could not decode image")
        return

    raw_dets, inference_ms = run_inference(image_bgr)

    roi_polygon, drop_outside = _resolve_camera_roi(row)
    if roi_polygon is not None:
        detections, _ = postprocess(
            raw_dets, key,
            roi_polygon_override=roi_polygon,
            drop_outside_override=drop_outside,
        )
    else:
        roi_config = cfg.load_roi_config()
        detections, _ = postprocess(raw_dets, key, roi_config)

    insert_detections(image_id, get_model_version(), detections, project_id=project_id)
    mark_done(image_id)
    logger.info(
        "image_done",
        image_id=image_id,
        detections=len(detections),
        inference_ms=round(inference_ms, 1),
    )


def main() -> None:
    """Standalone worker loop — polls DB for NEW images continuously."""
    configure_logging()
    cfg = get_settings()
    if not cfg.enable_db:
        logger.error("Worker requires ENABLE_DB=true")
        sys.exit(1)

    from app.db.engine import run_migrations
    run_migrations()
    load_model()
    logger.info("worker_started", poll_interval=cfg.worker_poll_interval)

    try:
        while True:
            rows = fetch_new_images(limit=10)
            if not rows:
                time.sleep(cfg.worker_poll_interval)
                continue
            for row in rows:
                process_image(row)
    except KeyboardInterrupt:
        logger.info("worker_stopped")


if __name__ == "__main__":
    main()
