"""Pydantic models for detection API request / response contracts."""

from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel, Field


class DetectRequest(BaseModel):
    bucket: Optional[str] = Field(
        default=None,
        description="S3 bucket name.  Falls back to MINIO_BUCKET_DEFAULT.",
    )
    key: Optional[str] = Field(
        default=None,
        description="Object key inside the bucket (e.g. '2024-02-13 11_44_05.jpg').",
    )
    s3_url: Optional[str] = Field(
        default=None,
        description="Full s3:// URL.  If provided, bucket and key are parsed from it.",
    )


class Detection(BaseModel):
    class_id: int
    class_name: str
    score: float
    bbox_xyxy: List[float] = Field(description="[x1, y1, x2, y2] in pixel coords")
    in_roi: Optional[bool] = Field(
        default=None,
        description="Whether the detection centre falls inside the ROI polygon (null if ROI disabled).",
    )


class DetectResponse(BaseModel):
    model_version: str
    bucket: str
    key: str
    image_width: int
    image_height: int
    inference_ms: float
    detections: List[Detection]
    warnings: List[str] = Field(default_factory=list)


class HealthResponse(BaseModel):
    status: str = "ok"
    model_loaded: bool
    model_version: str
