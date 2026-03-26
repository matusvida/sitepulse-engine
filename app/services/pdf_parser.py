"""Extract and clean text from uploaded PDF files using pdfplumber."""

from __future__ import annotations

import io
import re

import pdfplumber
import structlog

logger = structlog.get_logger(__name__)


def extract_text(pdf_bytes: bytes) -> str:
    """Extract all text content from a PDF byte stream.

    Returns a cleaned, concatenated string of all pages.
    """
    pages_text: list[str] = []
    with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
        for i, page in enumerate(pdf.pages):
            text = page.extract_text() or ""
            if text.strip():
                pages_text.append(text)

    raw = "\n\n".join(pages_text)
    cleaned = _clean_text(raw)
    logger.info("pdf_extract", pages=len(pages_text), chars=len(cleaned))
    return cleaned


def _clean_text(text: str) -> str:
    """Normalize whitespace and remove common PDF artefacts."""
    text = re.sub(r"\x00", "", text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()
