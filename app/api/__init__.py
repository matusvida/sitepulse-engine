"""API route aggregation.

All sub-routers are combined here and exposed as a single ``router``
that ``main.py`` includes on the FastAPI app.
"""

from fastapi import APIRouter

from app.api.detect import router as detect_router
from app.api.projects import router as projects_router

router = APIRouter()
router.include_router(detect_router)
router.include_router(projects_router)
