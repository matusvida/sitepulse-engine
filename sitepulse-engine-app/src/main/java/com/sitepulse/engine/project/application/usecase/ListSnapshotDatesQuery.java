package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.project.application.ProjectSnapshotService;
import com.sitepulse.engine.project.application.ProjectLookupService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListSnapshotDatesQuery {

    private final ProjectLookupService projectLookupService;
    private final ProjectSnapshotService projectSnapshotService;

    public List<LocalDate> list(Integer projectId) {
        projectLookupService.requireProject(projectId);
        return projectSnapshotService.listAvailableDates(projectId);
    }
}
