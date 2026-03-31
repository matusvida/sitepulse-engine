package com.sitepulse.engine.report.web;

import com.sitepulse.engine.http.report.api.ReportApi;
import com.sitepulse.engine.http.report.dto.GenerateReportRequest;
import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.application.usecase.GenerateProgressReportUseCase;
import com.sitepulse.engine.report.application.usecase.GetProjectReportQuery;
import com.sitepulse.engine.report.application.usecase.ListProjectReportsQuery;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportController implements ReportApi {

    private final GenerateProgressReportUseCase generateProgressReportUseCase;
    private final ListProjectReportsQuery listProjectReportsQuery;
    private final GetProjectReportQuery getProjectReportQuery;

    @Override
    public Map<String, Object> generate(Integer projectId, GenerateReportRequest request) {
        return toReportView(generateProgressReportUseCase.generate(
                projectId,
                LocalDate.parse(request.getDateFrom()),
                LocalDate.parse(request.getDateTo())
        ));
    }

    @Override
    public List<Map<String, Object>> list(
            Integer projectId,
            int limit,
            int offset
    ) {
        return listProjectReportsQuery.list(projectId, limit, offset).stream()
                .map(this::toReportSummaryView)
                .toList();
    }

    @Override
    public Map<String, Object> detail(Integer projectId, Integer reportId) {
        return toReportView(getProjectReportQuery.get(projectId, reportId));
    }

    private Map<String, Object> toReportSummaryView(ProgressReportResult result) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", result.getId());
        view.put("reportType", result.getReportType());
        view.put("summary", result.getSummary());
        view.put("dateRangeStart", result.getDateRangeStart() == null ? null : result.getDateRangeStart().toString());
        view.put("dateRangeEnd", result.getDateRangeEnd() == null ? null : result.getDateRangeEnd().toString());
        view.put("imageCount", result.getImageCount());
        view.put("modelUsed", result.getModelUsed());
        view.put("createdAt", result.getCreatedAt() == null ? null : result.getCreatedAt().toString());
        return view;
    }

    private Map<String, Object> toReportView(ProgressReportResult result) {
        Map<String, Object> view = new HashMap<>(toReportSummaryView(result));
        view.put("projectId", String.valueOf(result.getProjectId()));
        view.put("contentMd", result.getContentMd());
        return view;
    }
}
