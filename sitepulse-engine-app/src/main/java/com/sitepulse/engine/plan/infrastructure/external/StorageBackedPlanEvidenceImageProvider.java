package com.sitepulse.engine.plan.infrastructure.external;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.detection.persistence.ImageRepository;
import com.sitepulse.engine.plan.domain.port.PlanEvidenceImageProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageBackedPlanEvidenceImageProvider implements PlanEvidenceImageProvider {

    private final ImageRepository imageRepository;
    private final ObjectStorage objectStorage;

    @Override
    public List<byte[]> recentProjectImages(Integer projectId, int limit) {
        return imageRepository.findProcessedByProject(projectId).stream()
                .limit(limit)
                .map(image -> objectStorage.download(image.getBucket(), image.getKey()))
                .toList();
    }
}
