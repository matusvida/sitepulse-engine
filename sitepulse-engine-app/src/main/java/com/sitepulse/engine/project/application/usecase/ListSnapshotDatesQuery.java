package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.project.application.ProjectLookupService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListSnapshotDatesQuery {

    private final ProjectLookupService projectLookupService;
    private final ProcessedImageReadModel processedImageReadModel;

    public List<LocalDate> list(Integer projectId) {
        projectLookupService.requireProject(projectId);
        return processedImageReadModel.findSnapshotCapturedAtValues(projectId).stream()
                .map(instant -> instant.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate())
                .distinct()
                .toList();
    }
}
