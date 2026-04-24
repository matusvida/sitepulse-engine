package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.common.application.event.DomainEventPublisher;
import com.sitepulse.engine.common.exception.ProcessingException;
import com.sitepulse.engine.report.domain.event.ProgressReportGeneratedEvent;
import com.sitepulse.engine.report.domain.enums.ConfidenceLevel;
import com.sitepulse.engine.report.domain.enums.GenerationOrigin;
import com.sitepulse.engine.report.domain.enums.ReportLanguage;
import com.sitepulse.engine.report.domain.model.ProgressReport;
import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import com.sitepulse.engine.report.domain.enums.ReportType;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import com.sitepulse.engine.report.domain.port.ReportEvidenceImageProvider;
import com.sitepulse.engine.report.domain.port.ReportGenerator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportCompositionService {

    private static final String REPORT_MODEL = "gpt-4o";

    private final ReportEvidenceImageProvider reportEvidenceImageProvider;
    private final ReportGenerator reportGenerator;
    private final ProgressReportCatalogRepository progressReportCatalogRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final Clock clock;

    public ProgressReport composeAndSave(
            Integer projectId,
            ReportType reportType,
            GenerationOrigin generationOrigin,
            LocalDate dateFrom,
            LocalDate dateTo,
            String periodKey,
            ConfidenceLevel confidenceLevel,
            ReportLanguage language,
            String primaryContext,
            String milestonesContext,
            int maxImages
    ) {
        List<ReportImageEvidence> imageData = reportEvidenceImageProvider.gather(projectId, dateFrom, dateTo, maxImages);
        if (imageData.isEmpty()) {
            throw new ProcessingException("No images found in the given date range. Run a sync first.");
        }

        String content = reportGenerator.generate(imageData, primaryContext, milestonesContext, language);
        String summary = extractSummary(content);
        ProgressReport report = ProgressReport.create(
                projectId,
                reportType.toPersistenceValue(),
                generationOrigin.toPersistenceValue(),
                periodKey,
                resolveConfidenceLevel(confidenceLevel, imageData.size()).toPersistenceValue(),
                language.toPersistenceValue(),
                content,
                summary,
                summary,
                dateFrom,
                dateTo,
                imageData.size(),
                imageData.size(),
                REPORT_MODEL,
                OffsetDateTime.now(clock)
        );
        ProgressReport savedReport = progressReportCatalogRepository.save(report);
        publish(savedReport);
        return savedReport;
    }

    private ConfidenceLevel resolveConfidenceLevel(ConfidenceLevel requestedConfidenceLevel, int evidenceCount) {
        if (requestedConfidenceLevel != null) {
            return requestedConfidenceLevel;
        }
        if (evidenceCount >= 6) {
            return ConfidenceLevel.HIGH;
        }
        if (evidenceCount >= 3) {
            return ConfidenceLevel.MEDIUM;
        }
        return ConfidenceLevel.LOW;
    }

    private String extractSummary(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return content.substring(0, Math.min(300, content.length())).split("\n")[0];
    }

    private void publish(ProgressReport report) {
        domainEventPublisher.publish(new ProgressReportGeneratedEvent(
                report.getId(),
                report.getProjectId(),
                report.getReportType(),
                report.getDateRangeStart(),
                report.getDateRangeEnd()
        ));
    }
}
