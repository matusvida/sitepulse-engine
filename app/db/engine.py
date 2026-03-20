"""Database engine singleton and migration runner."""

from __future__ import annotations

from sqlalchemy import create_engine
from sqlalchemy.engine import Engine

from app.core import get_settings

_engine: Engine | None = None


def get_engine() -> Engine:
    global _engine
    if _engine is None:
        cfg = get_settings()
        _engine = create_engine(cfg.postgres_dsn, pool_size=5, max_overflow=2)
    return _engine


def run_migrations() -> None:
    """Run Alembic migrations up to head."""
    from pathlib import Path

    from alembic import command
    from alembic.config import Config

    alembic_ini = Path(__file__).resolve().parent.parent.parent / "alembic.ini"
    alembic_cfg = Config(str(alembic_ini))
    alembic_cfg.set_main_option("sqlalchemy.url", get_settings().postgres_dsn)
    command.upgrade(alembic_cfg, "head")
