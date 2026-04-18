package com.sitepulse.engine.detection.application.service;

public record ImageEvidenceFeatures(
        String weatherNote,
        Double activityScore,
        Double changeScore,
        Double qualityScore,
        Double overallScore,
        String summaryJson
) {
}
