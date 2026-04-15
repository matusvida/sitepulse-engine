package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.common.domain.model.ImageFormat;
import com.sitepulse.engine.project.application.ProjectSnapshotService;
import com.sitepulse.engine.project.application.result.ProjectSnapshotMetadataResult;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListProjectSnapshotsQueryTest {

    @Test
    void listDelegatesToProjectSnapshotService() {
        ProjectSnapshotMetadataResult first = new ProjectSnapshotMetadataResult(
                LocalDate.of(2024, 6, 15),
                "http://example.test/a",
                OffsetDateTime.of(2024, 6, 15, 11, 0, 0, 0, ZoneOffset.UTC),
                ImageFormat.JPEG.getMediaType()
        );
        ProjectSnapshotMetadataResult second = new ProjectSnapshotMetadataResult(
                LocalDate.of(2024, 6, 16),
                "http://example.test/b",
                OffsetDateTime.of(2024, 6, 15, 11, 0, 0, 0, ZoneOffset.UTC),
                ImageFormat.WEBP.getMediaType()
        );

        ListProjectSnapshotsQuery query = new ListProjectSnapshotsQuery(new ProjectSnapshotServiceStub(List.of(first, second)));

        List<ProjectSnapshotMetadataResult> results = query.list(1);

        assertEquals(List.of(first, second), results);
    }

    private static final class ProjectSnapshotServiceStub extends ProjectSnapshotService {

        private final List<ProjectSnapshotMetadataResult> results;

        private ProjectSnapshotServiceStub(List<ProjectSnapshotMetadataResult> results) {
            super(null, null, null, null, null, null, null, null, null);
            this.results = results;
        }

        @Override
        public List<ProjectSnapshotMetadataResult> list(Integer projectId) {
            return results;
        }
    }
}
