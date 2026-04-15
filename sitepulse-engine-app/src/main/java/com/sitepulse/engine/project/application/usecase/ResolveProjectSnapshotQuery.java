package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.project.application.ProjectSnapshotService;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.application.result.ProjectSnapshotSelectionResult;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResolveProjectSnapshotQuery {

    private final ProjectLookupService projectLookupService;
    private final ProjectSnapshotService projectSnapshotService;

    public ProjectSnapshotSelectionResult resolve(Integer projectId, LocalDate date) {
        projectLookupService.requireProject(projectId);
        try {
            return projectSnapshotService.resolve(projectId, date);
        } catch (ResourceNotFoundException ex) {
            throw ex;
        }
    }
}
