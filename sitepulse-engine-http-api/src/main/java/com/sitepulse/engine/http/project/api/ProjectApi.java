package com.sitepulse.engine.http.project.api;

import com.sitepulse.engine.http.project.dto.CameraCreateRequest;
import com.sitepulse.engine.http.project.dto.CameraUpdateRequest;
import com.sitepulse.engine.http.project.dto.CameraView;
import com.sitepulse.engine.http.project.dto.ProjectCreateRequest;
import com.sitepulse.engine.http.project.dto.ProjectSnapshotView;
import com.sitepulse.engine.http.project.dto.ProjectUpdateRequest;
import com.sitepulse.engine.http.project.dto.ProjectView;
import com.sitepulse.engine.http.common.dto.ActionResponse;
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

    @Operation(summary = "List dates with snapshots")
    @GetMapping("/projects/{projectId}/snapshot/dates")
    List<String> snapshotDates(@PathVariable Integer projectId);

    @Operation(summary = "List project snapshots with signed URLs")
    @GetMapping("/projects/{projectId}/snapshots")
    List<ProjectSnapshotView> snapshots(@PathVariable Integer projectId);

    @Operation(summary = "Backfill derived project snapshots")
    @PostMapping("/projects/{projectId}/snapshots/backfill")
    ActionResponse backfillSnapshots(
            @PathVariable Integer projectId,
            @RequestParam(value = "cameraId", required = false) Integer cameraId,
            @RequestParam(value = "force", required = false, defaultValue = "false") boolean force
    );

    @Operation(summary = "Get a project snapshot")
    @GetMapping(value = "/projects/{projectId}/snapshot", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE, "image/webp"})
    ResponseEntity<byte[]> snapshot(
            @PathVariable Integer projectId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    );
}
