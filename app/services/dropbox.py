"""Dropbox client for listing and downloading images from shared folders.

Works with **shared link URLs** — no need to mount the folder or own the
Dropbox account.  Requires a valid API access token (DROPBOX_TOKEN).

The ``dropbox_path`` stored on each project is a full shared link URL, e.g.::

    https://www.dropbox.com/scl/fo/abc123/XYZ/Tower?rlkey=xxx&dl=0

The code parses this into:
  - **shared link base URL** (everything before the subfolder path)
  - **subfolder** (``/Tower`` in this example)

All API calls pass ``shared_link=SharedLink(url=base_url)`` so the Dropbox
SDK resolves entries relative to the shared folder root.
"""

from __future__ import annotations

import re
import time
from dataclasses import dataclass
from typing import List, Optional, Tuple
from urllib.parse import parse_qs, urlencode, urlparse, urlunparse

import dropbox
from dropbox.exceptions import ApiError, AuthError
from dropbox.files import FileMetadata, FolderMetadata, SharedLink
import structlog

from app.core import get_settings

logger = structlog.get_logger(__name__)

_dbx: Optional[dropbox.Dropbox] = None


def _get_client() -> dropbox.Dropbox:
    global _dbx
    if _dbx is not None:
        return _dbx
    cfg = get_settings()
    if not cfg.dropbox_token:
        raise DropboxSyncError("DROPBOX_TOKEN is not set")
    _dbx = dropbox.Dropbox(cfg.dropbox_token, timeout=60)
    return _dbx


class DropboxSyncError(Exception):
    """Raised on any Dropbox operation failure."""


@dataclass
class FileEntry:
    """A file discovered in Dropbox."""
    name: str
    path: str
    size: int


# ── URL parsing ──────────────────────────────────────────────────────────────

def parse_shared_link_url(url: str) -> Tuple[str, str]:
    """Parse a Dropbox shared link URL into (base_url, subfolder_path).

    Example::

        >>> parse_shared_link_url(
        ...     "https://www.dropbox.com/scl/fo/abc/XYZ/Tower?rlkey=xxx&dl=0"
        ... )
        ('https://www.dropbox.com/scl/fo/abc/XYZ?rlkey=xxx&dl=0', '/Tower')

    Returns (base_url_with_rlkey, subfolder_path).
    subfolder_path is '' if the URL points to the shared folder root.
    """
    parsed = urlparse(url)
    path_parts = parsed.path.split("/")

    # ['', 'scl', 'fo', '<id>', '<hash>', ...]
    if len(path_parts) >= 5 and path_parts[1] == "scl" and path_parts[2] == "fo":
        base_path = "/".join(path_parts[:5])
        subfolder = "/".join(path_parts[5:])
    else:
        base_path = parsed.path
        subfolder = ""

    qs = parse_qs(parsed.query)
    keep_params = {}
    if "rlkey" in qs:
        keep_params["rlkey"] = qs["rlkey"][0]
    keep_params["dl"] = "0"

    base_url = urlunparse((
        parsed.scheme, parsed.netloc, base_path,
        "", urlencode(keep_params), "",
    ))

    subfolder_path = f"/{subfolder}" if subfolder else ""

    return base_url, subfolder_path


# ── Listing and downloading ──────────────────────────────────────────────────

def list_subfolders(dropbox_url: str) -> List[str]:
    """List immediate subfolder names under the shared link URL."""
    dbx = _get_client()
    base_url, subfolder = parse_shared_link_url(dropbox_url)
    shared_link = SharedLink(url=base_url)

    folders: List[str] = []
    try:
        result = dbx.files_list_folder(path=subfolder, shared_link=shared_link)
        while True:
            for entry in result.entries:
                if isinstance(entry, FolderMetadata):
                    folders.append(entry.name)
            if not result.has_more:
                break
            result = dbx.files_list_folder_continue(result.cursor)
    except AuthError as exc:
        raise DropboxSyncError(f"Dropbox auth failed: {exc}") from exc
    except ApiError as exc:
        _handle_api_error(exc, dropbox_url)

    logger.info("dropbox_list_subfolders", url=dropbox_url, count=len(folders))
    return sorted(folders)


def list_files(dropbox_url: str, subfolder_name: str) -> List[FileEntry]:
    """List image files (jpg/jpeg/png) inside a date subfolder."""
    dbx = _get_client()
    base_url, root_subfolder = parse_shared_link_url(dropbox_url)
    shared_link = SharedLink(url=base_url)
    folder_path = f"{root_subfolder}/{subfolder_name}" if root_subfolder else f"/{subfolder_name}"

    files: List[FileEntry] = []
    try:
        result = dbx.files_list_folder(path=folder_path, shared_link=shared_link)
        while True:
            for entry in result.entries:
                if isinstance(entry, FileMetadata) and _is_image(entry.name):
                    files.append(FileEntry(
                        name=entry.name,
                        path=f"{folder_path}/{entry.name}",
                        size=entry.size,
                    ))
            if not result.has_more:
                break
            result = dbx.files_list_folder_continue(result.cursor)
    except AuthError as exc:
        raise DropboxSyncError(f"Dropbox auth failed: {exc}") from exc
    except ApiError as exc:
        _handle_api_error(exc, folder_path)

    logger.info("dropbox_list_files", folder=folder_path, count=len(files))
    return files


def download_file(dropbox_url: str, relative_path: str) -> bytes:
    """Download a file from a Dropbox shared link.

    Retries once on transient errors with a 2-second backoff.
    """
    dbx = _get_client()
    base_url, _ = parse_shared_link_url(dropbox_url)

    for attempt in range(2):
        try:
            _meta, response = dbx.sharing_get_shared_link_file(
                url=base_url, path=relative_path,
            )
            data = response.content
            logger.info("dropbox_download_ok", path=relative_path, size_bytes=len(data))
            return data
        except AuthError as exc:
            raise DropboxSyncError(f"Dropbox auth failed: {exc}") from exc
        except ApiError as exc:
            if attempt == 0:
                logger.warning("dropbox_download_retry", path=relative_path, error=str(exc))
                time.sleep(2)
                continue
            _handle_api_error(exc, relative_path)
        except Exception as exc:
            if attempt == 0:
                logger.warning("dropbox_download_retry", path=relative_path, error=str(exc))
                time.sleep(2)
                continue
            raise DropboxSyncError(f"Download failed after retry: {exc}") from exc
    raise DropboxSyncError(f"Download failed: {relative_path}")


# ── Helpers ──────────────────────────────────────────────────────────────────

def _is_image(name: str) -> bool:
    return name.lower().endswith((".jpg", ".jpeg", ".png"))


def _handle_api_error(exc: ApiError, path: str) -> None:
    error = exc.error
    if hasattr(error, "is_path") and error.is_path():
        lookup = error.get_path()
        if hasattr(lookup, "is_not_found") and lookup.is_not_found():
            raise DropboxSyncError(f"Path not found: {path}") from exc
    raise DropboxSyncError(f"Dropbox API error at {path}: {exc}") from exc
