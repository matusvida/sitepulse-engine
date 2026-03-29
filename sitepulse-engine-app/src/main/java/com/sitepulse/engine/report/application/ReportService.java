package com.sitepulse.engine.report.application;

import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.detection.domain.ImageEntity;
import com.sitepulse.engine.detection.persistence.ImageRepository;
import com.sitepulse.engine.integration.openai.OpenAiService;
import com.sitepulse.engine.integration.storage.StorageService;
import com.sitepulse.engine.metrics.application.AnalysisService;
import com.sitepulse.engine.plan.application.PlanService;
import com.sitepulse.engine.project.application.ProjectService;
import com.sitepulse.engine.report.domain.ProgressReportEntity;
import com.sitepulse.engine.report.persistence.ProgressReportRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final ProjectService projectService;
    private final ImageRepository imageRepository;
    private final StorageService storageService;
    private final AnalysisService analysisService;
    private final ProgressReportRepository progressReportRepository;
    private final OpenAiService openAiService;
    private final PlanService planService;

    @Transactional
    public Map<String, Object> generateReport(Integer projectId, LocalDate dateFrom, LocalDate dateTo) {
        projectService.requireProject(projectId);
        log.info("Generating report for projectId={} dateFrom={} dateTo={}", projectId, dateFrom, dateTo);
        if (dateFrom.isAfter(dateTo)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "dateFrom must be <= dateTo");
        }
        List<Map<String, Object>> imageData = gatherImages(projectId, dateFrom, dateTo, 8);
        if (imageData.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "No images found in the given date range. Run a sync first.");
        }
        String metricsContext = buildMetricsContext(projectId, dateFrom, dateTo);
        String milestonesContext = buildMilestonesContext(projectId);
        String content = openAiService.generateProgressReport(imageData, metricsContext, milestonesContext);
        String summary = content.isBlank() ? "" : content.substring(0, Math.min(300, content.length())).split("\n")[0];
        ProgressReportEntity entity = progressReportRepository.save(ProgressReportEntity.builder()
                .projectId(projectId)
                .reportType("custom")
                .contentMd(content)
                .summary(summary)
                .dateRangeStart(dateFrom)
                .dateRangeEnd(dateTo)
                .imageCount(imageData.size())
                .modelUsed("gpt-4o")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
        log.info("Report generated for projectId={} reportId={} imageCount={}", projectId, entity.getId(), imageData.size());
        return reportView(entity);
    }

    public List<Map<String, Object>> listReports(Integer projectId, int limit, int offset) {
        projectService.requireProject(projectId);
        int page = Math.max(0, offset / Math.max(1, limit));
        return progressReportRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(page, limit)).stream()
                .map(this::reportSummaryView)
                .toList();
    }

    public Map<String, Object> getReport(Integer projectId, Integer reportId) {
        projectService.requireProject(projectId);
        ProgressReportEntity entity = progressReportRepository.findByIdAndProjectId(reportId, projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Report not found"));
        return reportView(entity);
    }

    private List<Map<String, Object>> gatherImages(Integer projectId, LocalDate dateFrom, LocalDate dateTo, int maxImages) {
        List<ImageEntity> rows = imageRepository.findDoneInRange(
                projectId,
                dateFrom.atStartOfDay().atOffset(ZoneOffset.UTC),
                dateTo.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
        );
        if (rows.isEmpty()) {
            return List.of();
        }
        int step = Math.max(1, rows.size() / maxImages);
        return java.util.stream.IntStream.range(0, rows.size())
                .filter(index -> index % step == 0)
                .limit(maxImages)
                .mapToObj(rows::get)
                .map(image -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("date", image.getCapturedAt() == null ? dateFrom.toString() : image.getCapturedAt().toLocalDate().toString());
                    payload.put("b64", Base64.getEncoder().encodeToString(storageService.download(image.getBucket(), image.getKey())));
                    return payload;
                })
                .toList();
    }

    private String buildMetricsContext(Integer projectId, LocalDate dateFrom, LocalDate dateTo) {
        StringBuilder builder = new StringBuilder();
        builder.append("### Daily Metrics\n");
        analysisService.dailyMetrics(projectId, (int) java.time.temporal.ChronoUnit.DAYS.between(dateFrom, dateTo) + 1).forEach(row ->
                builder.append("- ").append(row.get("date"))
                        .append(": people=").append(row.get("peopleCount"))
                        .append(", vehicles=").append(row.get("vehicleCount"))
                        .append(", active_hours=").append(row.get("activeHours"))
                        .append('\n')
        );
        builder.append("\n### Weekly Metrics\n");
        analysisService.weeklyMetrics(projectId, 12).forEach(row ->
                builder.append("- Week of ").append(row.get("weekStart"))
                        .append(": progress_delta=").append(row.get("progressDelta"))
                        .append(", activity_index=").append(row.get("activityIndex"))
                        .append(", active_hours=").append(row.get("activeHours"))
                        .append(", risk=").append(row.get("riskLevel"))
                        .append('\n')
        );
        return builder.toString();
    }

    private String buildMilestonesContext(Integer projectId) {
        Map<String, Object> planPayload = planService.getLatestPlan(projectId);
        Object milestones = planPayload.get("milestones");
        if (!(milestones instanceof List<?> milestoneList) || milestoneList.isEmpty()) {
            return "No construction plan uploaded.";
        }
        StringBuilder builder = new StringBuilder("### Construction Plan Milestones\n");
        for (Object item : milestoneList) {
            if (!(item instanceof Map<?, ?> milestone)) {
                continue;
            }
            builder.append("- Week ").append(milestone.get("weekNumber"))
                    .append(": ").append(milestone.get("title"))
                    .append(" (status: ").append(milestone.get("status")).append(')');
            if (milestone.get("expectedState") != null) {
                builder.append("\n  Expected: ").append(milestone.get("expectedState"));
            }
            if (milestone.get("actualState") != null) {
                builder.append("\n  Actual: ").append(milestone.get("actualState"));
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    private Map<String, Object> reportSummaryView(ProgressReportEntity entity) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", entity.getId());
        view.put("reportType", entity.getReportType());
        view.put("summary", entity.getSummary());
        view.put("dateRangeStart", entity.getDateRangeStart() == null ? null : entity.getDateRangeStart().toString());
        view.put("dateRangeEnd", entity.getDateRangeEnd() == null ? null : entity.getDateRangeEnd().toString());
        view.put("imageCount", entity.getImageCount());
        view.put("modelUsed", entity.getModelUsed());
        view.put("createdAt", entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString());
        return view;
    }

    private Map<String, Object> reportView(ProgressReportEntity entity) {
        Map<String, Object> view = new HashMap<>(reportSummaryView(entity));
        view.put("projectId", String.valueOf(entity.getProjectId()));
        view.put("contentMd", entity.getContentMd());
        return view;
    }
}
