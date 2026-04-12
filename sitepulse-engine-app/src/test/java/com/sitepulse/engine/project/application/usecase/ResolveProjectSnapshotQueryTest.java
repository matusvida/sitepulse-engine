package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResolveProjectSnapshotQueryTest {

    @Test
    void resolveReturnsSelectedImageAndMediaType() {
        ProjectCatalogRepository projectCatalogRepository = projectCatalogRepository(true);
        ProcessedImageReadModel processedImageReadModel = new ProcessedImageReadModel() {
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
                return Optional.of(new StoredImage(
                        10,
                        "bucket",
                        "snapshot.png",
                        OffsetDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneOffset.UTC)
                ));
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
            public List<com.sitepulse.engine.detection.domain.model.DetectedObject> findDetections(Integer imageId) {
                return List.of();
            }
        };

        ResolveProjectSnapshotQuery query = new ResolveProjectSnapshotQuery(
                new com.sitepulse.engine.project.application.ProjectLookupService(projectCatalogRepository),
                processedImageReadModel
        );

        var result = query.resolve(1, LocalDate.of(2024, 6, 15));

        assertEquals(LocalDate.of(2024, 6, 15), result.date());
        assertEquals("image/png", result.mediaType());
        assertEquals("snapshot.png", result.image().getKey());
    }

    @Test
    void resolveThrowsWhenNoImageExistsForDate() {
        ProjectCatalogRepository projectCatalogRepository = projectCatalogRepository(true);
        ProcessedImageReadModel processedImageReadModel = new ProcessedImageReadModel() {
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
            public List<StoredImage> findDoneInRange(Integer projectId, OffsetDateTime from, OffsetDateTime to) {
                return List.of();
            }

            @Override
            public List<StoredImage> findProcessedByProject(Integer projectId) {
                return List.of();
            }

            @Override
            public List<com.sitepulse.engine.detection.domain.model.DetectedObject> findDetections(Integer imageId) {
                return List.of();
            }
        };

        ResolveProjectSnapshotQuery query = new ResolveProjectSnapshotQuery(
                new com.sitepulse.engine.project.application.ProjectLookupService(projectCatalogRepository),
                processedImageReadModel
        );

        assertThrows(ResourceNotFoundException.class, () -> query.resolve(1, LocalDate.of(2024, 6, 15)));
    }

    private static ProjectCatalogRepository projectCatalogRepository(boolean exists) {
        return new ProjectCatalogRepository() {
            @Override
            public List<Project> findAll() {
                return List.of();
            }

            @Override
            public Optional<Project> findById(Integer projectId) {
                return exists
                        ? Optional.of(Project.restore(
                                projectId,
                                "Project",
                                "Location",
                                "dropbox/path",
                                "Europe/Bratislava",
                                OffsetDateTime.of(2024, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC)
                        ))
                        : Optional.empty();
            }

            @Override
            public Project save(Project project) {
                throw new UnsupportedOperationException();
            }
        };
    }
}
