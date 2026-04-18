package com.sitepulse.engine.detection.infrastructure.persistence;

import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.enums.ImageStatus;
import com.sitepulse.engine.detection.domain.port.DetectionImageRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class DetectionImageRepositoryAdapter implements DetectionImageRepository {

    private final ImageRepository imageRepository;

    @Override
    @Transactional
    public List<DetectionImage> claimPendingImages(int limit) {
        return imageRepository.claimNewImages(limit).stream()
                .map(this::toDomain)
                .sorted(Comparator
                        .comparing(DetectionImage::getCapturedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(DetectionImage::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    public Optional<DetectionImage> findById(Integer imageId) {
        return imageRepository.findById(imageId).map(this::toDomain);
    }

    @Override
    public Optional<DetectionImage> findPreviousDone(DetectionImage image) {
        Integer projectId = image.getProjectId();
        if (projectId == null) {
            return Optional.empty();
        }
        Integer cameraId = image.getCameraId();
        OffsetDateTime capturedAt = image.getCapturedAt();
        Integer imageId = image.getId();
        if (cameraId != null) {
            if (capturedAt != null) {
                return imageRepository.findPreviousByCameraCapturedAt(projectId, cameraId, ImageStatus.DONE.name(), capturedAt).map(this::toDomain);
            }
            if (imageId != null) {
                return imageRepository.findPreviousByCameraId(projectId, cameraId, ImageStatus.DONE.name(), imageId).map(this::toDomain);
            }
        }
        if (capturedAt != null) {
            return imageRepository.findPreviousByProjectCapturedAt(projectId, ImageStatus.DONE.name(), capturedAt).map(this::toDomain);
        }
        if (imageId != null) {
            return imageRepository.findPreviousByProjectId(projectId, ImageStatus.DONE.name(), imageId).map(this::toDomain);
        }
        return Optional.empty();
    }

    @Override
    public boolean existsByBucketAndKey(String bucket, String key) {
        return imageRepository.existsByBucketAndKey(bucket, key);
    }

    @Override
    @Transactional
    public DetectionImage save(DetectionImage image) {
        return toDomain(imageRepository.save(toEntity(image)));
    }

    private DetectionImage toDomain(ImageEntity entity) {
        return DetectionImage.restore(
                entity.getId(),
                entity.getBucket(),
                entity.getKey(),
                entity.getStatus(),
                entity.getProjectId(),
                entity.getCameraId(),
                entity.getCapturedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getWeatherNote(),
                entity.getEvidenceActivityScore(),
                entity.getEvidenceChangeScore(),
                entity.getEvidenceQualityScore(),
                entity.getEvidenceOverallScore(),
                entity.getEvidenceSummary()
        );
    }

    private ImageEntity toEntity(DetectionImage image) {
        return ImageEntity.builder()
                .id(image.getId())
                .bucket(image.getBucket())
                .key(image.getKey())
                .status(image.getStatus())
                .projectId(image.getProjectId())
                .cameraId(image.getCameraId())
                .capturedAt(image.getCapturedAt())
                .createdAt(image.getCreatedAt())
                .updatedAt(image.getUpdatedAt())
                .weatherNote(image.getWeatherNote())
                .evidenceActivityScore(image.getEvidenceActivityScore())
                .evidenceChangeScore(image.getEvidenceChangeScore())
                .evidenceQualityScore(image.getEvidenceQualityScore())
                .evidenceOverallScore(image.getEvidenceOverallScore())
                .evidenceSummary(image.getEvidenceSummary())
                .build();
    }
}
