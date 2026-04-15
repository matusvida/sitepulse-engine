package com.sitepulse.engine.snapshot.application.usecase;

import com.sitepulse.engine.common.domain.model.ImageFormat;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageRepository;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotSourceDecision;
import com.sitepulse.engine.snapshot.application.service.CameraSnapshotProfileService;
import com.sitepulse.engine.snapshot.application.service.SnapshotTimezoneResolver;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResolveCameraSnapshotSourceUseCaseTest {

    @Test
    void todayBeforeCutoffUsesLatestImageAndRemainsMutable() {
        RecordingImageRepository imageRepository = new RecordingImageRepository(List.of(imageEntity(101)), List.of());
        CameraSnapshotProfileService profileService = profileService();

        ResolveCameraSnapshotSourceUseCase useCase = new ResolveCameraSnapshotSourceUseCase(
                imageRepository.proxy(),
                profileService,
                new SnapshotTimezoneResolver(),
                Clock.fixed(Instant.parse("2026-04-15T12:00:00Z"), ZoneOffset.UTC)
        );

        CameraSnapshotSourceDecision decision = useCase.resolve(project(), camera(), LocalDate.of(2026, 4, 15));

        assertFalse(decision.frozen());
        assertTrue(decision.sourceImages().getFirst().getId().equals(101));
        assertTrue(imageRepository.latestCalled);
        assertFalse(imageRepository.representativeCalled);
    }

    @Test
    void todayAfterCutoffUsesRepresentativeImageAndFreezes() {
        RecordingImageRepository imageRepository = new RecordingImageRepository(List.of(), List.of(imageEntity(202)));
        CameraSnapshotProfileService profileService = profileService();

        ResolveCameraSnapshotSourceUseCase useCase = new ResolveCameraSnapshotSourceUseCase(
                imageRepository.proxy(),
                profileService,
                new SnapshotTimezoneResolver(),
                Clock.fixed(Instant.parse("2026-04-15T16:00:00Z"), ZoneOffset.UTC)
        );

        CameraSnapshotSourceDecision decision = useCase.resolve(project(), camera(), LocalDate.of(2026, 4, 15));

        assertTrue(decision.frozen());
        assertTrue(decision.sourceImages().getFirst().getId().equals(202));
        assertTrue(imageRepository.representativeCalled);
        assertFalse(imageRepository.latestCalled);
    }

    private static Project project() {
        return Project.restore(1, "Danubius", "Site", "danubius", "Europe/Bratislava",
                OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    }

    private static Camera camera() {
        return Camera.restore(7, 1, "Outside", null, true, "/cam", "cam-outside", 1920, 1080,
                OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    }

    private static ImageEntity imageEntity(int id) {
        return ImageEntity.builder()
                .id(id)
                .bucket("bucket")
                .key("source." + ImageFormat.JPEG.getCanonicalExtension())
                .capturedAt(OffsetDateTime.of(2026, 4, 15, 11, 0, 0, 0, ZoneOffset.UTC))
                .build();
    }

    private static CameraSnapshotProfileService profileService() {
        return new CameraSnapshotProfileService(null, null, null) {
            @Override
            public CameraSnapshotProfile getOrCreate(Integer cameraId) {
                return new CameraSnapshotProfile(cameraId, 1920, 75, ImageFormat.WEBP, LocalTime.of(17, 0));
            }
        };
    }

    private static final class RecordingImageRepository {

        private final List<ImageEntity> latest;
        private final List<ImageEntity> representative;
        private boolean latestCalled;
        private boolean representativeCalled;

        private RecordingImageRepository(List<ImageEntity> latest, List<ImageEntity> representative) {
            this.latest = latest;
            this.representative = representative;
        }

        private ImageRepository proxy() {
            return (ImageRepository) Proxy.newProxyInstance(
                    ImageRepository.class.getClassLoader(),
                    new Class<?>[] {ImageRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findLatestSnapshotCandidatesByCameraId" -> {
                            latestCalled = true;
                            yield latest;
                        }
                        case "findRepresentativeSnapshotCandidatesByCameraId" -> {
                            representativeCalled = true;
                            yield representative;
                        }
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
