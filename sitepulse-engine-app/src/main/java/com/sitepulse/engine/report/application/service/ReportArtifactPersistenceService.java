package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.report.domain.model.ProgressReport;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import com.sitepulse.engine.report.infrastructure.persistence.ReportImageEntity;
import com.sitepulse.engine.report.infrastructure.persistence.ReportImageRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportArtifactPersistenceService {

    private final ProgressReportCatalogRepository progressReportCatalogRepository;
    private final ReportImageRepository reportImageRepository;

    @Transactional
    public ProgressReport save(
            ProgressReport report,
            List<StoredReportImageAssetService.PreparedReportImageAsset> preparedImages
    ) {
        ProgressReport savedReport = progressReportCatalogRepository.save(report);
        if (!preparedImages.isEmpty()) {
            reportImageRepository.saveAll(preparedImages.stream()
                    .map(image -> ReportImageEntity.builder()
                            .reportId(savedReport.getId())
                            .imageId(image.imageId())
                            .imagePath(image.imagePath())
                            .build())
                    .toList());
        }
        return savedReport;
    }
}
