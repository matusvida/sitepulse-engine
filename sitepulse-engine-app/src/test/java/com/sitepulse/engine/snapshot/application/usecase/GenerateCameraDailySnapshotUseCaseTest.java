package com.sitepulse.engine.snapshot.application.usecase;

import com.sitepulse.engine.common.domain.model.ImageFormat;
import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.ExternalServiceException;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotSourceDecision;
import com.sitepulse.engine.snapshot.application.service.CameraSnapshotProfileService;
import com.sitepulse.engine.snapshot.application.service.SnapshotKeyFactory;
import com.sitepulse.engine.snapshot.application.service.WebImageTransformer;
import com.sitepulse.engine.snapshot.infrastructure.persistence.CameraDailySnapshotEntity;
import com.sitepulse.engine.snapshot.infrastructure.persistence.CameraDailySnapshotRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateCameraDailySnapshotUseCaseTest {

    @Test
    void fallsBackToNextCandidateWhenPrimarySourceObjectIsMissing() throws Exception {
        var first = imageEntity(101, "missing.jpg");
        var second = imageEntity(102, "available.jpg");
        ResolveCameraSnapshotSourceUseCase resolveSourceUseCase = new ResolveCameraSnapshotSourceUseCase(null, null, null, null) {
            @Override
            public CameraSnapshotSourceDecision resolve(Project project, Camera camera, LocalDate snapshotDate) {
                return new CameraSnapshotSourceDecision(List.of(first, second), true);
            }
        };
        CameraSnapshotProfileService profileService = new CameraSnapshotProfileService(null, null, null) {
            @Override
            public CameraSnapshotProfile getOrCreate(Integer cameraId) {
                return new CameraSnapshotProfile(cameraId, 1920, 75, ImageFormat.WEBP, LocalTime.of(17, 0));
            }
        };
        RecordingObjectStorage objectStorage = new RecordingObjectStorage(testImageBytes());
        CameraDailySnapshotRepository repository = repository();

        GenerateCameraDailySnapshotUseCase useCase = new GenerateCameraDailySnapshotUseCase(
                resolveSourceUseCase,
                profileService,
                repository,
                new SnapshotKeyFactory(),
                new WebImageTransformer(),
                objectStorage,
                Clock.systemUTC()
        );

        var result = useCase.generate(project(), camera(), LocalDate.of(2026, 3, 4), false);

        assertEquals(102, result.sourceImageId());
        assertEquals(2, objectStorage.downloadAttempts);
        assertTrue(objectStorage.uploaded);
    }

    private static CameraDailySnapshotRepository repository() {
        return (CameraDailySnapshotRepository) Proxy.newProxyInstance(
                CameraDailySnapshotRepository.class.getClassLoader(),
                new Class<?>[] {CameraDailySnapshotRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByCameraIdAndSnapshotDate" -> Optional.empty();
                    case "save" -> {
                        CameraDailySnapshotEntity entity = (CameraDailySnapshotEntity) args[0];
                        entity.setId(1L);
                        yield entity;
                    }
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private static Project project() {
        return Project.restore(1, "Danubius", "Site", "danubius", "Europe/Bratislava",
                OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    }

    private static Camera camera() {
        return Camera.restore(7, 1, "Outside", null, true, "/cam", "cam-outside", 1920, 1080,
                OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    }

    private static com.sitepulse.engine.detection.infrastructure.persistence.ImageEntity imageEntity(int id, String key) {
        return com.sitepulse.engine.detection.infrastructure.persistence.ImageEntity.builder()
                .id(id)
                .bucket("bucket")
                .key(key)
                .capturedAt(OffsetDateTime.of(2026, 3, 4, 11, 56, 4, 0, ZoneOffset.UTC))
                .build();
    }

    private static byte[] testImageBytes() throws Exception {
        BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, ImageFormat.JPEG.getCanonicalExtension(), out);
        return out.toByteArray();
    }

    private static final class RecordingObjectStorage implements ObjectStorage {
        private final byte[] imageBytes;
        private int downloadAttempts;
        private boolean uploaded;

        private RecordingObjectStorage(byte[] imageBytes) {
            this.imageBytes = imageBytes;
        }

        @Override
        public byte[] download(String bucket, String key) {
            downloadAttempts++;
            if ("missing.jpg".equals(key)) {
                throw new ExternalServiceException("missing");
            }
            return imageBytes;
        }

        @Override
        public boolean exists(String bucket, String key) {
            return true;
        }

        @Override
        public void upload(String bucket, String key, byte[] data, String contentType) {
            uploaded = true;
        }

        @Override
        public String defaultBucket() {
            return "bucket";
        }

        @Override
        public String presign(String bucket, String key, java.time.Duration expiresAfter) {
            return "";
        }
    }
}
