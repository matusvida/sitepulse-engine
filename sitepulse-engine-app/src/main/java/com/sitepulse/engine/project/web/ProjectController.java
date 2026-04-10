package com.sitepulse.engine.project.web;

import com.sitepulse.engine.http.project.api.ProjectApi;
import com.sitepulse.engine.http.project.dto.CameraCreateRequest;
import com.sitepulse.engine.http.project.dto.CameraUpdateRequest;
import com.sitepulse.engine.http.project.dto.CameraView;
import com.sitepulse.engine.http.project.dto.ProjectCreateRequest;
import com.sitepulse.engine.http.project.dto.ProjectSnapshotView;
import com.sitepulse.engine.http.project.dto.ProjectUpdateRequest;
import com.sitepulse.engine.http.project.dto.ProjectView;
import com.sitepulse.engine.project.application.command.CreateCameraCommand;
import com.sitepulse.engine.project.application.command.CreateProjectCommand;
import com.sitepulse.engine.project.application.command.UpdateCameraCommand;
import com.sitepulse.engine.project.application.command.UpdateProjectCommand;
import com.sitepulse.engine.project.application.result.CameraResult;
import com.sitepulse.engine.project.application.result.ProjectResult;
import com.sitepulse.engine.project.application.result.ProjectSnapshotMetadataResult;
import com.sitepulse.engine.project.application.result.ProjectSnapshotResult;
import com.sitepulse.engine.project.application.usecase.CreateCameraUseCase;
import com.sitepulse.engine.project.application.usecase.CreateProjectUseCase;
import com.sitepulse.engine.project.application.usecase.GetProjectSnapshotQuery;
import com.sitepulse.engine.project.application.usecase.GetProjectQuery;
import com.sitepulse.engine.project.application.usecase.ListProjectSnapshotsQuery;
import com.sitepulse.engine.project.application.usecase.ListSnapshotDatesQuery;
import com.sitepulse.engine.project.application.usecase.ListProjectCamerasQuery;
import com.sitepulse.engine.project.application.usecase.ListProjectsQuery;
import com.sitepulse.engine.project.application.usecase.UpdateCameraUseCase;
import com.sitepulse.engine.project.application.usecase.UpdateProjectUseCase;
import java.time.LocalDate;
import java.util.List;
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
    private final ListSnapshotDatesQuery listSnapshotDatesQuery;
    private final ListProjectSnapshotsQuery listProjectSnapshotsQuery;
    private final GetProjectSnapshotQuery getProjectSnapshotQuery;

    @Override
    public List<ProjectView> listProjects() {
        return listProjectsQuery.get().stream().map(this::toProjectView).toList();
    }

    @Override
    public ProjectView getProject(Integer projectId) {
        return toProjectView(getProjectQuery.get(projectId));
    }

    @Override
    public ProjectView createProject(ProjectCreateRequest request) {
        return toProjectView(createProjectUseCase.create(
                new CreateProjectCommand(request.getName(), request.getLocation(), request.getStorageKeyPrefix())));
    }

    @Override
    public ProjectView updateProject(Integer projectId, ProjectUpdateRequest request) {
        return toProjectView(updateProjectUseCase.update(
                new UpdateProjectCommand(projectId, request.getName(), request.getLocation(), request.getStorageKeyPrefix())));
    }

    @Override
    public List<CameraView> listCameras(Integer projectId) {
        return listProjectCamerasQuery.get(projectId).stream().map(this::toCameraView).toList();
    }

    @Override
    public CameraView createCamera(Integer projectId, CameraCreateRequest request) {
        return toCameraView(createCameraUseCase.create(
                new CreateCameraCommand(projectId, request.getName(), request.getDropboxPath(), request.getKeyPrefix(), request.getRoiPolygon(), request.getDropOutside())));
    }

    @Override
    public CameraView updateCamera(Integer projectId, Integer cameraId, CameraUpdateRequest request) {
        return toCameraView(updateCameraUseCase.update(
                new UpdateCameraCommand(projectId, cameraId, request.getDropboxPath(), request.getKeyPrefix(), request.getRoiPolygon(), request.getDropOutside())));
    }

    @Override
    public List<String> snapshotDates(Integer projectId) {
        return listSnapshotDatesQuery.list(projectId).stream()
                .map(LocalDate::toString)
                .toList();
    }

    @Override
    public List<ProjectSnapshotView> snapshots(Integer projectId) {
        return listProjectSnapshotsQuery.list(projectId).stream()
                .map(this::toProjectSnapshotView)
                .toList();
    }

    @Override
    public ResponseEntity<byte[]> snapshot(Integer projectId, @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ProjectSnapshotResult snapshot = getProjectSnapshotQuery.get(projectId, date);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(snapshot.getMediaType())).body(snapshot.getContent());
    }

    private ProjectView toProjectView(ProjectResult result) {
        return new ProjectView(
                String.valueOf(result.id()),
                result.name(),
                result.location(),
                result.imageCount(),
                result.cameraCount(),
                result.latestSnapshotAt(),
                result.storageKeyPrefix(),
                result.createdAt() == null ? null : result.createdAt().toString()
        );
    }

    private CameraView toCameraView(CameraResult result) {
        return new CameraView(
                result.id(),
                result.projectId(),
                result.name(),
                result.dropboxPath(),
                result.roiPolygon(),
                result.dropOutside(),
                result.keyPrefix(),
                result.createdAt() == null ? null : result.createdAt().toString()
        );
    }

    private ProjectSnapshotView toProjectSnapshotView(ProjectSnapshotMetadataResult result) {
        return new ProjectSnapshotView(
                result.date().toString(),
                result.url(),
                result.expiresAt(),
                result.mediaType()
        );
    }
}
