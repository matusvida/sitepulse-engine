package com.sitepulse.engine.detection.application.command;

public record RunOnDemandDetectionCommand(String bucket, String key, String s3Url) {
}
