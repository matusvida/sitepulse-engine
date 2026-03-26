"""SQLAlchemy table definitions for the sitepulse-engine system.

All tables are defined here and nowhere else — single source of truth
for the schema. Alembic migrations reference this ``metadata`` object.
"""

from __future__ import annotations

from datetime import datetime, timezone

from sqlalchemy import (
    Boolean,
    Column,
    Date,
    DateTime,
    Float,
    ForeignKey,
    Integer,
    MetaData,
    String,
    Table,
    Text,
    UniqueConstraint,
)
from sqlalchemy.dialects.postgresql import JSONB

metadata = MetaData()

projects = Table(
    "projects",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("name", String(256), nullable=False),
    Column("location", String(512)),
    Column("dropbox_path", String(1024)),
    Column("created_at", DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)),
)

cameras = Table(
    "cameras",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("project_id", Integer, ForeignKey("projects.id"), nullable=False),
    Column("name", String(256)),
    Column("roi_polygon", JSONB),
    Column("drop_outside", Boolean, server_default="true"),
    Column("key_prefix", String(512)),
    Column("created_at", DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)),
)

images = Table(
    "images",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("bucket", String(256), nullable=False),
    Column("key", String(1024), nullable=False),
    Column("status", String(32), nullable=False, server_default="NEW"),
    Column("project_id", Integer, ForeignKey("projects.id")),
    Column("camera_id", Integer, ForeignKey("cameras.id")),
    Column("captured_at", DateTime(timezone=True)),
    Column("created_at", DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)),
    Column("updated_at", DateTime(timezone=True), onupdate=lambda: datetime.now(timezone.utc)),
)

detections = Table(
    "detections",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("image_id", Integer, ForeignKey("images.id"), nullable=False),
    Column("project_id", Integer, ForeignKey("projects.id")),
    Column("model_version", String(128)),
    Column("class_id", Integer),
    Column("class_name", String(128)),
    Column("score", Float),
    Column("bbox_xyxy", Text),
    Column("in_roi", String(8)),
    Column("created_at", DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)),
)

daily_metrics = Table(
    "daily_metrics",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("project_id", Integer, ForeignKey("projects.id"), nullable=False),
    Column("date", Date, nullable=False),
    Column("people_count", Integer),
    Column("vehicle_count", Integer),
    Column("active_hours", Float),
    Column("created_at", DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)),
    UniqueConstraint("project_id", "date", name="uq_daily_metrics_project_date"),
)

weekly_metrics = Table(
    "weekly_metrics",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("project_id", Integer, ForeignKey("projects.id"), nullable=False),
    Column("week_start", Date, nullable=False),
    Column("progress_delta", Float),
    Column("activity_index", Float),
    Column("active_hours", Float),
    Column("risk_level", String(16)),
    Column("created_at", DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)),
    UniqueConstraint("project_id", "week_start", name="uq_weekly_metrics_project_week"),
)

alerts = Table(
    "alerts",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("project_id", Integer, ForeignKey("projects.id"), nullable=False),
    Column("type", String(32)),
    Column("severity", String(16)),
    Column("status", String(16), server_default="open"),
    Column("summary", Text),
    Column("details", Text),
    Column("recommended_actions", JSONB),
    Column("created_at", DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)),
    Column("updated_at", DateTime(timezone=True)),
)

sync_jobs = Table(
    "sync_jobs",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("project_id", Integer, ForeignKey("projects.id"), nullable=False),
    Column("status", String(32)),
    Column("images_found", Integer),
    Column("images_synced", Integer),
    Column("error", Text),
    Column("started_at", DateTime(timezone=True)),
    Column("finished_at", DateTime(timezone=True)),
)

construction_plans = Table(
    "construction_plans",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("project_id", Integer, ForeignKey("projects.id"), nullable=False),
    Column("filename", String(512)),
    Column("raw_text", Text),
    Column("status", String(32), nullable=False, server_default="processing"),
    Column("created_at", DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)),
    Column("updated_at", DateTime(timezone=True)),
)

plan_milestones = Table(
    "plan_milestones",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("plan_id", Integer, ForeignKey("construction_plans.id", ondelete="CASCADE"), nullable=False),
    Column("project_id", Integer, ForeignKey("projects.id"), nullable=False),
    Column("week_number", Integer, nullable=False),
    Column("title", String(512), nullable=False),
    Column("description", Text),
    Column("expected_state", Text),
    Column("actual_state", Text),
    Column("status", String(32), nullable=False, server_default="not_started"),
    Column("checked_at", DateTime(timezone=True)),
    Column("created_at", DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)),
)

progress_reports = Table(
    "progress_reports",
    metadata,
    Column("id", Integer, primary_key=True, autoincrement=True),
    Column("project_id", Integer, ForeignKey("projects.id"), nullable=False),
    Column("report_type", String(32), nullable=False, server_default="custom"),
    Column("content_md", Text),
    Column("summary", Text),
    Column("date_range_start", Date),
    Column("date_range_end", Date),
    Column("image_count", Integer),
    Column("model_used", String(128)),
    Column("created_at", DateTime(timezone=True), default=lambda: datetime.now(timezone.utc)),
)
