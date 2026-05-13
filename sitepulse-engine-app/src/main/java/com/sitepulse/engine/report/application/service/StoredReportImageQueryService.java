package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageEntity;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageRepository;
import com.sitepulse.engine.report.application.result.ReportEvidenceImageResult;
import com.sitepulse.engine.report.infrastructure.persistence.ReportImageEntity;
import com.sitepulse.engine.report.infrastructure.persistence.ReportImageRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StoredReportImageQueryService {

    private final ReportImageRepository reportImageRepository;
    private final ImageRepository imageRepository;
    private final ObjectStorage objectStorage;
    private final SitePulseProperties properties;

    public List<ReportEvidenceImageResult> list(Integer reportId) {
        if (reportId == null) {
            return List.of();
        }
        List<ReportImageEntity> rows = reportImageRepository.findByReportIdOrderByIdAsc(reportId);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Integer, ImageEntity> imagesById = new LinkedHashMap<>();
        imageRepository.findAllById(rows.stream().map(ReportImageEntity::getImageId).distinct().toList())
                .forEach(image -> imagesById.put(image.getId(), image));
        return rows.stream()
                .map(row -> toResult(row, imagesById.get(row.getImageId())))
                .toList();
    }

    private ReportEvidenceImageResult toResult(ReportImageEntity row, ImageEntity image) {
        String bucket = image == null || image.getBucket() == null || image.getBucket().isBlank()
                ? objectStorage.defaultBucket()
                : image.getBucket();
        String date = image == null || image.getCapturedAt() == null
                ? null
                : image.getCapturedAt().toLocalDate().toString();
        return ReportEvidenceImageResult.of(
                image == null ? null : image.getCapturedAt(),
                date,
                objectStorage.presign(bucket, row.getImagePath(), properties.storagePresignTtl()),
                row.getImagePath()
        );
    }
}
