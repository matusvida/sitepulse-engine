package com.sitepulse.engine.project.web;

import com.sitepulse.engine.alert.application.result.AlertResult;
import com.sitepulse.engine.alert.application.usecase.ListProjectAlertsQuery;
import com.sitepulse.engine.alert.application.usecase.UpdateAlertStatusUseCase;
import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.detection.domain.ImageEntity;
import com.sitepulse.engine.detection.persistence.ImageRepository;
import com.sitepulse.engine.http.alert.dto.AlertStatusUpdateRequest;
import com.sitepulse.engine.http.alert.dto.AlertView;
import com.sitepulse.engine.http.common.dto.ActionResponse;
import com.sitepulse.engine.http.metrics.dto.MetricsGenerateRequest;
import com.sitepulse.engine.http.project.api.ProjectApi;
import com.sitepulse.engine.http.project.dto.CameraCreateRequest;
import com.sitepulse.engine.http.project.dto.CameraUpdateRequest;
import com.sitepulse.engine.http.project.dto.CameraView;
import com.sitepulse.engine.http.project.dto.ProjectCreateRequest;
import com.sitepulse.engine.http.project.dto.ProjectUpdateRequest;
import com.sitepulse.engine.http.project.dto.ProjectView;
import com.sitepulse.engine.http.project.dto.SyncStatusView;
import com.sitepulse.engine.http.visualization.dto.VisualizeRequest;
import com.sitepulse.engine.integration.storage.StorageService;
import com.sitepulse.engine.metrics.application.result.ActivityHeatmapPointResult;
import com.sitepulse.engine.metrics.application.result.DailyMetricResult;
import com.sitepulse.engine.metrics.application.result.WeeklyMetricResult;
import com.sitepulse.engine.metrics.application.usecase.GetActivityHeatmapQuery;
import com.sitepulse.engine.metrics.application.usecase.ListDailyMetricsQuery;
import com.sitepulse.engine.metrics.application.usecase.ListWeeklyMetricsQuery;
import com.sitepulse.engine.metrics.application.usecase.RunProjectAnalysisUseCase;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.application.command.CreateCameraCommand;
import com.sitepulse.engine.project.application.command.CreateProjectCommand;
import com.sitepulse.engine.project.application.command.UpdateCameraCommand;
import com.sitepulse.engine.project.application.command.UpdateProjectCommand;
import com.sitepulse.engine.project.application.usecase.CreateCameraUseCase;
import com.sitepulse.engine.project.application.usecase.CreateProjectUseCase;
import com.sitepulse.engine.project.application.usecase.GetProjectQuery;
import com.sitepulse.engine.project.application.usecase.ListProjectCamerasQuery;
import com.sitepulse.engine.project.application.usecase.ListProjectsQuery;
import com.sitepulse.engine.project.application.usecase.UpdateCameraUseCase;
import com.sitepulse.engine.project.application.usecase.UpdateProjectUseCase;
import com.sitepulse.engine.sync.application.result.SyncStatusResult;
import com.sitepulse.engine.sync.application.usecase.GetProjectSyncStatusQuery;
import com.sitepulse.engine.sync.application.usecase.TriggerProjectSyncUseCase;
import com.sitepulse.engine.visualization.application.VisualizationService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProjectController implements ProjectApi {

    private final ListProjectsQuery listProjectsQuery;
    private final GetProjectQuery getProjectQuery;
    private final CreateProjectUseCase createProjectUseCase;
    private final UpdateProjectUseCase updateProjectUseCase;
    private final ListProjectCamerasQuery listProjectCamerasQuery;
    private final CreateCameraUseCase createCameraUseCase;
    private final UpdateCameraUseCase updateCameraUseCase;
    private final ProjectLookupService projectLookupService;
    private final ListDailyMetricsQuery listDailyMetricsQuery;
    private final ListWeeklyMetricsQuery listWeeklyMetricsQuery;
    private final RunProjectAnalysisUseCase runProjectAnalysisUseCase;
    private final ListProjectAlertsQuery listProjectAlertsQuery;
    private final UpdateAlertStatusUseCase updateAlertStatusUseCase;
    private final GetProjectSyncStatusQuery getProjectSyncStatusQuery;
    private final TriggerProjectSyncUseCase triggerProjectSyncUseCase;
    private final ImageRepository imageRepository;
    private final StorageService storageService;
    private final VisualizationService visualizationService;
    private final GetActivityHeatmapQuery getActivityHeatmapQuery;

    @Override
    public List<ProjectView> listProjects() {
        return listProjectsQuery.get();
    }

    @Override
    public ProjectView getProject(Integer projectId) {
        return getProjectQuery.get(projectId);
    }

    @Override
    public ProjectView createProject(ProjectCreateRequest request) {
        return createProjectUseCase.create(new CreateProjectCommand(request.getName(), request.getLocation(), request.getDropboxPath()));
    }

    @Override
    public ProjectView updateProject(Integer projectId, ProjectUpdateRequest request) {
        return updateProjectUseCase.update(new UpdateProjectCommand(projectId, request.getName(), request.getLocation(), request.getDropboxPath()));
    }

    @Override
    public List<CameraView> listCameras(Integer projectId) {
        return listProjectCamerasQuery.get(projectId);
    }

    @Override
    public CameraView createCamera(Integer projectId, CameraCreateRequest request) {
        return createCameraUseCase.create(
                new CreateCameraCommand(projectId, request.getName(), request.getKeyPrefix(), request.getRoiPolygon(), request.getDropOutside())
        );
    }

    @Override
    public CameraView updateCamera(Integer projectId, Integer cameraId, CameraUpdateRequest request) {
        return updateCameraUseCase.update(
                new UpdateCameraCommand(projectId, cameraId, request.getRoiPolygon(), request.getDropOutside())
        );
    }

    @Override
    public List<Map<String, Object>> dailyMetrics(Integer projectId, int days) {
        return listDailyMetricsQuery.list(projectId, days).stream()
                .map(this::toDailyMetricView)
                .toList();
    }

    @Override
    public List<Map<String, Object>> weeklyMetrics(Integer projectId, int weeks) {
        return listWeeklyMetricsQuery.list(projectId, weeks).stream()
                .map(this::toWeeklyMetricView)
                .toList();
    }

    @Override
    public ActionResponse generateMetrics(Integer projectId, MetricsGenerateRequest request) {
        runProjectAnalysisUseCase.run(projectId, request == null || request.getLookbackDays() == null ? 30 : request.getLookbackDays());
        return new ActionResponse("accepted", "Metrics generation started", projectId);
    }

    @Override
    public List<AlertView> listAlerts(
            Integer projectId,
            String type,
            String severity,
            String status
    ) {
        projectLookupService.requireProject(projectId);
        return listProjectAlertsQuery.list(projectId, type, severity, status).stream()
                .map(this::toAlertView)
                .toList();
    }

    @Override
    public AlertView updateAlert(Integer projectId, Integer alertId, AlertStatusUpdateRequest request) {
        projectLookupService.requireProject(projectId);
        return toAlertView(updateAlertStatusUseCase.update(projectId, alertId, request));
    }

    @Override
    public SyncStatusView syncStatus(Integer projectId) {
        SyncStatusResult syncStatusResult = getProjectSyncStatusQuery.getLatest(projectId);
        if (syncStatusResult.isNeverRun()) {
            return new SyncStatusView(
                    null,
                    String.valueOf(syncStatusResult.getProjectId()),
                    "never_run",
                    syncStatusResult.getMessage(),
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
        return new SyncStatusView(
                String.valueOf(syncStatusResult.getJobId()),
                String.valueOf(syncStatusResult.getProjectId()),
                syncStatusResult.getStatus().name(),
                syncStatusResult.getMessage(),
                syncStatusResult.getImagesFound(),
                syncStatusResult.getImagesSynced(),
                syncStatusResult.getError(),
                syncStatusResult.getStartedAt() == null ? null : syncStatusResult.getStartedAt().toString(),
                syncStatusResult.getFinishedAt() == null ? null : syncStatusResult.getFinishedAt().toString()
        );
    }

    @Override
    public ActionResponse triggerSync(Integer projectId) {
        triggerProjectSyncUseCase.trigger(projectId);
        return new ActionResponse("accepted", "Sync job started in background", projectId);
    }

    @Override
    public List<Map<String, Object>> activityHeatmap(Integer projectId) {
        return getActivityHeatmapQuery.get(projectId).stream()
                .map(this::toActivityHeatmapView)
                .toList();
    }

    @Override
    public List<String> snapshotDates(Integer projectId) {
        projectLookupService.requireProject(projectId);
        return imageRepository.findSnapshotCapturedAtValues(projectId).stream()
                .map(instant -> instant.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate())
                .distinct()
                .map(LocalDate::toString)
                .toList();
    }

    @Override
    public ResponseEntity<byte[]> snapshot(Integer projectId, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        projectLookupService.requireProject(projectId);
        OffsetDateTime dayStart = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime dayEnd = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime midday = date.atTime(12, 0).atOffset(ZoneOffset.UTC);
        ImageEntity image = imageRepository.findClosestSnapshot(projectId, dayStart, dayEnd, midday)
                .orElseThrow(() -> new ApiException(org.springframework.http.HttpStatus.NOT_FOUND, "No image found for " + date));
        byte[] data = storageService.download(image.getBucket(), image.getKey());
        String mediaType = image.getKey().toLowerCase().endsWith(".png") ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(mediaType)).body(data);
    }

    @Override
    public Map<String, Object> visualize(Integer projectId, VisualizeRequest request) {
        return visualizationService.visualize(projectId, LocalDate.parse(request.getDateFrom()), LocalDate.parse(request.getDateTo()));
    }

    private AlertView toAlertView(AlertResult alert) {
        return new AlertView(
                alert.getId(),
                alert.getProjectId(),
                alert.getType(),
                alert.getSeverity().toPersistenceValue(),
                alert.getStatus().toPersistenceValue(),
                alert.getSummary(),
                alert.getDetails(),
                alert.getRecommendedActions(),
                alert.getCreatedAt() == null ? null : alert.getCreatedAt().toString(),
                alert.getUpdatedAt() == null ? null : alert.getUpdatedAt().toString()
        );
    }

    private Map<String, Object> toDailyMetricView(DailyMetricResult row) {
        return Map.of(
                "date", row.getDate().toString(),
                "peopleCount", row.getPeopleCount(),
                "vehicleCount", row.getVehicleCount(),
                "activeHours", row.getActiveHours()
        );
    }

    private Map<String, Object> toWeeklyMetricView(WeeklyMetricResult row) {
        return Map.of(
                "weekStart", row.getWeekStart().toString(),
                "progressDelta", row.getProgressDelta(),
                "activityIndex", row.getActivityIndex(),
                "activeHours", row.getActiveHours(),
                "riskLevel", row.getRiskLevel()
        );
    }

    private Map<String, Object> toActivityHeatmapView(ActivityHeatmapPointResult row) {
        return Map.of(
                "dayOfWeek", row.getDayOfWeek(),
                "hour", row.getHour(),
                "count", row.getCount()
        );
    }
}
