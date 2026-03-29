package com.sitepulse.engine.report.web;

import com.sitepulse.engine.http.report.api.ReportApi;
import com.sitepulse.engine.http.report.dto.GenerateReportRequest;
import com.sitepulse.engine.report.application.ReportService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReportController implements ReportApi {

    private final ReportService reportService;

    @Override
    public Map<String, Object> generate(Integer projectId, GenerateReportRequest request) {
        return reportService.generateReport(projectId, LocalDate.parse(request.getDateFrom()), LocalDate.parse(request.getDateTo()));
    }

    @Override
    public List<Map<String, Object>> list(
            Integer projectId,
            int limit,
            int offset
    ) {
        return reportService.listReports(projectId, limit, offset);
    }

    @Override
    public Map<String, Object> detail(Integer projectId, Integer reportId) {
        return reportService.getReport(projectId, reportId);
    }
}
