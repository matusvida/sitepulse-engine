"""OpenAI GPT-4o integration for construction-site AI analysis.

Three capabilities:
1. Parse a construction plan PDF (text) into structured milestones.
2. Generate a progress report from site photos + metrics.
3. Evaluate a single milestone against recent site photos.
"""

from __future__ import annotations

import base64
import json
from typing import Any, Dict, List, Optional

import structlog
from openai import OpenAI

from app.core import get_settings

logger = structlog.get_logger(__name__)

_client: Optional[OpenAI] = None


def _get_client() -> OpenAI:
    global _client
    if _client is not None:
        return _client
    cfg = get_settings()
    if not cfg.openai_api_key:
        raise RuntimeError(
            "OPENAI_API_KEY is not configured. "
            "Set it in .env or as an environment variable."
        )
    _client = OpenAI(api_key=cfg.openai_api_key, timeout=120)
    return _client


def _encode_image(img_bytes: bytes) -> str:
    return base64.b64encode(img_bytes).decode("utf-8")


# ---------------------------------------------------------------------------
# 1) Parse construction plan into milestones
# ---------------------------------------------------------------------------

_PLAN_SYSTEM_PROMPT = """\
You are a construction project analyst. You receive the text extracted from
a construction plan PDF.  Your job is to identify the key milestones / phases
and return them as a JSON array.

Each milestone object MUST have these fields:
- "week_number": integer — the week by which this milestone should be completed
- "title": string — short name (max 80 chars)
- "description": string — 1-2 sentence explanation
- "expected_state": string — what the site should look like when this milestone is done

Sort by week_number ascending.  If the plan uses months instead of weeks,
convert to approximate week numbers (month 1 = week 4, month 2 = week 8, etc.).
If no clear timeline exists, estimate reasonable week numbers based on
typical construction sequencing.

Return ONLY a JSON array — no markdown fences, no commentary."""


def parse_plan_milestones(pdf_text: str) -> List[Dict[str, Any]]:
    """Send extracted PDF text to GPT-4o and get structured milestones back."""
    client = _get_client()
    cfg = get_settings()

    logger.info("llm_parse_plan", text_length=len(pdf_text))

    resp = client.chat.completions.create(
        model=cfg.openai_model,
        response_format={"type": "json_object"},
        messages=[
            {"role": "system", "content": _PLAN_SYSTEM_PROMPT},
            {
                "role": "user",
                "content": (
                    "Here is the construction plan text:\n\n"
                    f"{pdf_text[:30_000]}"
                ),
            },
        ],
        temperature=0.2,
        max_tokens=4096,
    )

    raw = resp.choices[0].message.content or "{}"
    parsed = json.loads(raw)

    milestones = parsed if isinstance(parsed, list) else parsed.get("milestones", [])
    logger.info("llm_parse_plan_done", milestones=len(milestones))
    return milestones


# ---------------------------------------------------------------------------
# 2) Generate a progress report from photos + metrics
# ---------------------------------------------------------------------------

_REPORT_SYSTEM_PROMPT = """\
You are a construction progress analyst for SitePulse, a site-monitoring
platform. You will receive:
- A set of site photos (from different dates within the report period)
- Metrics data (daily/weekly activity, detections)
- Current plan milestone statuses (if available)

Write a professional, markdown-formatted progress report covering:
1. **Executive Summary** — 2-3 sentence overview
2. **Visual Progress** — describe what changed between the earliest and
   latest photos (structural changes, new floors, equipment, scaffolding, etc.)
3. **Activity Analysis** — summarise the metrics (people/vehicle counts,
   active hours trends)
4. **Plan Compliance** — compare actual state against milestones (if provided)
5. **Risk Assessment** — flag any concerns or delays
6. **Recommendations** — concrete next-step suggestions

Be concise but specific.  Reference approximate dates when describing changes.
Use markdown headers, bullet points, and bold for emphasis."""


def generate_progress_report(
    image_data: List[Dict[str, Any]],
    metrics_context: str,
    milestones_context: str,
) -> str:
    """Call GPT-4o Vision with site photos and metrics to produce a report.

    Parameters
    ----------
    image_data
        List of dicts with keys ``date`` (str) and ``b64`` (base64-encoded JPEG).
    metrics_context
        Pre-formatted text block summarising daily/weekly metrics.
    milestones_context
        Pre-formatted text block describing current plan milestones.
    """
    client = _get_client()
    cfg = get_settings()

    content_parts: List[Dict[str, Any]] = []

    text_block = "Generate a construction progress report.\n\n"
    if metrics_context:
        text_block += f"## Metrics\n{metrics_context}\n\n"
    if milestones_context:
        text_block += f"## Plan Milestones\n{milestones_context}\n\n"
    text_block += "## Site Photos\nBelow are site photos from the report period:\n"
    content_parts.append({"type": "text", "text": text_block})

    for img in image_data:
        content_parts.append({"type": "text", "text": f"Photo date: {img['date']}"})
        content_parts.append({
            "type": "image_url",
            "image_url": {
                "url": f"data:image/jpeg;base64,{img['b64']}",
                "detail": "low",
            },
        })

    logger.info("llm_generate_report", images=len(image_data))

    resp = client.chat.completions.create(
        model=cfg.openai_model,
        messages=[
            {"role": "system", "content": _REPORT_SYSTEM_PROMPT},
            {"role": "user", "content": content_parts},
        ],
        temperature=0.3,
        max_tokens=4096,
    )

    report_md = resp.choices[0].message.content or ""
    logger.info("llm_generate_report_done", length=len(report_md))
    return report_md


# ---------------------------------------------------------------------------
# 3) Evaluate a milestone against recent photos
# ---------------------------------------------------------------------------

_EVAL_SYSTEM_PROMPT = """\
You are a construction milestone evaluator.  You will receive:
- A milestone description and expected state
- Recent site photos

Determine whether the milestone is met based on visual evidence.
Return ONLY a JSON object with these fields:
- "status": one of "completed", "on_track", "delayed", "not_started"
- "actual_state": 1-2 sentence description of what you see in the photos
  that is relevant to this milestone
- "confidence": float 0-1 expressing how confident you are

No markdown fences, no extra commentary — just the JSON object."""


def evaluate_milestone(
    milestone_title: str,
    expected_state: str,
    image_bytes_list: List[bytes],
) -> Dict[str, Any]:
    """Send milestone info + photos to GPT-4o, get status assessment."""
    client = _get_client()
    cfg = get_settings()

    content_parts: List[Dict[str, Any]] = [
        {
            "type": "text",
            "text": (
                f"Milestone: {milestone_title}\n"
                f"Expected state: {expected_state}\n\n"
                "Evaluate the following site photos against this milestone:"
            ),
        },
    ]
    for img_bytes in image_bytes_list[:5]:
        b64 = _encode_image(img_bytes)
        content_parts.append({
            "type": "image_url",
            "image_url": {
                "url": f"data:image/jpeg;base64,{b64}",
                "detail": "low",
            },
        })

    logger.info("llm_evaluate_milestone", title=milestone_title, images=len(image_bytes_list))

    resp = client.chat.completions.create(
        model=cfg.openai_model,
        response_format={"type": "json_object"},
        messages=[
            {"role": "system", "content": _EVAL_SYSTEM_PROMPT},
            {"role": "user", "content": content_parts},
        ],
        temperature=0.2,
        max_tokens=1024,
    )

    raw = resp.choices[0].message.content or "{}"
    result = json.loads(raw)
    logger.info("llm_evaluate_milestone_done", status=result.get("status"))
    return result
