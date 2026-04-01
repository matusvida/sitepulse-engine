package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.application.result.ProjectSnapshotResult;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProjectSnapshotQuery {

    private final ProjectLookupService projectLookupService;
    private final ProcessedImageReadModel processedImageReadModel;
    private final ObjectStorage objectStorage;

    public ProjectSnapshotResult get(Integer projectId, LocalDate date) {
        projectLookupService.requireProject(projectId);
        OffsetDateTime dayStart = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime midday = date.atTime(12, 0).atOffset(ZoneOffset.UTC);
        StoredImage image = processedImageReadModel.findClosestSnapshot(projectId, dayStart, dayEnd, midday)
                .orElseThrow(() -> new ResourceNotFoundException("No image found for " + date));
        return new ProjectSnapshotResult(
                objectStorage.download(image.getBucket(), image.getKey()),
                image.getKey().toLowerCase().endsWith(".png") ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE
        );
    }
}
