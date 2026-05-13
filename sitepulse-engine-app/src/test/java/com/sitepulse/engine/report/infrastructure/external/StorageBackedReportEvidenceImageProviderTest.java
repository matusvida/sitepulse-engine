package com.sitepulse.engine.report.infrastructure.external;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageBackedReportEvidenceImageProviderTest {

    @Test
    void selectEvidenceUsesCustomStrategyForRangesLongerThanOneWeek() {
        StorageBackedReportEvidenceImageProvider provider = new StorageBackedReportEvidenceImageProvider(
                new ProcessedImageReadModel() {
                    @Override
                    public List<OffsetDateTime> findSnapshotCapturedAtValues(Integer projectId) {
                        return List.of();
                    }

                    @Override
                    public List<StoredImage> findRepresentativeSnapshots(Integer projectId) {
                        return List.of();
                    }

                    @Override
                    public Optional<StoredImage> findClosestSnapshot(Integer projectId, OffsetDateTime dayStart, OffsetDateTime dayEnd, OffsetDateTime midday) {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<StoredImage> findPreviousDoneImage(Integer projectId, Integer cameraId, OffsetDateTime capturedAt, Integer imageId) {
                        return Optional.empty();
                    }

                    @Override
                    public List<StoredImage> findDoneInRange(Integer projectId, OffsetDateTime from, OffsetDateTime to) {
                        return List.of();
                    }

                    @Override
                    public List<StoredImage> findProcessedByProject(Integer projectId) {
                        return List.of();
                    }

                    @Override
                    public List<DetectedObject> findDetections(Integer imageId) {
                        return List.of();
                    }
                },
                new ObjectStorage() {
                    @Override
                    public byte[] download(String bucket, String key) {
                        return new byte[0];
                    }

                    @Override
                    public boolean exists(String bucket, String key) {
                        return false;
                    }

                    @Override
                    public void upload(String bucket, String key, InputStream data, long size, String contentType) {
                    }

                    @Override
                    public String defaultBucket() {
                        return "bucket";
                    }

                    @Override
                    public String presign(String bucket, String key, Duration expiresAfter) {
                        return "";
                    }
                },
                new SitePulseProperties(
                        "postgresql://sitepulse:sitepulse@localhost:5432/sitepulse",
                        "http://localhost:3000",
                        "minio",
                        "tower-tl",
                        60,
                        new SitePulseProperties.StorageProperties(
                                "http://minio:9000",
                                "http://localhost:9001",
                                "admin",
                                "password123",
                                "us-east-1"
                        ),
                        "yolov8x.pt",
                        0.35,
                        "{}",
                        25.0,
                        false,
                        "roi_config.json",
                        0.0,
                        0,
                        255,
                        false,
                        "0 0/10 * * * *",
                        "0 5/10 * * * *",
                        "openai",
                        "0 0 2 * * *",
                        3,
                        "",
                        "",
                        "",
                        "",
                        "test-key",
                        "gpt-4.1",
                        52_428_800L,
                        "http://python-yolo:8000",
                        new SitePulseProperties.ImageWebSnapshotsProperties(false, 1920, 75, ImageFormat.WEBP, java.time.LocalTime.of(17, 0)),
                        new SitePulseProperties.AuthProperties(
                                "http://localhost:3000",
                                "sitepulse_session",
                                false,
                                "Lax",
                                168,
                                72,
                                1,
                                null,
                                null,
                                new SitePulseProperties.MailProperties(
                                        true,
                                        "SitePulse",
                                        "SitePulse <noreply@example.com>",
                                        null,
                                        new SitePulseProperties.ResendProperties(
                                                true,
                                                "https://api.resend.com",
                                                "re_test"
                                        )
                                )
                        )
                )
        );

        List<StoredImage> rows = List.of(
                image(1, 0, 2.0, 1.0),
                image(2, 1, 2.1, 1.0),
                image(3, 2, 2.2, 1.0),
                image(4, 3, 2.3, 1.0),
                image(5, 4, 2.4, 1.0),
                image(6, 5, 2.5, 1.0),
                image(7, 6, 2.6, 1.0),
                image(8, 7, 9.5, 9.0)
        );

        List<StoredImage> selected = provider.selectEvidence(
                rows,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 8),
                6
        );

        assertTrue(selected.stream().anyMatch(image -> image.getId() == 8));
    }

    private StoredImage image(int id, int dayOffset, double overallScore, double changeScore) {
        return new StoredImage(
                id,
                "bucket",
                "image-" + id,
                OffsetDateTime.of(2026, 4, 1 + dayOffset, 9, 0, 0, 0, ZoneOffset.UTC),
                "overcast",
                2.0,
                changeScore,
                4.0,
                overallScore,
                "{\"candidate_tags\":[]}"
        );
    }
}
