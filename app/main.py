"""FastAPI application factory with lifespan management."""

from __future__ import annotations

from contextlib import asynccontextmanager

import structlog
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api import router
from app.core import get_settings
from app.core.logging import configure_logging
from app.detection.model import load_model

configure_logging()
logger = structlog.get_logger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("startup", msg="Loading YOLO model…")
    load_model()

    cfg = get_settings()
    if cfg.enable_db:
        from app.db.engine import run_migrations
        logger.info("db_init", dsn=cfg.postgres_dsn.split("@")[-1])
        run_migrations()

    logger.info("startup_complete")
    yield
    logger.info("shutdown")


app = FastAPI(
    title="SitePulse Engine",
    version="0.2.0",
    lifespan=lifespan,
)

cfg = get_settings()
app.add_middleware(
    CORSMiddleware,
    allow_origins=[o.strip() for o in cfg.cors_origins.split(",")],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(router)


@app.get("/")
async def root():
    return {
        "service": "sitepulse-engine",
        "docs": "/docs",
        "endpoints": ["/health", "/detect", "/api/projects"],
    }
