package com.sitepulse.engine.sync.infrastructure.persistence;

import com.sitepulse.engine.sync.application.result.SyncStatusResult;
import com.sitepulse.engine.sync.domain.port.SyncStatusReadModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SyncStatusReadModelAdapter implements SyncStatusReadModel {

    private final SyncJobJpaRepository syncJobJpaRepository;

    @Override
    public SyncStatusResult getLatestStatus(Integer projectId) {
        return syncJobJpaRepository.findTopByProjectIdOrderByStartedAtDesc(projectId)
                .map(this::toResult)
                .orElseGet(() -> SyncStatusResult.neverRun(projectId));
    }

    private SyncStatusResult toResult(SyncJobJpaEntity entity) {
        return new SyncStatusResult(
                entity.getId(),
                entity.getProjectId(),
                entity.getStatus(),
                null,
                entity.getImagesFound(),
                entity.getImagesSynced(),
                entity.getError(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                false
        );
    }
}
