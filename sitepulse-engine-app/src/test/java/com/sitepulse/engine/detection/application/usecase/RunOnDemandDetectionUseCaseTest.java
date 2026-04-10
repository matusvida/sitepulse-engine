package com.sitepulse.engine.detection.application.usecase;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.detection.application.command.RunOnDemandDetectionCommand;
import com.sitepulse.engine.detection.application.result.DetectionOutcomeResult;
import com.sitepulse.engine.detection.application.service.DetectionExecutionResult;
import com.sitepulse.engine.detection.application.service.DetectionExecutionService;
import com.sitepulse.engine.detection.application.service.DetectionTrackingService;
import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.DetectionInference;
import com.sitepulse.engine.detection.domain.model.DetectionOutcome;
import com.sitepulse.engine.detection.domain.model.DetectionProvider;
import com.sitepulse.engine.detection.domain.model.RawDetection;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.detection.domain.port.DetectionImageRepository;
import com.sitepulse.engine.detection.domain.port.DetectionRecordRepository;
import com.sitepulse.engine.detection.domain.service.DetectionPostProcessor;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RunOnDemandDetectionUseCaseTest {

    @Test
    void runFallsBackToDefaultBucketWhenBucketMissing() {
        Fixtures fixtures = new Fixtures();
        fixtures.objectStorage.defaultBucket = "default-bucket";

        DetectionOutcomeResult result = fixtures.useCase.run(new RunOnDemandDetectionCommand(null, "images/site-a.jpg", null));

        assertEquals(1, fixtures.objectStorage.downloadCalls.size());
        assertEquals("default-bucket", fixtures.objectStorage.downloadCalls.get(0).bucket());
        assertEquals("images/site-a.jpg", fixtures.objectStorage.downloadCalls.get(0).key());
        assertEquals("bucket", result.bucket());
        assertEquals("key", result.key());
        assertFalse(result.skipped());
    }

    @Test
    void runParsesAnyStorageSchemeInUrl() {
        Fixtures fixtures = new Fixtures();

        DetectionOutcomeResult result = fixtures.useCase.run(new RunOnDemandDetectionCommand(null, null, "s3://archive-bucket/2024/06/site-a.png"));

        assertEquals(1, fixtures.objectStorage.downloadCalls.size());
        assertEquals("archive-bucket", fixtures.objectStorage.downloadCalls.get(0).bucket());
        assertEquals("2024/06/site-a.png", fixtures.objectStorage.downloadCalls.get(0).key());
        assertEquals("bucket", result.bucket());
        assertEquals("key", result.key());
        assertFalse(result.skipped());
    }

    private static final class Fixtures {

        private final RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        private final DetectionExecutionService detectionExecutionService = new DetectionExecutionService(null, null, null, null, null, null) {
            @Override
            public DetectionExecutionResult execute(DetectionImage image, byte[] imageBytes) {
                return new DetectionExecutionResult(
                        DetectionProvider.OPENAI,
                        new DetectionInference("model", 1920, 1080, 12.5, List.of()),
                        101
                );
            }
        };
        private final DetectionTrackingService detectionTrackingService = new DetectionTrackingService(null, null) {
            @Override
            public List<DetectedObject> assignTracks(DetectionImage image, List<DetectedObject> detections, DetectionProvider provider) {
                return detections;
            }
        };
        private final DetectionImageRepository detectionImageRepository = new DetectionImageRepository() {
            @Override
            public List<DetectionImage> claimPendingImages(int limit) {
                return List.of();
            }

            @Override
            public Optional<DetectionImage> findById(Integer imageId) {
                return Optional.empty();
            }

            @Override
            public boolean existsByBucketAndKey(String bucket, String key) {
                return false;
            }

            @Override
            public DetectionImage save(DetectionImage image) {
                return image;
            }
        };
        private final DetectionRecordRepository detectionRecordRepository = (imageId, projectId, modelVersion, analysisRunId, detections) -> {
        };
        private final CameraLookup cameraLookup = new CameraLookup() {
            @Override
            public Integer findCameraIdByProjectAndKey(Integer projectId, String key) {
                return null;
            }

            @Override
            public CameraRoiSettings findRoiSettings(Integer projectId, String key) {
                return null;
            }

            @Override
            public Integer findImageWidth(Integer projectId, String key) {
                return null;
            }

            @Override
            public Integer findImageHeight(Integer projectId, String key) {
                return null;
            }
        };
        private final DetectionPostProcessor detectionPostProcessor = new DetectionPostProcessor(null, null) {
            @Override
            public DetectionOutcome process(String bucket, String key, byte[] imageBytes, DetectionInference inference, CameraRoiSettings cameraRoiSettings) {
                return new DetectionOutcome(
                        "model",
                        "bucket",
                        "key",
                        1920,
                        1080,
                        12.5,
                        List.of(),
                        List.of(),
                        false
                );
            }
        };
        private final RunOnDemandDetectionUseCase useCase = new RunOnDemandDetectionUseCase(
                objectStorage,
                detectionExecutionService,
                detectionTrackingService,
                detectionImageRepository,
                detectionRecordRepository,
                cameraLookup,
                detectionPostProcessor
        );
    }

    private static final class RecordingObjectStorage implements ObjectStorage {

        private final List<DownloadCall> downloadCalls = new ArrayList<>();
        private String defaultBucket = "bucket";

        @Override
        public byte[] download(String bucket, String key) {
            downloadCalls.add(new DownloadCall(bucket, key));
            return new byte[] {1};
        }

        @Override
        public boolean exists(String bucket, String key) {
            return false;
        }

        @Override
        public void upload(String bucket, String key, byte[] data, String contentType) {
        }

        @Override
        public String defaultBucket() {
            return defaultBucket;
        }

        @Override
        public String presign(String bucket, String key, java.time.Duration expiresAfter) {
            return "";
        }
    }

    private record DownloadCall(String bucket, String key) {
    }
}
