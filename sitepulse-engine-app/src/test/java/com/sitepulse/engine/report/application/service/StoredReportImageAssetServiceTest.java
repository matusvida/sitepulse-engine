package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.report.domain.model.ProgressReport;
import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import com.sitepulse.engine.snapshot.application.service.WebImageTransformer;
import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoredReportImageAssetServiceTest {

    @Test
    void prepareStoresCompressedImagesUnderReportFolderUsingSourceFileName() {
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        WebImageTransformer webImageTransformer = new WebImageTransformer() {
            @Override
            public TransformedImage transform(byte[] sourceBytes, CameraSnapshotProfile profile) {
                return new TransformedImage("compressed".getBytes(), "image/jpeg");
            }
        };
        StoredReportImageAssetService service = new StoredReportImageAssetService(
                objectStorage,
                properties(),
                webImageTransformer
        );

        ProgressReport report = ProgressReport.restore(
                44,
                1,
                "weekly",
                "automatic",
                "weekly:2026-04-27",
                "high",
                "SK",
                "content",
                "headline",
                "summary",
                java.time.LocalDate.of(2026, 4, 27),
                java.time.LocalDate.of(2026, 5, 3),
                4,
                4,
                "gpt-4o",
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        ReportImageEvidence evidenceImage = new ReportImageEvidence(
                99,
                "2026-04-28",
                Base64.getEncoder().encodeToString("raw-image".getBytes()),
                OffsetDateTime.of(2026, 4, 28, 17, 16, 4, 0, ZoneOffset.UTC),
                "sitepulse-images",
                "danubius/cam-outside/2026-04-28/2026-04-28 17:16:04.jpg",
                "ignored"
        );

        List<StoredReportImageAssetService.PreparedReportImageAsset> prepared = service.prepare(report, List.of(evidenceImage));

        assertEquals("sitepulse-images", objectStorage.bucket);
        assertEquals("danubius/cam-outside/reports/weekly/2026-04-27/2026-04-28 17:16:04.jpg", objectStorage.key);
        assertEquals("image/jpeg", objectStorage.contentType);
        assertEquals(1, prepared.size());
        var asset = prepared.getFirst();
        assertEquals(99, asset.imageId());
        assertEquals("sitepulse-images", asset.bucket());
        assertEquals("danubius/cam-outside/reports/weekly/2026-04-27/2026-04-28 17:16:04.jpg", asset.imagePath());
    }

    private SitePulseProperties properties() {
        return new SitePulseProperties(
                "postgresql://sitepulse:sitepulse@localhost:5432/sitepulse",
                "http://localhost:3000",
                "minio",
                "sitepulse-images",
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
        );
    }

    private static final class RecordingObjectStorage implements ObjectStorage {
        private String bucket;
        private String key;
        private String contentType;

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
            this.bucket = bucket;
            this.key = key;
            this.contentType = contentType;
        }

        @Override
        public String defaultBucket() {
            return "sitepulse-images";
        }

        @Override
        public String presign(String bucket, String key, Duration expiresAfter) {
            return "";
        }
    }

}
