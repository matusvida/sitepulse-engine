package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.project.application.result.ProjectSnapshotResult;
import com.sitepulse.engine.project.application.result.ProjectSnapshotSelectionResult;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProjectSnapshotQuery {

    private final ResolveProjectSnapshotQuery resolveProjectSnapshotQuery;
    private final ObjectStorage objectStorage;

    public ProjectSnapshotResult get(Integer projectId, LocalDate date) {
        ProjectSnapshotSelectionResult snapshot = resolveProjectSnapshotQuery.resolve(projectId, date);
        return new ProjectSnapshotResult(
                objectStorage.download(snapshot.image().getBucket(), snapshot.image().getKey()),
                snapshot.mediaType()
        );
    }
}
