package com.sitepulse.engine.project.web;

import com.sitepulse.engine.alert.application.AlertService;
import com.sitepulse.engine.alert.domain.AlertEntity;
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
import com.sitepulse.engine.metrics.application.AnalysisService;
import com.sitepulse.engine.project.application.ProjectService;
import com.sitepulse.engine.sync.application.SyncService;
import com.sitepulse.engine.sync.domain.SyncJobEntity;
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

    private final ProjectService projectService;
    private final AnalysisService analysisService;
    private final AlertService alertService;
    private final SyncService syncService;
    private final ImageRepository imageRepository;
    private final StorageService storageService;
    private final VisualizationService visualizationService;

    @Override
    public List<ProjectView> listProjects() {
        return projectService.listProjects();
    }

    @Override
    public ProjectView getProject(Integer projectId) {
        return projectService.getProject(projectId);
    }

    @Override
    public ProjectView createProject(ProjectCreateRequest request) {
        return projectService.createProject(request);
    }

    @Override
    public ProjectView updateProject(Integer projectId, ProjectUpdateRequest request) {
        return projectService.updateProject(projectId, request);
    }

    @Override
    public List<CameraView> listCameras(Integer projectId) {
        return projectService.listCameras(projectId);
    }

    @Override
    public CameraView createCamera(Integer projectId, CameraCreateRequest request) {
        return projectService.createCamera(projectId, request);
    }

    @Override
    public CameraView updateCamera(Integer projectId, Integer cameraId, CameraUpdateRequest request) {
        return projectService.updateCamera(projectId, cameraId, request);
    }

    @Override
    public List<Map<String, Object>> dailyMetrics(Integer projectId, int days) {
        return analysisService.dailyMetrics(projectId, days);
    }

    @Override
    public List<Map<String, Object>> weeklyMetrics(Integer projectId, int weeks) {
        return analysisService.weeklyMetrics(projectId, weeks);
    }

    @Override
    public ActionResponse generateMetrics(Integer projectId, MetricsGenerateRequest request) {
        analysisService.runAnalysisForProject(projectId, request == null || request.getLookbackDays() == null ? 30 : request.getLookbackDays());
        return new ActionResponse("accepted", "Metrics generation started", projectId);
    }

    @Override
    public List<AlertView> listAlerts(
            Integer projectId,
            String type,
            String severity,
            String status
    ) {
        projectService.requireProject(projectId);
        return alertService.listAlerts(projectId, type, severity, status).stream()
                .map(this::toAlertView)
                .toList();
    }

    @Override
    public AlertView updateAlert(Integer projectId, Integer alertId, AlertStatusUpdateRequest request) {
        projectService.requireProject(projectId);
        return toAlertView(alertService.updateStatus(projectId, alertId, request));
    }

    @Override
    public SyncStatusView syncStatus(Integer projectId) {
        projectService.requireProject(projectId);
        SyncJobEntity job = syncService.latestSyncJob(projectId);
        if (job == null) {
            return new SyncStatusView(null, String.valueOf(projectId), "never_run", "No sync jobs have been run for this project", null, null, null, null, null);
        }
        return new SyncStatusView(
                String.valueOf(job.getId()),
                String.valueOf(job.getProjectId()),
                job.getStatus(),
                null,
                job.getImagesFound(),
                job.getImagesSynced(),
                job.getError(),
                job.getStartedAt() == null ? null : job.getStartedAt().toString(),
                job.getFinishedAt() == null ? null : job.getFinishedAt().toString()
        );
    }

    @Override
    public ActionResponse triggerSync(Integer projectId) {
        projectService.requireProject(projectId);
        syncService.requireSyncableProject(projectId);
        syncService.triggerProjectSync(projectId);
        return new ActionResponse("accepted", "Sync job started in background", projectId);
    }

    @Override
    public List<Map<String, Object>> activityHeatmap(Integer projectId) {
        return analysisService.activityHeatmap(projectId);
    }

    @Override
    public List<String> snapshotDates(Integer projectId) {
        projectService.requireProject(projectId);
        return imageRepository.findSnapshotCapturedAtValues(projectId).stream()
                .map(instant -> instant.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate())
                .distinct()
                .map(LocalDate::toString)
                .toList();
    }

    @Override
    public ResponseEntity<byte[]> snapshot(Integer projectId, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        projectService.requireProject(projectId);
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

    private AlertView toAlertView(AlertEntity alert) {
        return new AlertView(
                alert.getId(),
                alert.getProjectId(),
                alert.getType(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getSummary(),
                alert.getDetails(),
                alert.getRecommendedActions(),
                alert.getCreatedAt() == null ? null : alert.getCreatedAt().toString(),
                alert.getUpdatedAt() == null ? null : alert.getUpdatedAt().toString()
        );
    }
}
