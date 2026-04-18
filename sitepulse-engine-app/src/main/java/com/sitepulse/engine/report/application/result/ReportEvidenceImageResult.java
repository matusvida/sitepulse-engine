package com.sitepulse.engine.report.application.result;

import java.time.OffsetDateTime;

public record ReportEvidenceImageResult(
        String capturedAt,
        String date,
        String url,
        String key
) {
    public static ReportEvidenceImageResult of(OffsetDateTime capturedAt, String date, String url, String key) {
        return new ReportEvidenceImageResult(
                capturedAt == null ? null : capturedAt.toString(),
                date,
                url,
                key
        );
    }
}
