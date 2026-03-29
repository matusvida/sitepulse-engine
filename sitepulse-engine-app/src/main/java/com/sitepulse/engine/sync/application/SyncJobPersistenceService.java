package com.sitepulse.engine.sync.application;

import com.sitepulse.engine.sync.domain.SyncJobEntity;
import com.sitepulse.engine.sync.persistence.SyncJobRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SyncJobPersistenceService {

    private final SyncJobRepository syncJobRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncJobEntity createRunningJob(Integer projectId) {
        return syncJobRepository.save(SyncJobEntity.builder()
                .projectId(projectId)
                .status("RUNNING")
                .imagesFound(0)
                .imagesSynced(0)
                .startedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finishJob(Integer jobId, String status, int imagesFound, int imagesSynced, List<String> errors) {
        SyncJobEntity job = syncJobRepository.findById(jobId).orElseThrow();
        job.setStatus(status);
        job.setImagesFound(imagesFound);
        job.setImagesSynced(imagesSynced);
        job.setError(errors.isEmpty() ? null : String.join("; ", errors));
        job.setFinishedAt(OffsetDateTime.now(ZoneOffset.UTC));
        syncJobRepository.save(job);
    }
}
