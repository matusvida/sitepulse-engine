"""Detection and health-check endpoints."""

from __future__ import annotations

import cv2
import numpy as np
import structlog
from fastapi import APIRouter, HTTPException

from app.core import get_settings
from app.detection.model import get_model_version, is_loaded, run_inference
from app.detection.postprocess import postprocess
from app.detection.quality import check_quality
from app.detection.schemas import DetectRequest, DetectResponse, HealthResponse
from app.services.storage import S3DownloadError, download_image_bytes, parse_s3_url

logger = structlog.get_logger(__name__)
router = APIRouter()

_db_loaded = False
_db_mod = None


def _get_db():
    global _db_loaded, _db_mod
    if not _db_loaded:
        cfg = get_settings()
        if cfg.enable_db:
            from app.db import images as img_repo, detections as det_repo
            _db_mod = (img_repo, det_repo)
        _db_loaded = True
    return _db_mod


def _decode_image(raw: bytes) -> np.ndarray:
    arr = np.frombuffer(raw, dtype=np.uint8)
    img = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    if img is None:
        raise HTTPException(status_code=422, detail="Could not decode image (not a valid JPEG/PNG)")
    return img


@router.get("/health", response_model=HealthResponse)
async def health():
    return HealthResponse(
        status="ok",
        model_loaded=is_loaded(),
        model_version=get_model_version(),
    )


@router.post("/detect", response_model=DetectResponse)
async def detect(req: DetectRequest):
    cfg = get_settings()

    if req.s3_url:
        try:
            bucket, key = parse_s3_url(req.s3_url)
        except ValueError as exc:
            raise HTTPException(status_code=400, detail=str(exc))
    else:
        bucket = req.bucket or cfg.minio_bucket_default
        key = req.key
        if not key:
            raise HTTPException(
                status_code=400,
                detail="Either 'key' or 's3_url' must be provided",
            )

    logger.info("detect_request", bucket=bucket, key=key)

    try:
        raw_bytes = download_image_bytes(bucket, key)
    except S3DownloadError as exc:
        logger.warning("s3_download_failed", bucket=bucket, key=key, error=str(exc))
        raise HTTPException(status_code=502, detail=str(exc))

    image_bgr = _decode_image(raw_bytes)
    h, w = image_bgr.shape[:2]

    warnings: list[str] = check_quality(image_bgr)
    if warnings and cfg.skip_bad_quality:
        return DetectResponse(
            model_version=get_model_version(),
            bucket=bucket,
            key=key,
            image_width=w,
            image_height=h,
            inference_ms=0.0,
            detections=[],
            warnings=warnings + ["Skipped inference due to bad image quality"],
        )

    raw_dets, inference_ms = run_inference(image_bgr)

    roi_config = cfg.load_roi_config()
    detections, pp_warnings = postprocess(raw_dets, key, roi_config)
    warnings.extend(pp_warnings)

    response = DetectResponse(
        model_version=get_model_version(),
        bucket=bucket,
        key=key,
        image_width=w,
        image_height=h,
        inference_ms=round(inference_ms, 1),
        detections=detections,
        warnings=warnings,
    )

    db = _get_db()
    if db is not None:
        img_repo, det_repo = db
        try:
            image_id = img_repo.insert_image_record(bucket, key, status="DONE")
            det_repo.insert_detections(image_id, get_model_version(), detections)
            logger.info("db_persisted", image_id=image_id, detections=len(detections))
        except Exception:
            logger.exception("db_persist_failed")
            response.warnings.append("Detection succeeded but failed to persist to database")

    return response
