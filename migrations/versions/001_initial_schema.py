"""Initial schema — all tables for the sitepulse-engine system.

Revision ID: 001
Revises: None
Create Date: 2026-03-19
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects.postgresql import JSONB

revision: str = "001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # ── projects ─────────────────────────────────────────
    op.create_table(
        "projects",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("name", sa.String(256), nullable=False),
        sa.Column("location", sa.String(512)),
        sa.Column("dropbox_path", sa.String(1024)),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )

    # ── cameras ──────────────────────────────────────────
    op.create_table(
        "cameras",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("project_id", sa.Integer, sa.ForeignKey("projects.id"), nullable=False),
        sa.Column("name", sa.String(256)),
        sa.Column("roi_polygon", JSONB),
        sa.Column("drop_outside", sa.Boolean, server_default="true"),
        sa.Column("key_prefix", sa.String(512)),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )

    # ── images ───────────────────────────────────────────
    op.create_table(
        "images",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("bucket", sa.String(256), nullable=False),
        sa.Column("key", sa.String(1024), nullable=False),
        sa.Column("status", sa.String(32), nullable=False, server_default="NEW"),
        sa.Column("project_id", sa.Integer, sa.ForeignKey("projects.id")),
        sa.Column("camera_id", sa.Integer, sa.ForeignKey("cameras.id")),
        sa.Column("captured_at", sa.DateTime(timezone=True)),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
        sa.Column("updated_at", sa.DateTime(timezone=True)),
    )

    # ── detections ───────────────────────────────────────
    op.create_table(
        "detections",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("image_id", sa.Integer, sa.ForeignKey("images.id"), nullable=False),
        sa.Column("project_id", sa.Integer, sa.ForeignKey("projects.id")),
        sa.Column("model_version", sa.String(128)),
        sa.Column("class_id", sa.Integer),
        sa.Column("class_name", sa.String(128)),
        sa.Column("score", sa.Float),
        sa.Column("bbox_xyxy", sa.Text),
        sa.Column("in_roi", sa.String(8)),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )

    # ── daily_metrics ────────────────────────────────────
    op.create_table(
        "daily_metrics",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("project_id", sa.Integer, sa.ForeignKey("projects.id"), nullable=False),
        sa.Column("date", sa.Date, nullable=False),
        sa.Column("people_count", sa.Integer),
        sa.Column("vehicle_count", sa.Integer),
        sa.Column("active_hours", sa.Float),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
        sa.UniqueConstraint("project_id", "date", name="uq_daily_metrics_project_date"),
    )

    # ── weekly_metrics ───────────────────────────────────
    op.create_table(
        "weekly_metrics",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("project_id", sa.Integer, sa.ForeignKey("projects.id"), nullable=False),
        sa.Column("week_start", sa.Date, nullable=False),
        sa.Column("progress_delta", sa.Float),
        sa.Column("activity_index", sa.Float),
        sa.Column("active_hours", sa.Float),
        sa.Column("risk_level", sa.String(16)),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
        sa.UniqueConstraint("project_id", "week_start", name="uq_weekly_metrics_project_week"),
    )

    # ── alerts ───────────────────────────────────────────
    op.create_table(
        "alerts",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("project_id", sa.Integer, sa.ForeignKey("projects.id"), nullable=False),
        sa.Column("type", sa.String(32)),
        sa.Column("severity", sa.String(16)),
        sa.Column("status", sa.String(16), server_default="open"),
        sa.Column("summary", sa.Text),
        sa.Column("details", sa.Text),
        sa.Column("recommended_actions", JSONB),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
        sa.Column("updated_at", sa.DateTime(timezone=True)),
    )

    # ── sync_jobs ────────────────────────────────────────
    op.create_table(
        "sync_jobs",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("project_id", sa.Integer, sa.ForeignKey("projects.id"), nullable=False),
        sa.Column("status", sa.String(32)),
        sa.Column("images_found", sa.Integer),
        sa.Column("images_synced", sa.Integer),
        sa.Column("error", sa.Text),
        sa.Column("started_at", sa.DateTime(timezone=True)),
        sa.Column("finished_at", sa.DateTime(timezone=True)),
    )


def downgrade() -> None:
    op.drop_table("sync_jobs")
    op.drop_table("alerts")
    op.drop_table("weekly_metrics")
    op.drop_table("daily_metrics")
    op.drop_table("detections")
    op.drop_table("images")
    op.drop_table("cameras")
    op.drop_table("projects")
