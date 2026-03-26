"""Add construction_plans, plan_milestones, and progress_reports tables.

Revision ID: 002
Revises: 001
Create Date: 2026-03-03
"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa

revision: str = "002"
down_revision: Union[str, None] = "001"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "construction_plans",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("project_id", sa.Integer, sa.ForeignKey("projects.id"), nullable=False),
        sa.Column("filename", sa.String(512)),
        sa.Column("raw_text", sa.Text),
        sa.Column("status", sa.String(32), nullable=False, server_default="processing"),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
        sa.Column("updated_at", sa.DateTime(timezone=True)),
    )

    op.create_table(
        "plan_milestones",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column(
            "plan_id",
            sa.Integer,
            sa.ForeignKey("construction_plans.id", ondelete="CASCADE"),
            nullable=False,
        ),
        sa.Column("project_id", sa.Integer, sa.ForeignKey("projects.id"), nullable=False),
        sa.Column("week_number", sa.Integer, nullable=False),
        sa.Column("title", sa.String(512), nullable=False),
        sa.Column("description", sa.Text),
        sa.Column("expected_state", sa.Text),
        sa.Column("actual_state", sa.Text),
        sa.Column("status", sa.String(32), nullable=False, server_default="not_started"),
        sa.Column("checked_at", sa.DateTime(timezone=True)),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )

    op.create_table(
        "progress_reports",
        sa.Column("id", sa.Integer, primary_key=True, autoincrement=True),
        sa.Column("project_id", sa.Integer, sa.ForeignKey("projects.id"), nullable=False),
        sa.Column("report_type", sa.String(32), nullable=False, server_default="custom"),
        sa.Column("content_md", sa.Text),
        sa.Column("summary", sa.Text),
        sa.Column("date_range_start", sa.Date),
        sa.Column("date_range_end", sa.Date),
        sa.Column("image_count", sa.Integer),
        sa.Column("model_used", sa.String(128)),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.text("NOW()")),
    )


def downgrade() -> None:
    op.drop_table("progress_reports")
    op.drop_table("plan_milestones")
    op.drop_table("construction_plans")
