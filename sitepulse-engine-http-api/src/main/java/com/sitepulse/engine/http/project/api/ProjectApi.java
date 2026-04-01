package com.sitepulse.engine.http.project.api;

import com.sitepulse.engine.http.alert.dto.AlertStatusUpdateRequest;
import com.sitepulse.engine.http.alert.dto.AlertView;
import com.sitepulse.engine.http.common.dto.ActionResponse;
import com.sitepulse.engine.http.metrics.dto.ActivityHeatmapPointView;
import com.sitepulse.engine.http.metrics.dto.DailyMetricView;
import com.sitepulse.engine.http.metrics.dto.MetricsGenerateRequest;
import com.sitepulse.engine.http.metrics.dto.WeeklyMetricView;
import com.sitepulse.engine.http.project.dto.CameraCreateRequest;
import com.sitepulse.engine.http.project.dto.CameraUpdateRequest;
import com.sitepulse.engine.http.project.dto.CameraView;
import com.sitepulse.engine.http.project.dto.ProjectCreateRequest;
import com.sitepulse.engine.http.project.dto.ProjectUpdateRequest;
import com.sitepulse.engine.http.project.dto.ProjectView;
import com.sitepulse.engine.http.project.dto.SyncStatusView;
import com.sitepulse.engine.http.visualization.dto.VisualizeRequest;
import com.sitepulse.engine.http.visualization.dto.VisualizationResultView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Projects")
@RequestMapping("/api")
public interface ProjectApi {

    @Operation(summary = "List projects")
    @GetMapping("/projects")
    List<ProjectView> listProjects();

    @Operation(summary = "Get project detail")
    @GetMapping("/projects/{projectId}")
    ProjectView getProject(@PathVariable Integer projectId);

    @Operation(summary = "Create project")
    @PostMapping("/projects")
    ProjectView createProject(@Valid @RequestBody ProjectCreateRequest request);

    @Operation(summary = "Update project")
    @PatchMapping("/projects/{projectId}")
    ProjectView updateProject(@PathVariable Integer projectId, @Valid @RequestBody ProjectUpdateRequest request);

    @Operation(summary = "List project cameras")
    @GetMapping("/projects/{projectId}/cameras")
    List<CameraView> listCameras(@PathVariable Integer projectId);

    @Operation(summary = "Create project camera")
    @PostMapping("/projects/{projectId}/cameras")
    CameraView createCamera(@PathVariable Integer projectId, @Valid @RequestBody CameraCreateRequest request);

    @Operation(summary = "Update project camera")
    @PatchMapping("/projects/{projectId}/cameras/{cameraId}")
    CameraView updateCamera(@PathVariable Integer projectId, @PathVariable Integer cameraId, @Valid @RequestBody CameraUpdateRequest request);

    @Operation(summary = "Get daily metrics")
    @GetMapping("/projects/{projectId}/metrics/daily")
    List<DailyMetricView> dailyMetrics(@PathVariable Integer projectId, @RequestParam(defaultValue = "28") int days);

    @Operation(summary = "Get weekly metrics")
    @GetMapping("/projects/{projectId}/metrics/weekly")
    List<WeeklyMetricView> weeklyMetrics(@PathVariable Integer projectId, @RequestParam(defaultValue = "12") int weeks);

    @Operation(summary = "Trigger metrics generation")
    @PostMapping("/projects/{projectId}/metrics/generate")
    ActionResponse generateMetrics(@PathVariable Integer projectId, @RequestBody(required = false) MetricsGenerateRequest request);

    @Operation(summary = "List alerts")
    @GetMapping("/projects/{projectId}/alerts")
    List<AlertView> listAlerts(
            @PathVariable Integer projectId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status
    );

    @Operation(summary = "Update alert status")
    @PatchMapping("/projects/{projectId}/alerts/{alertId}")
    AlertView updateAlert(@PathVariable Integer projectId, @PathVariable Integer alertId, @Valid @RequestBody AlertStatusUpdateRequest request);

    @Operation(summary = "Get latest sync status")
    @GetMapping("/projects/{projectId}/sync/status")
    SyncStatusView syncStatus(@PathVariable Integer projectId);

    @Operation(summary = "Trigger a project sync")
    @PostMapping("/projects/{projectId}/sync/trigger")
    ActionResponse triggerSync(@PathVariable Integer projectId);

    @Operation(summary = "Get activity heatmap")
    @GetMapping("/projects/{projectId}/activity/heatmap")
    List<ActivityHeatmapPointView> activityHeatmap(@PathVariable Integer projectId);

    @Operation(summary = "List dates with snapshots")
    @GetMapping("/projects/{projectId}/snapshot/dates")
    List<String> snapshotDates(@PathVariable Integer projectId);

    @Operation(summary = "Get a project snapshot")
    @GetMapping(value = "/projects/{projectId}/snapshot", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    ResponseEntity<byte[]> snapshot(
            @PathVariable Integer projectId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );

    @Operation(summary = "Generate a visualization overlay")
    @PostMapping("/projects/{projectId}/visualize")
    VisualizationResultView visualize(@PathVariable Integer projectId, @Valid @RequestBody VisualizeRequest request);
}
