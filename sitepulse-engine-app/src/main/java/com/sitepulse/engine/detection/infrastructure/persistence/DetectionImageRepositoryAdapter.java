package com.sitepulse.engine.detection.infrastructure.persistence;

import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.port.DetectionImageRepository;
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
        return imageRepository.claimNewImages(limit).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<DetectionImage> findById(Integer imageId) {
        return imageRepository.findById(imageId).map(this::toDomain);
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
                entity.getUpdatedAt()
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
                .build();
    }
}
