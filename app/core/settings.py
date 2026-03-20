"""Centralised configuration via pydantic-settings.

All values are overridable through environment variables or a .env file
placed next to the project root.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Dict

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    # ── MinIO / S3 ──────────────────────────────────────
    minio_endpoint: str = "http://localhost:9000"
    minio_access_key: str = "admin"
    minio_secret_key: str = "password123"
    minio_bucket_default: str = "tower-tl"

    # ── YOLO ────────────────────────────────────────────
    yolo_model_path: str = "yolov8x.pt"

    # ── Post-processing ─────────────────────────────────
    conf_threshold: float = 0.35
    per_class_thresholds_json: str = "{}"
    min_box_area: float = 400.0

    # ── ROI ─────────────────────────────────────────────
    enable_roi: bool = False
    roi_config_path: str = "roi_config.json"

    # ── Image quality ───────────────────────────────────
    blur_threshold: float = 50.0
    brightness_low: int = 30
    brightness_high: int = 240
    skip_bad_quality: bool = False

    # ── Postgres (optional) ─────────────────────────────
    enable_db: bool = False
    postgres_dsn: str = "postgresql://sitepulse:sitepulse@localhost:5432/sitepulse"

    # ── Dropbox sync ─────────────────────────────────────
    dropbox_token: str = ""
    sync_schedule_minutes: int = 60

    # ── Worker ──────────────────────────────────────────
    worker_poll_interval: int = 5

    # ── Scheduler / Analysis ────────────────────────────
    analysis_hour: int = 2
    min_detections_active_hour: int = 3

    # ── Safety ──────────────────────────────────────────
    max_image_bytes: int = 50 * 1024 * 1024  # 50 MB

    # ── Derived helpers ─────────────────────────────────
    @property
    def per_class_thresholds(self) -> Dict[str, float]:
        return json.loads(self.per_class_thresholds_json)

    @field_validator("per_class_thresholds_json")
    @classmethod
    def _validate_json_map(cls, v: str) -> str:
        parsed = json.loads(v)
        if not isinstance(parsed, dict):
            raise ValueError("PER_CLASS_THRESHOLDS_JSON must be a JSON object")
        return v

    def load_roi_config(self) -> dict | None:
        if not self.enable_roi:
            return None
        p = Path(self.roi_config_path)
        if not p.exists():
            return None
        return json.loads(p.read_text(encoding="utf-8"))


def get_settings() -> Settings:
    """Singleton-ish factory cached at module level."""
    return _settings


_settings = Settings()
