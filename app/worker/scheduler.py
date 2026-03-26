"""APScheduler-based entry point for all recurring jobs.

Usage::

    python -m app.worker.scheduler

Runs three scheduled jobs:
  1. Dropbox sync — every SYNC_SCHEDULE_MINUTES (default 60)
  2. Detection worker sweep — every WORKER_POLL_INTERVAL seconds (default 5)
  3. Analysis job — nightly at ANALYSIS_HOUR (default 02:00)
"""

from __future__ import annotations

import sys

import structlog
from apscheduler.schedulers.blocking import BlockingScheduler

from app.core import get_settings
from app.core.logging import configure_logging

configure_logging()
logger = structlog.get_logger(__name__)


def _run_sync() -> None:
    logger.info("scheduler_sync_start")
    try:
        from app.services.sync import run_sync
        run_sync()
    except Exception:
        logger.exception("scheduler_sync_failed")


def _run_detection_sweep() -> None:
    from app.db.images import fetch_new_images
    from app.worker.processor import process_image

    rows = fetch_new_images(limit=10)
    if not rows:
        return

    for row in rows:
        try:
            process_image(row)
        except Exception:
            logger.exception("scheduler_detection_failed", image_id=row["id"])


def _run_analysis() -> None:
    logger.info("scheduler_analysis_start")
    try:
        from app.services.analysis import run_analysis
        run_analysis()
    except Exception:
        logger.exception("scheduler_analysis_failed")


def _run_plan_check() -> None:
    logger.info("scheduler_plan_check_start")
    try:
        from app.services.plan_tracker import run_plan_check_all
        run_plan_check_all()
    except Exception:
        logger.exception("scheduler_plan_check_failed")


def main() -> None:
    cfg = get_settings()
    if not cfg.enable_db:
        logger.error("Scheduler requires ENABLE_DB=true")
        sys.exit(1)

    from app.db.engine import run_migrations
    run_migrations()

    from app.detection.model import load_model
    load_model()

    scheduler = BlockingScheduler()

    if cfg.dropbox_token or cfg.dropbox_refresh_token:
        scheduler.add_job(
            _run_sync, "interval",
            minutes=cfg.sync_schedule_minutes,
            id="dropbox_sync",
            name="Dropbox Sync",
        )
        logger.info("scheduler_job_registered", job="dropbox_sync", interval_min=cfg.sync_schedule_minutes)
    else:
        logger.warning("scheduler_dropbox_skipped", reason="DROPBOX_TOKEN not set")

    scheduler.add_job(
        _run_detection_sweep, "interval",
        seconds=cfg.worker_poll_interval,
        id="detection_sweep",
        name="Detection Sweep",
    )
    logger.info("scheduler_job_registered", job="detection_sweep", interval_sec=cfg.worker_poll_interval)

    scheduler.add_job(
        _run_analysis, "cron",
        hour=cfg.analysis_hour,
        id="nightly_analysis",
        name="Nightly Analysis",
    )
    logger.info("scheduler_job_registered", job="nightly_analysis", hour=cfg.analysis_hour)

    if cfg.openai_api_key:
        scheduler.add_job(
            _run_plan_check, "cron",
            day_of_week="mon",
            hour=cfg.analysis_hour + 1,
            id="weekly_plan_check",
            name="Weekly Plan Check",
        )
        logger.info("scheduler_job_registered", job="weekly_plan_check", day="mon", hour=cfg.analysis_hour + 1)
    else:
        logger.warning("scheduler_plan_check_skipped", reason="OPENAI_API_KEY not set")

    logger.info("scheduler_started")
    try:
        scheduler.start()
    except KeyboardInterrupt:
        logger.info("scheduler_stopped")


if __name__ == "__main__":
    main()
