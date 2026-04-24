package com.sitepulse.engine.report.web;

import com.sitepulse.engine.http.report.api.ReportApi;
import com.sitepulse.engine.http.report.dto.GenerateDailyReportRequest;
import com.sitepulse.engine.http.report.dto.GenerateReportRequest;
import com.sitepulse.engine.http.report.dto.GenerateWeeklyReportRequest;
import com.sitepulse.engine.http.report.dto.ReportDetailView;
import com.sitepulse.engine.http.report.dto.ReportEvidenceImageView;
import com.sitepulse.engine.http.report.dto.ReportSummaryView;
import com.sitepulse.engine.report.domain.enums.ReportLanguage;
import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.application.result.ReportEvidenceImageResult;
import com.sitepulse.engine.report.application.usecase.GenerateProgressReportUseCase;
import com.sitepulse.engine.report.application.usecase.GetProjectReportQuery;
import com.sitepulse.engine.report.application.usecase.ListProjectReportsQuery;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportController implements ReportApi {

    private final GenerateProgressReportUseCase generateProgressReportUseCase;
    private final ListProjectReportsQuery listProjectReportsQuery;
    private final GetProjectReportQuery getProjectReportQuery;

    @Override
    public ReportDetailView generate(Integer projectId, GenerateReportRequest request) {
        return toReportView(generateProgressReportUseCase.generate(
                projectId,
                LocalDate.parse(request.getDateFrom()),
                LocalDate.parse(request.getDateTo()),
                ReportLanguage.fromRequest(request.getLanguage())
        ));
    }

    @Override
    public ReportDetailView generateDaily(Integer projectId, GenerateDailyReportRequest request) {
        return toReportView(generateProgressReportUseCase.generateAutomaticDailyOnDemand(
                projectId,
                LocalDate.parse(request.getDate()),
                ReportLanguage.fromRequest(request.getLanguage())
        ));
    }

    @Override
    public ReportDetailView generateWeekly(Integer projectId, GenerateWeeklyReportRequest request) {
        return toReportView(generateProgressReportUseCase.generateAutomaticWeeklyOnDemand(
                projectId,
                LocalDate.parse(request.getDate()),
                ReportLanguage.fromRequest(request.getLanguage())
        ));
    }

    @Override
    public List<ReportSummaryView> list(
            Integer projectId,
            int limit,
            int offset
    ) {
        return listProjectReportsQuery.list(projectId, limit, offset).stream()
                .map(this::toReportSummaryView)
                .toList();
    }

    @Override
    public ReportDetailView detail(Integer projectId, Integer reportId) {
        return toReportView(getProjectReportQuery.get(projectId, reportId));
    }

    private ReportSummaryView toReportSummaryView(ProgressReportResult result) {
        return new ReportSummaryView(
                result.getId(),
                result.getReportType(),
                result.getGenerationOrigin(),
                result.getConfidenceLevel(),
                result.getLanguage(),
                result.getPeriodLabel(),
                result.getHeadline(),
                result.getSummary(),
                result.getDateRangeStart() == null ? null : result.getDateRangeStart().toString(),
                result.getDateRangeEnd() == null ? null : result.getDateRangeEnd().toString(),
                result.getImageCount(),
                result.getEvidenceImageCount(),
                result.getModelUsed(),
                result.getCreatedAt() == null ? null : result.getCreatedAt().toString()
        );
    }

    private ReportDetailView toReportView(ProgressReportResult result) {
        return new ReportDetailView(
                result.getId(),
                result.getReportType(),
                result.getGenerationOrigin(),
                result.getConfidenceLevel(),
                result.getLanguage(),
                result.getPeriodLabel(),
                result.getHeadline(),
                result.getSummary(),
                result.getDateRangeStart() == null ? null : result.getDateRangeStart().toString(),
                result.getDateRangeEnd() == null ? null : result.getDateRangeEnd().toString(),
                result.getImageCount(),
                result.getEvidenceImageCount(),
                result.getModelUsed(),
                result.getCreatedAt() == null ? null : result.getCreatedAt().toString(),
                String.valueOf(result.getProjectId()),
                result.getContentMd(),
                result.getEvidenceImages() == null ? List.of() : result.getEvidenceImages().stream()
                        .map(this::toEvidenceImageView)
                        .toList()
        );
    }

    private ReportEvidenceImageView toEvidenceImageView(ReportEvidenceImageResult result) {
        return new ReportEvidenceImageView(
                result.capturedAt(),
                result.date(),
                result.url(),
                result.key()
        );
    }
}
