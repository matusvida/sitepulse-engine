"""Post-processing pipeline: confidence filtering, area filtering, ROI."""

from __future__ import annotations

from typing import Dict, List, Optional, Tuple

import structlog

from app.core import get_settings
from app.detection.model import RawDetection
from app.detection.schemas import Detection

logger = structlog.get_logger(__name__)


def _box_area(xyxy: List[float]) -> float:
    return max(0.0, xyxy[2] - xyxy[0]) * max(0.0, xyxy[3] - xyxy[1])


def _box_centre(xyxy: List[float]) -> Tuple[float, float]:
    return (xyxy[0] + xyxy[2]) / 2, (xyxy[1] + xyxy[3]) / 2


def _point_in_polygon(px: float, py: float, polygon: List[List[float]]) -> bool:
    """Ray-casting algorithm for point-in-polygon test."""
    n = len(polygon)
    inside = False
    j = n - 1
    for i in range(n):
        xi, yi = polygon[i]
        xj, yj = polygon[j]
        if ((yi > py) != (yj > py)) and (px < (xj - xi) * (py - yi) / (yj - yi) + xi):
            inside = not inside
        j = i
    return inside


def _resolve_roi_polygon(
    key: str, roi_config: Optional[dict],
) -> Optional[List[List[float]]]:
    """Find matching ROI polygon for a given S3 key, if any."""
    if roi_config is None:
        return None
    cameras: dict = roi_config.get("cameras", {})
    for prefix, cam_cfg in cameras.items():
        if key.startswith(prefix):
            return cam_cfg.get("roi_polygon")
    return None


def postprocess(
    raw: List[RawDetection],
    image_key: str,
    roi_config: Optional[dict] = None,
    *,
    roi_polygon_override: Optional[List[List[float]]] = None,
    drop_outside_override: Optional[bool] = None,
) -> Tuple[List[Detection], List[str]]:
    """Apply all filtering and return final detections + warning strings.

    When ``roi_polygon_override`` is provided (e.g. from the cameras DB table),
    it takes precedence over the file-based ``roi_config``.
    """
    cfg = get_settings()
    per_class: Dict[str, float] = cfg.per_class_thresholds
    warnings: List[str] = []

    if roi_polygon_override is not None:
        roi_polygon = roi_polygon_override
        drop_outside = drop_outside_override if drop_outside_override is not None else True
    else:
        roi_polygon = _resolve_roi_polygon(image_key, roi_config)
        drop_outside = False
        if roi_polygon is not None and roi_config is not None:
            for cam_cfg in roi_config.get("cameras", {}).values():
                if cam_cfg.get("roi_polygon") == roi_polygon:
                    drop_outside = cam_cfg.get("drop_outside", False)
                    break

    kept: List[Detection] = []
    filtered_conf = 0
    filtered_area = 0
    filtered_roi = 0

    for det in raw:
        threshold = per_class.get(det.class_name, cfg.conf_threshold)
        if det.score < threshold:
            filtered_conf += 1
            continue

        area = _box_area(det.bbox_xyxy)
        if area < cfg.min_box_area:
            filtered_area += 1
            continue

        in_roi: Optional[bool] = None
        if roi_polygon is not None:
            cx, cy = _box_centre(det.bbox_xyxy)
            in_roi = _point_in_polygon(cx, cy, roi_polygon)
            if drop_outside and not in_roi:
                filtered_roi += 1
                continue

        kept.append(
            Detection(
                class_id=det.class_id,
                class_name=det.class_name,
                score=round(det.score, 4),
                bbox_xyxy=[round(c, 1) for c in det.bbox_xyxy],
                in_roi=in_roi,
            )
        )

    if filtered_conf:
        warnings.append(f"{filtered_conf} detections below confidence threshold")
    if filtered_area:
        warnings.append(f"{filtered_area} detections below minimum box area")
    if filtered_roi:
        warnings.append(f"{filtered_roi} detections outside ROI")

    logger.info(
        "postprocess_done",
        kept=len(kept),
        filtered_conf=filtered_conf,
        filtered_area=filtered_area,
        filtered_roi=filtered_roi,
    )
    return kept, warnings
