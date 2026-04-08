package com.sitepulse.engine.project.application.usecase;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.project.application.result.ProjectSnapshotMetadataResult;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListProjectSnapshotsQuery {

    private final ProcessedImageReadModel processedImageReadModel;
    private final ObjectStorage objectStorage;
    private final SitePulseProperties properties;

    public List<ProjectSnapshotMetadataResult> list(Integer projectId) {
        var ttl = properties.storagePresignTtl();
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plus(ttl);
        return processedImageReadModel.findRepresentativeSnapshots(projectId).stream()
                .map(image -> toResult(image, ttl, expiresAt))
                .toList();
    }

    private ProjectSnapshotMetadataResult toResult(StoredImage image, java.time.Duration ttl, OffsetDateTime expiresAt) {
        String url = objectStorage.presign(image.getBucket(), image.getKey(), ttl);
        return new ProjectSnapshotMetadataResult(image.getCapturedAt().toLocalDate(), url, expiresAt, detectMediaType(image.getKey()));
    }

    private String detectMediaType(String key) {
        return key.toLowerCase().endsWith(".png") ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;
    }
}
