package com.sitepulse.engine.http.report.api;

import com.sitepulse.engine.http.report.dto.GenerateReportRequest;
import com.sitepulse.engine.http.report.dto.ReportDetailView;
import com.sitepulse.engine.http.report.dto.ReportSummaryView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Reports")
@RequestMapping("/api/projects/{projectId}/reports")
public interface ReportApi {

    @Operation(summary = "Generate a progress report")
    @PostMapping("/generate")
    ReportDetailView generate(@PathVariable Integer projectId, @Valid @RequestBody GenerateReportRequest request);

    @Operation(summary = "List generated reports")
    @GetMapping
    List<ReportSummaryView> list(
            @PathVariable Integer projectId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    );

    @Operation(summary = "Get report detail")
    @GetMapping("/{reportId}")
    ReportDetailView detail(@PathVariable Integer projectId, @PathVariable Integer reportId);
}
