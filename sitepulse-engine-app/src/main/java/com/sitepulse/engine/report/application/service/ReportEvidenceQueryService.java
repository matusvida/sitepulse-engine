package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.report.application.result.ReportEvidenceImageResult;
import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import com.sitepulse.engine.report.domain.port.ReportEvidenceImageProvider;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportEvidenceQueryService {

    private final ReportEvidenceImageProvider reportEvidenceImageProvider;

    public List<ReportEvidenceImageResult> list(Integer projectId, LocalDate dateFrom, LocalDate dateTo, int maxImages) {
        if (projectId == null || dateFrom == null || dateTo == null || maxImages <= 0) {
            return List.of();
        }
        return reportEvidenceImageProvider.gather(projectId, dateFrom, dateTo, maxImages).stream()
                .map(this::toResult)
                .toList();
    }

    private ReportEvidenceImageResult toResult(ReportImageEvidence image) {
        return ReportEvidenceImageResult.of(
                image.capturedAt(),
                image.date(),
                image.url(),
                image.key()
        );
    }
}
