"""Cheap image-quality heuristics.

These are intentionally lightweight so they add negligible latency even on
CPU.  They return warning strings (empty list = all clear).
"""

from __future__ import annotations

from typing import List

import cv2
import numpy as np

from app.core import get_settings


def check_quality(image_bgr: np.ndarray) -> List[str]:
    """Return a list of human-readable warning strings."""
    cfg = get_settings()
    warnings: List[str] = []

    gray = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2GRAY)

    blur_var = cv2.Laplacian(gray, cv2.CV_64F).var()
    if blur_var < cfg.blur_threshold:
        warnings.append(
            f"Image appears blurry (laplacian_var={blur_var:.1f}, threshold={cfg.blur_threshold})"
        )

    mean_brightness = float(np.mean(gray))
    if mean_brightness < cfg.brightness_low:
        warnings.append(
            f"Image is very dark (mean_brightness={mean_brightness:.1f}, threshold={cfg.brightness_low})"
        )
    elif mean_brightness > cfg.brightness_high:
        warnings.append(
            f"Image is overexposed (mean_brightness={mean_brightness:.1f}, threshold={cfg.brightness_high})"
        )

    return warnings
