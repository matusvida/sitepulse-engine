package com.sitepulse.engine.plan.infrastructure.external;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.plan.domain.port.PlanEvidenceImageProvider;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageBackedPlanEvidenceImageProvider implements PlanEvidenceImageProvider {

    private final ProcessedImageReadModel processedImageReadModel;
    private final ObjectStorage objectStorage;

    @Override
    public List<byte[]> recentProjectImages(Integer projectId, int limit) {
        return processedImageReadModel.findProcessedByProject(projectId).stream()
                .limit(limit)
                .map(image -> objectStorage.download(image.getBucket(), image.getKey()))
                .toList();
    }
}
