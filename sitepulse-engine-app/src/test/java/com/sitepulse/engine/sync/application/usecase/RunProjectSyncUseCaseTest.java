package com.sitepulse.engine.sync.application.usecase;

import com.sitepulse.engine.common.application.event.DomainEventPublisher;
import com.sitepulse.engine.common.domain.event.DomainEvent;
import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.CameraCatalogRepository;
import com.sitepulse.engine.snapshot.application.usecase.RefreshCameraDailySnapshotsUseCase;
import com.sitepulse.engine.sync.domain.model.ImageImport;
import com.sitepulse.engine.sync.domain.model.SourceImageFile;
import com.sitepulse.engine.sync.domain.model.SyncJob;
import com.sitepulse.engine.sync.domain.model.SyncJobStatus;
import com.sitepulse.engine.sync.domain.port.ImageCatalogRepository;
import com.sitepulse.engine.sync.domain.port.SyncJobRepository;
import com.sitepulse.engine.sync.domain.port.SyncSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunProjectSyncUseCaseTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 4, 11, 12, 0, 0, 0, ZoneOffset.UTC);

    @Test
    void runParsesCapturedAtUsingProjectTimezone() {
        Fixtures fixtures = new Fixtures("Europe/Bratislava", true);

        fixtures.useCase.run(fixtures.project);

        assertEquals(2, fixtures.syncJobRepository.savedJobs.size());
        SyncJob savedJob = fixtures.syncJobRepository.savedJobs.getLast();
        assertEquals(SyncJobStatus.DONE, savedJob.getStatus());
        assertEquals(1, savedJob.getImagesFound());
        assertEquals(1, savedJob.getImagesSynced());
        assertTrue(savedJob.getErrors().isEmpty());
        assertEquals(1, fixtures.objectStorage.uploadCalls.size());
        assertEquals(1, fixtures.syncSource.downloadCalls.size());
        assertEquals(1, fixtures.imageCatalogRepository.imports.size());
        assertEquals(
                OffsetDateTime.of(2026, 4, 10, 15, 40, 4, 0, ZoneOffset.ofHours(2)),
                fixtures.imageCatalogRepository.imports.getFirst().capturedAt()
        );
    }

    @Test
    void runFallsBackToDefaultTimezoneWhenProjectTimezoneIsMissing() {
        Fixtures fixtures = new Fixtures(null, true);

        fixtures.useCase.run(fixtures.project);

        assertEquals(1, fixtures.imageCatalogRepository.imports.size());
        assertEquals(
                OffsetDateTime.of(2026, 4, 10, 15, 40, 4, 0, ZoneOffset.ofHours(2)),
                fixtures.imageCatalogRepository.imports.getFirst().capturedAt()
        );
    }

    private static final class Fixtures {

        private final Project project;
        private final Camera camera = Camera.restore(11, 7, "Cam 1", null, true, "/dropbox/cam1", "cam1", null, null, NOW);
        private final RecordingSyncSource syncSource = new RecordingSyncSource();
        private final RecordingObjectStorage objectStorage = new RecordingObjectStorage();
        private final RecordingSyncJobRepository syncJobRepository = new RecordingSyncJobRepository();
        private final RecordingImageCatalogRepository imageCatalogRepository;
        private final CameraCatalogRepository cameraCatalogRepository = new CameraCatalogRepository() {
            @Override
            public List<Camera> findByProjectId(Integer projectId) {
                return List.of(camera);
            }

            @Override
            public Optional<Camera> findByIdAndProjectId(Integer cameraId, Integer projectId) {
                return Optional.empty();
            }

            @Override
            public Camera save(Camera camera) {
                return camera;
            }
        };
        private final DomainEventPublisher domainEventPublisher = new DomainEventPublisher() {
            @Override
            public void publish(DomainEvent event) {
            }
        };
        private final RefreshCameraDailySnapshotsUseCase refreshCameraDailySnapshotsUseCase = new RefreshCameraDailySnapshotsUseCase(null, null, null) {
            @Override
            public void refresh(Project project, Camera camera) {
            }
        };
        private final RunProjectSyncUseCase useCase;

        private Fixtures(String timezone, boolean saveResult) {
            this.project = Project.restore(7, "Danubius", null, "danubius", timezone, NOW);
            this.imageCatalogRepository = new RecordingImageCatalogRepository(saveResult);
            this.useCase = new RunProjectSyncUseCase(
                    syncSource,
                    objectStorage,
                    syncJobRepository,
                    imageCatalogRepository,
                    cameraCatalogRepository,
                    domainEventPublisher,
                    refreshCameraDailySnapshotsUseCase
            );
        }
    }

    private static final class RecordingSyncSource implements SyncSource {

        private final List<String> downloadCalls = new ArrayList<>();

        @Override
        public List<String> listSubfolders(String sourcePath) {
            return List.of("2026-04-10");
        }

        @Override
        public List<SourceImageFile> listFiles(String sourcePath, String subfolderName) {
            return List.of(new SourceImageFile("cam1_2026-04-10_15_40_04.jpg", "cam1_2026-04-10_15_40_04.jpg", 10));
        }

        @Override
        public byte[] downloadFile(String sourcePath, String relativePath) {
            downloadCalls.add(sourcePath + "|" + relativePath);
            return new byte[] {1, 2, 3};
        }
    }

    private static final class RecordingObjectStorage implements ObjectStorage {

        private final List<String> uploadCalls = new ArrayList<>();

        @Override
        public byte[] download(String bucket, String key) {
            return new byte[0];
        }

        @Override
        public boolean exists(String bucket, String key) {
            return false;
        }

        @Override
        public void upload(String bucket, String key, byte[] data, String contentType) {
            uploadCalls.add(bucket + "|" + key + "|" + contentType);
        }

        @Override
        public String defaultBucket() {
            return "default-bucket";
        }

        @Override
        public String presign(String bucket, String key, java.time.Duration expiresAfter) {
            return "";
        }
    }

    private static final class RecordingSyncJobRepository implements SyncJobRepository {

        private final List<SyncJob> savedJobs = new ArrayList<>();
        private int nextId = 1;

        @Override
        public SyncJob save(SyncJob syncJob) {
            SyncJob persisted = syncJob.getId() == null ? syncJob.persisted(nextId++) : syncJob;
            savedJobs.add(persisted);
            return persisted;
        }

        @Override
        public Optional<SyncJob> findLatestForProject(Integer projectId) {
            return Optional.empty();
        }
    }

    private static final class RecordingImageCatalogRepository implements ImageCatalogRepository {

        private final boolean saveResult;
        private final List<ImageImport> imports = new ArrayList<>();

        private RecordingImageCatalogRepository(boolean saveResult) {
            this.saveResult = saveResult;
        }

        @Override
        public boolean exists(String bucket, String key) {
            return false;
        }

        @Override
        public boolean saveImportedImage(ImageImport imageImport) {
            imports.add(imageImport);
            return saveResult;
        }

        @Override
        public Integer resolveCameraId(Integer projectId, String key) {
            return 11;
        }
    }
}
