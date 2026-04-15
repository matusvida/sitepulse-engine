package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.project.application.result.ProjectSnapshotMetadataResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProjectSnapshotsQuery {

    private final com.sitepulse.engine.project.application.ProjectSnapshotService projectSnapshotService;

    public List<ProjectSnapshotMetadataResult> list(Integer projectId) {
        return projectSnapshotService.list(projectId);
    }
}
