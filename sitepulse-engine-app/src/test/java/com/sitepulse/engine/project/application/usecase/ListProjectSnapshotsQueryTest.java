package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.project.application.result.ProjectSnapshotMetadataResult;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ListProjectSnapshotsQueryTest {

    @Test
    void listReturnsOrderedSnapshotMetadataWithSignedUrls() {
        StoredImage first = new StoredImage(
                10,
                "bucket",
                "img-1.jpg",
                OffsetDateTime.of(2024, 6, 15, 10, 0, 0, 0, ZoneOffset.UTC)
        );
        StoredImage second = new StoredImage(
                11,
                "bucket",
                "img-2.png",
                OffsetDateTime.of(2024, 6, 16, 11, 0, 0, 0, ZoneOffset.UTC)
        );
        ProcessedImageReadModel processedImageReadModel = new ProcessedImageReadModel() {
            @Override
            public List<OffsetDateTime> findSnapshotCapturedAtValues(Integer projectId) {
                return List.of();
            }

            @Override
            public List<StoredImage> findRepresentativeSnapshots(Integer projectId) {
                return List.of(first, second);
            }

            @Override
            public Optional<StoredImage> findClosestSnapshot(Integer projectId, OffsetDateTime dayStart, OffsetDateTime dayEnd, OffsetDateTime midday) {
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
        };
        RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        ListProjectSnapshotsQuery query = new ListProjectSnapshotsQuery(
                processedImageReadModel,
                objectStorage,
                testProperties()
        );

        List<ProjectSnapshotMetadataResult> results = query.list(1);

        assertEquals(2, results.size());
        assertEquals(first.getCapturedAt().toLocalDate(), results.get(0).date());
        assertEquals("http://example.test/a", results.get(0).url());
        assertEquals("image/jpeg", results.get(0).mediaType());
        assertNotNull(results.get(0).expiresAt());
        assertEquals(second.getCapturedAt().toLocalDate(), results.get(1).date());
        assertEquals("http://example.test/b", results.get(1).url());
        assertEquals("image/png", results.get(1).mediaType());
        assertNotNull(results.get(1).expiresAt());
        assertEquals(results.get(0).expiresAt(), results.get(1).expiresAt());

        assertEquals(2, objectStorage.calls.size());
        assertEquals("bucket", objectStorage.calls.get(0).bucket());
        assertEquals("img-1.jpg", objectStorage.calls.get(0).key());
        assertEquals(Duration.ofMinutes(60), objectStorage.calls.get(0).expiresAfter());
        assertEquals("bucket", objectStorage.calls.get(1).bucket());
        assertEquals("img-2.png", objectStorage.calls.get(1).key());
        assertEquals(Duration.ofMinutes(60), objectStorage.calls.get(1).expiresAfter());
    }

    private static SitePulseProperties testProperties() {
        return new SitePulseProperties(
                "postgresql://sitepulse:sitepulse@localhost:5432/sitepulse",
                "http://localhost:3000",
                "minio",
                "tower-tl",
                60,
                new SitePulseProperties.MinioProperties(
                        "http://minio:9000",
                        "http://localhost:9001",
                        "admin",
                        "password123"
                ),
                new SitePulseProperties.GcsProperties(
                        "",
                        ""
                ),
                "yolov8x.pt",
                0.35,
                "{}",
                400.0,
                false,
                "roi_config.json",
                50.0,
                30,
                240,
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
                "",
                "gpt-4.1",
                52_428_800L,
                "http://python-yolo:8000"
        );
    }

    private static final class RecordingObjectStorage implements ObjectStorage {

        private final List<PresignCall> calls = new ArrayList<>();

        @Override
        public byte[] download(String bucket, String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean exists(String bucket, String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void upload(String bucket, String key, byte[] data, String contentType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String defaultBucket() {
            return "bucket";
        }

        @Override
        public String presign(String bucket, String key, Duration expiresAfter) {
            calls.add(new PresignCall(bucket, key, expiresAfter));
            return "img-1.jpg".equals(key) ? "http://example.test/a" : "http://example.test/b";
        }
    }

    private record PresignCall(String bucket, String key, Duration expiresAfter) {
    }
}
