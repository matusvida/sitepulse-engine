package com.sitepulse.engine.sync.infrastructure.persistence;

import com.sitepulse.engine.sync.domain.model.SyncJob;
import com.sitepulse.engine.sync.domain.port.SyncJobRepository;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class SyncJobRepositoryAdapter implements SyncJobRepository {

    private final SyncJobJpaRepository syncJobJpaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncJob save(SyncJob syncJob) {
        SyncJobJpaEntity entity = syncJob.getId() == null
                ? new SyncJobJpaEntity()
                : syncJobJpaRepository.findById(syncJob.getId()).orElseGet(SyncJobJpaEntity::new);
        entity.setProjectId(syncJob.getProjectId());
        entity.setStatus(syncJob.getStatus());
        entity.setImagesFound(syncJob.getImagesFound());
        entity.setImagesSynced(syncJob.getImagesSynced());
        entity.setError(syncJob.errorSummary());
        entity.setStartedAt(syncJob.getStartedAt());
        entity.setFinishedAt(syncJob.getFinishedAt());
        return toDomain(syncJobJpaRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<SyncJob> findLatestForProject(Integer projectId) {
        return syncJobJpaRepository.findTopByProjectIdOrderByStartedAtDesc(projectId).map(this::toDomain);
    }

    private SyncJob toDomain(SyncJobJpaEntity entity) {
        List<String> errors = entity.getError() == null || entity.getError().isBlank()
                ? List.of()
                : Arrays.stream(entity.getError().split("; ")).toList();
        return SyncJob.restore(
                entity.getId(),
                entity.getProjectId(),
                entity.getStatus(),
                entity.getImagesFound() == null ? 0 : entity.getImagesFound(),
                entity.getImagesSynced() == null ? 0 : entity.getImagesSynced(),
                errors,
                entity.getStartedAt(),
                entity.getFinishedAt()
        );
    }
}
