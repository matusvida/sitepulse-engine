"""YOLO model loading and inference.

The model file is downloaded on first run and cached by Ultralytics under
``~/.config/Ultralytics/``.  When a CUDA GPU is present the runtime uses
it automatically.
"""

from __future__ import annotations

import time
from typing import List, Tuple

import numpy as np
import structlog
from ultralytics import YOLO
from ultralytics.engine.results import Results

from app.core import get_settings

logger = structlog.get_logger(__name__)

_model: YOLO | None = None
_model_version: str = ""


class RawDetection:
    """Lightweight container for a single YOLO detection before postprocessing."""

    __slots__ = ("class_id", "class_name", "score", "bbox_xyxy")

    def __init__(self, class_id: int, class_name: str, score: float, bbox_xyxy: List[float]):
        self.class_id = class_id
        self.class_name = class_name
        self.score = score
        self.bbox_xyxy = bbox_xyxy


def load_model() -> None:
    """Load the YOLO model once at process startup."""
    global _model, _model_version

    cfg = get_settings()
    logger.info("loading_yolo_model", path=cfg.yolo_model_path)
    t0 = time.perf_counter()
    _model = YOLO(cfg.yolo_model_path)
    elapsed = (time.perf_counter() - t0) * 1000
    _model_version = cfg.yolo_model_path
    logger.info("yolo_model_loaded", model=_model_version, elapsed_ms=round(elapsed, 1))


def get_model_version() -> str:
    return _model_version


def is_loaded() -> bool:
    return _model is not None


def run_inference(image_bgr: np.ndarray) -> Tuple[List[RawDetection], float]:
    """Run YOLO inference and return raw detections + elapsed ms.

    The caller is responsible for post-processing (thresholds, ROI, etc.).
    """
    if _model is None:
        raise RuntimeError("Model not loaded — call load_model() first")

    t0 = time.perf_counter()
    results: List[Results] = _model.predict(
        source=image_bgr,
        verbose=False,
        conf=0.10,
    )
    elapsed_ms = (time.perf_counter() - t0) * 1000

    detections: List[RawDetection] = []
    for r in results:
        boxes = r.boxes
        if boxes is None:
            continue
        for i in range(len(boxes)):
            xyxy = boxes.xyxy[i].tolist()
            conf = float(boxes.conf[i])
            cls_id = int(boxes.cls[i])
            cls_name = r.names.get(cls_id, str(cls_id))
            detections.append(RawDetection(cls_id, cls_name, conf, xyxy))

    logger.info(
        "inference_done",
        raw_count=len(detections),
        elapsed_ms=round(elapsed_ms, 1),
    )
    return detections, elapsed_ms
