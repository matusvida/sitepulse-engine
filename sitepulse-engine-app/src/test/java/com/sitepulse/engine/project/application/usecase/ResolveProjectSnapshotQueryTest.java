package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.application.ProjectSnapshotService;
import com.sitepulse.engine.project.application.result.ProjectSnapshotSelectionResult;
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
        ResolveProjectSnapshotQuery query = new ResolveProjectSnapshotQuery(
                new ProjectLookupService(projectCatalogRepository(true)),
                new ProjectSnapshotServiceStub(new ProjectSnapshotSelectionResult(
                        LocalDate.of(2024, 6, 15),
                        "bucket",
                        "snapshot." + ImageFormat.PNG.getCanonicalExtension(),
                        ImageFormat.PNG.getMediaType()
                ))
        );

        ProjectSnapshotSelectionResult result = query.resolve(1, LocalDate.of(2024, 6, 15));

        assertEquals(LocalDate.of(2024, 6, 15), result.date());
        assertEquals(ImageFormat.PNG.getMediaType(), result.mediaType());
        assertEquals("snapshot." + ImageFormat.PNG.getCanonicalExtension(), result.key());
    }

    @Test
    void resolveThrowsWhenNoImageExistsForDate() {
        ResolveProjectSnapshotQuery query = new ResolveProjectSnapshotQuery(
                new ProjectLookupService(projectCatalogRepository(true)),
                new MissingProjectSnapshotServiceStub()
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

    private static final class ProjectSnapshotServiceStub extends ProjectSnapshotService {

        private final ProjectSnapshotSelectionResult result;

        private ProjectSnapshotServiceStub(ProjectSnapshotSelectionResult result) {
            super(null, null, null, null, null, null, null, null, null);
            this.result = result;
        }

        @Override
        public ProjectSnapshotSelectionResult resolve(Integer projectId, LocalDate date) {
            return result;
        }
    }

    private static final class MissingProjectSnapshotServiceStub extends ProjectSnapshotService {

        private MissingProjectSnapshotServiceStub() {
            super(null, null, null, null, null, null, null, null, null);
        }

        @Override
        public ProjectSnapshotSelectionResult resolve(Integer projectId, LocalDate date) {
            throw new ResourceNotFoundException("No image found");
        }
    }
}
