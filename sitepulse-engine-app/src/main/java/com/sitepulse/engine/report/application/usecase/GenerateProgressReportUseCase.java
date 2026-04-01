package com.sitepulse.engine.report.application.usecase;

import com.sitepulse.engine.common.application.event.DomainEventPublisher;
import com.sitepulse.engine.common.exception.ProcessingException;
import com.sitepulse.engine.common.exception.ValidationException;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.report.application.ReportResultMapper;
import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.domain.event.ProgressReportGeneratedEvent;
import com.sitepulse.engine.report.domain.model.ProgressReport;
import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import com.sitepulse.engine.report.domain.port.ReportContextProvider;
import com.sitepulse.engine.report.domain.port.ReportEvidenceImageProvider;
import com.sitepulse.engine.report.domain.port.ReportGenerator;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenerateProgressReportUseCase {

    private final ProjectLookupService projectLookupService;
    private final ReportEvidenceImageProvider reportEvidenceImageProvider;
    private final ReportContextProvider reportContextProvider;
    private final ReportGenerator reportGenerator;
    private final ProgressReportCatalogRepository progressReportCatalogRepository;
    private final ReportResultMapper reportResultMapper;
    private final DomainEventPublisher domainEventPublisher;

    public ProgressReportResult generate(Integer projectId, LocalDate dateFrom, LocalDate dateTo) {
        projectLookupService.requireProject(projectId);
        if (dateFrom.isAfter(dateTo)) {
            throw new ValidationException("dateFrom must be <= dateTo");
        }
        List<ReportImageEvidence> imageData = reportEvidenceImageProvider.gather(projectId, dateFrom, dateTo, 8);
        if (imageData.isEmpty()) {
            throw new ProcessingException("No images found in the given date range. Run a sync first.");
        }
        int days = (int) ChronoUnit.DAYS.between(dateFrom, dateTo) + 1;
        String metricsContext = reportContextProvider.getMetricsSummary(projectId, days);
        String milestonesContext = reportContextProvider.getMilestoneSummary(projectId);
        String content = reportGenerator.generate(imageData, metricsContext, milestonesContext);
        String summary = content.isBlank() ? "" : content.substring(0, Math.min(300, content.length())).split("\n")[0];
        ProgressReport report = ProgressReport.create(
                projectId,
                "custom",
                content,
                summary,
                dateFrom,
                dateTo,
                imageData.size(),
                "gpt-4o",
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        ProgressReport savedReport = progressReportCatalogRepository.save(report);
        domainEventPublisher.publish(new ProgressReportGeneratedEvent(
                savedReport.getId(),
                savedReport.getProjectId(),
                savedReport.getReportType(),
                savedReport.getDateRangeStart(),
                savedReport.getDateRangeEnd()
        ));
        return reportResultMapper.toResult(savedReport);
    }
}
