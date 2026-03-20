"""S3-compatible (MinIO) client wrapper with robust error handling."""

from __future__ import annotations

import io
import re
from typing import Tuple

import boto3
import structlog
from botocore.config import Config as BotoConfig
from botocore.exceptions import (
    ClientError,
    EndpointConnectionError,
    NoCredentialsError,
)

from app.core import get_settings

logger = structlog.get_logger(__name__)

_s3_client = None


def _get_client():
    global _s3_client
    if _s3_client is not None:
        return _s3_client

    cfg = get_settings()
    _s3_client = boto3.client(
        "s3",
        endpoint_url=cfg.minio_endpoint,
        aws_access_key_id=cfg.minio_access_key,
        aws_secret_access_key=cfg.minio_secret_key,
        config=BotoConfig(
            signature_version="s3v4",
            s3={"addressing_style": "path"},
            connect_timeout=10,
            read_timeout=30,
            retries={"max_attempts": 2},
        ),
        region_name="us-east-1",
    )
    return _s3_client


class S3DownloadError(Exception):
    """Raised when an S3 download fails for any expected reason."""


class S3UploadError(Exception):
    """Raised when an S3 upload fails."""


def parse_s3_url(url: str) -> Tuple[str, str]:
    """Parse ``s3://bucket/key`` into (bucket, key)."""
    match = re.match(r"^s3://([^/]+)/(.+)$", url)
    if not match:
        raise ValueError(f"Invalid S3 URL: {url}")
    return match.group(1), match.group(2)


def download_image_bytes(bucket: str, key: str) -> bytes:
    """Download an object from S3 into memory.

    Raises ``S3DownloadError`` on any expected failure (auth, missing key,
    network, oversized object).
    """
    cfg = get_settings()
    client = _get_client()

    try:
        head = client.head_object(Bucket=bucket, Key=key)
    except ClientError as exc:
        code = exc.response["Error"]["Code"]
        if code in ("404", "NoSuchKey"):
            raise S3DownloadError(f"Object not found: s3://{bucket}/{key}") from exc
        if code in ("403", "AccessDenied"):
            raise S3DownloadError(f"Access denied for s3://{bucket}/{key}") from exc
        raise S3DownloadError(f"S3 error ({code}): {exc}") from exc
    except NoCredentialsError as exc:
        raise S3DownloadError("Missing S3 credentials") from exc
    except EndpointConnectionError as exc:
        raise S3DownloadError(f"Cannot reach S3 endpoint {cfg.minio_endpoint}") from exc

    content_length = head.get("ContentLength", 0)
    if content_length > cfg.max_image_bytes:
        raise S3DownloadError(
            f"Object too large ({content_length} bytes, limit {cfg.max_image_bytes})"
        )

    try:
        buf = io.BytesIO()
        client.download_fileobj(bucket, key, buf)
        buf.seek(0)
        data = buf.read()
        logger.info("s3_download_ok", bucket=bucket, key=key, size_bytes=len(data))
        return data
    except ClientError as exc:
        raise S3DownloadError(f"Download failed: {exc}") from exc
    except EndpointConnectionError as exc:
        raise S3DownloadError(f"Network error during download: {exc}") from exc


def upload_bytes(bucket: str, key: str, data: bytes, content_type: str = "image/jpeg") -> None:
    """Upload raw bytes to S3/MinIO.  Creates the bucket if it doesn't exist."""
    client = _get_client()
    try:
        try:
            client.head_bucket(Bucket=bucket)
        except ClientError:
            client.create_bucket(Bucket=bucket)

        client.put_object(Bucket=bucket, Key=key, Body=data, ContentType=content_type)
        logger.info("s3_upload_ok", bucket=bucket, key=key, size_bytes=len(data))
    except (ClientError, EndpointConnectionError, NoCredentialsError) as exc:
        raise S3UploadError(f"Upload failed for s3://{bucket}/{key}: {exc}") from exc
