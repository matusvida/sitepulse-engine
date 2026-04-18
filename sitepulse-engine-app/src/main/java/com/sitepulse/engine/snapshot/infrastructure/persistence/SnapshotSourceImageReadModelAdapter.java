package com.sitepulse.engine.snapshot.infrastructure.persistence;

import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageRepository;
import com.sitepulse.engine.snapshot.domain.port.SnapshotSourceImageReadModel;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SnapshotSourceImageReadModelAdapter implements SnapshotSourceImageReadModel {

    private final ImageRepository imageRepository;

    @Override
    public List<StoredImage> findRepresentativeSnapshotCandidatesByCameraId(
            Integer cameraId,
            OffsetDateTime dayStart,
            OffsetDateTime dayEnd,
            OffsetDateTime midday,
            int limit
    ) {
        return imageRepository.findRepresentativeSnapshotCandidatesByCameraId(cameraId, dayStart, dayEnd, midday, limit).stream()
                .map(this::toStoredImage)
                .toList();
    }

    @Override
    public List<StoredImage> findLatestSnapshotCandidatesByCameraId(
            Integer cameraId,
            OffsetDateTime dayStart,
            OffsetDateTime dayEnd,
            int limit
    ) {
        return imageRepository.findLatestSnapshotCandidatesByCameraId(cameraId, dayStart, dayEnd, limit).stream()
                .map(this::toStoredImage)
                .toList();
    }

    @Override
    public List<LocalDate> findAvailableSnapshotDatesByCameraId(Integer cameraId, String timezone) {
        return imageRepository.findAvailableSnapshotDatesByCameraId(cameraId, timezone).stream()
                .map(java.sql.Date::toLocalDate)
                .toList();
    }

    private StoredImage toStoredImage(ImageEntity imageEntity) {
        return new StoredImage(
                imageEntity.getId(),
                imageEntity.getBucket(),
                imageEntity.getKey(),
                imageEntity.getCapturedAt(),
                imageEntity.getWeatherNote(),
                imageEntity.getEvidenceActivityScore(),
                imageEntity.getEvidenceChangeScore(),
                imageEntity.getEvidenceQualityScore(),
                imageEntity.getEvidenceOverallScore(),
                imageEntity.getEvidenceSummary()
        );
    }
}
