"""Shared structlog configuration.

Import ``configure_logging()`` once at process startup (API lifespan,
worker main, scheduler main) to get consistent structured logging
across all entry points.
"""

from __future__ import annotations

import structlog

_configured = False


def configure_logging() -> None:
    """Idempotent structlog setup — safe to call multiple times."""
    global _configured
    if _configured:
        return

    structlog.configure(
        processors=[
            structlog.contextvars.merge_contextvars,
            structlog.processors.add_log_level,
            structlog.processors.TimeStamper(fmt="iso"),
            structlog.dev.ConsoleRenderer(),
        ],
        wrapper_class=structlog.make_filtering_bound_logger(0),
        context_class=dict,
        logger_factory=structlog.PrintLoggerFactory(),
        cache_logger_on_first_use=True,
    )
    _configured = True
