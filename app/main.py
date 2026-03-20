"""FastAPI application factory with lifespan management."""

from __future__ import annotations

from contextlib import asynccontextmanager

import structlog
from fastapi import FastAPI

from app.api import router
from app.core.logging import configure_logging
from app.detection.model import load_model

configure_logging()
logger = structlog.get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("startup", msg="Loading YOLO model…")
    load_model()

    from app.core import get_settings
    cfg = get_settings()
    if cfg.enable_db:
        from app.db.engine import run_migrations
        logger.info("db_init", dsn=cfg.postgres_dsn.split("@")[-1])
        run_migrations()

    logger.info("startup_complete")
    yield
    logger.info("shutdown")


app = FastAPI(
    title="Construction Site Object Detection",
    version="0.1.0",
    lifespan=lifespan,
)
app.include_router(router)


@app.get("/")
async def root():
    return {
        "service": "sitepulse-engine",
        "docs": "/docs",
        "endpoints": ["/health", "/detect"],
    }
