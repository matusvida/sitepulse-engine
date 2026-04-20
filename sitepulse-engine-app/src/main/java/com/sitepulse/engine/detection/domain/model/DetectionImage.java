package com.sitepulse.engine.detection.domain.model;

import com.sitepulse.engine.detection.domain.enums.ImageStatus;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DetectionImage {

    @EqualsAndHashCode.Include
    @ToString.Include
    private final Integer id;

    private final String bucket;
    private final String key;

    @ToString.Include
    private ImageStatus status;
    private final Integer projectId;
    private final Integer cameraId;
    private final OffsetDateTime capturedAt;
    private final OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String weatherNote;
    private Double evidenceActivityScore;
    private Double evidenceChangeScore;
    private Double evidenceQualityScore;
    private Double evidenceOverallScore;
    private String evidenceSummary;

    private DetectionImage(
            Integer id,
            String bucket,
            String key,
            ImageStatus status,
            Integer projectId,
            Integer cameraId,
            OffsetDateTime capturedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String weatherNote,
            Double evidenceActivityScore,
            Double evidenceChangeScore,
            Double evidenceQualityScore,
            Double evidenceOverallScore,
            String evidenceSummary
    ) {
        this.id = id;
        this.bucket = bucket;
        this.key = key;
        this.status = status;
        this.projectId = projectId;
        this.cameraId = cameraId;
        this.capturedAt = capturedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.weatherNote = weatherNote;
        this.evidenceActivityScore = evidenceActivityScore;
        this.evidenceChangeScore = evidenceChangeScore;
        this.evidenceQualityScore = evidenceQualityScore;
        this.evidenceOverallScore = evidenceOverallScore;
        this.evidenceSummary = evidenceSummary;
    }

    public static DetectionImage createNew(
            String bucket,
            String key,
            Integer projectId,
            Integer cameraId,
            OffsetDateTime capturedAt,
            OffsetDateTime now
    ) {
        return new DetectionImage(null, bucket, key, ImageStatus.NEW, projectId, cameraId, capturedAt, now, now, null, null, null, null, null, null);
    }

    public static DetectionImage restore(
            Integer id,
            String bucket,
            String key,
            ImageStatus status,
            Integer projectId,
            Integer cameraId,
            OffsetDateTime capturedAt,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String weatherNote,
            Double evidenceActivityScore,
            Double evidenceChangeScore,
            Double evidenceQualityScore,
            Double evidenceOverallScore,
            String evidenceSummary
    ) {
        return new DetectionImage(
                id,
                bucket,
                key,
                status,
                projectId,
                cameraId,
                capturedAt,
                createdAt,
                updatedAt,
                weatherNote,
                evidenceActivityScore,
                evidenceChangeScore,
                evidenceQualityScore,
                evidenceOverallScore,
                evidenceSummary
        );
    }

    public static DetectionImage createDetected(
            String bucket,
            String key,
            Integer projectId,
            Integer cameraId,
            OffsetDateTime capturedAt,
            OffsetDateTime now
    ) {
        return new DetectionImage(null, bucket, key, ImageStatus.DONE, projectId, cameraId, capturedAt, now, now, null, null, null, null, null, null);
    }

    public void applyAnalysisMetadata(
            String weatherNote,
            Double evidenceActivityScore,
            Double evidenceChangeScore,
            Double evidenceQualityScore,
            Double evidenceOverallScore,
            String evidenceSummary
    ) {
        this.weatherNote = normalize(weatherNote);
        this.evidenceActivityScore = evidenceActivityScore;
        this.evidenceChangeScore = evidenceChangeScore;
        this.evidenceQualityScore = evidenceQualityScore;
        this.evidenceOverallScore = evidenceOverallScore;
        this.evidenceSummary = evidenceSummary;
    }

    public void markProcessing(OffsetDateTime now) {
        if (status != ImageStatus.NEW) {
            throw new IllegalStateException("Can only start processing images in NEW state, current: " + status);
        }
        status = ImageStatus.PROCESSING;
        updatedAt = now;
    }

    public void markDone(OffsetDateTime now) {
        if (status != ImageStatus.PROCESSING) {
            throw new IllegalStateException("Can only mark done images in PROCESSING state, current: " + status);
        }
        status = ImageStatus.DONE;
        updatedAt = now;
    }

    public void markFailed(OffsetDateTime now) {
        if (status == ImageStatus.FAILED) {
            updatedAt = now;
            return;
        }
        if (status != ImageStatus.PROCESSING && status != ImageStatus.DONE) {
            throw new IllegalStateException("Can only mark failed images in PROCESSING or DONE state, current: " + status);
        }
        status = ImageStatus.FAILED;
        updatedAt = now;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
