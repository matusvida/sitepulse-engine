package com.sitepulse.engine.project.application;

import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.detection.domain.ImageEntity;
import com.sitepulse.engine.detection.persistence.ImageRepository;
import com.sitepulse.engine.http.project.dto.CameraCreateRequest;
import com.sitepulse.engine.http.project.dto.CameraUpdateRequest;
import com.sitepulse.engine.http.project.dto.CameraView;
import com.sitepulse.engine.http.project.dto.ProjectCreateRequest;
import com.sitepulse.engine.http.project.dto.ProjectUpdateRequest;
import com.sitepulse.engine.http.project.dto.ProjectView;
import com.sitepulse.engine.project.domain.CameraEntity;
import com.sitepulse.engine.project.domain.ProjectEntity;
import com.sitepulse.engine.project.persistence.CameraRepository;
import com.sitepulse.engine.project.persistence.ProjectRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CameraRepository cameraRepository;
    private final ImageRepository imageRepository;

    public List<ProjectView> listProjects() {
        return projectRepository.findAll().stream().map(this::toProjectView).toList();
    }

    public ProjectView getProject(Integer projectId) {
        return toProjectView(requireProject(projectId));
    }

    @Transactional
    public ProjectView createProject(ProjectCreateRequest request) {
        ProjectEntity entity = ProjectEntity.builder()
                .name(request.getName())
                .location(nullIfBlank(request.getLocation()))
                .dropboxPath(nullIfBlank(request.getDropboxPath()))
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        return toProjectView(projectRepository.save(entity));
    }

    @Transactional
    public ProjectView updateProject(Integer projectId, ProjectUpdateRequest request) {
        ProjectEntity entity = requireProject(projectId);
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getLocation() != null) {
            entity.setLocation(request.getLocation());
        }
        if (request.getDropboxPath() != null) {
            entity.setDropboxPath(request.getDropboxPath());
        }
        return toProjectView(projectRepository.save(entity));
    }

    public List<CameraView> listCameras(Integer projectId) {
        requireProject(projectId);
        return cameraRepository.findByProjectIdOrderById(projectId).stream().map(this::toCameraView).toList();
    }

    @Transactional
    public CameraView createCamera(Integer projectId, CameraCreateRequest request) {
        requireProject(projectId);
        CameraEntity entity = CameraEntity.builder()
                .projectId(projectId)
                .name(request.getName())
                .keyPrefix(nullIfBlank(request.getKeyPrefix()))
                .roiPolygon(request.getRoiPolygon())
                .dropOutside(request.getDropOutside() == null ? Boolean.TRUE : request.getDropOutside())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        return toCameraView(cameraRepository.save(entity));
    }

    @Transactional
    public CameraView updateCamera(Integer projectId, Integer cameraId, CameraUpdateRequest request) {
        requireProject(projectId);
        CameraEntity entity = cameraRepository.findByIdAndProjectId(cameraId, projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Camera not found"));
        if (request.getRoiPolygon() != null) {
            entity.setRoiPolygon(request.getRoiPolygon());
        }
        if (request.getDropOutside() != null) {
            entity.setDropOutside(request.getDropOutside());
        }
        return toCameraView(cameraRepository.save(entity));
    }

    public ProjectEntity requireProject(Integer projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    public CameraEntity findCameraByKey(Integer projectId, String key) {
        return cameraRepository.findByProjectIdAndKeyPrefixIsNotNullOrderByKeyPrefixDesc(projectId).stream()
                .filter(camera -> key.startsWith(camera.getKeyPrefix()))
                .max(Comparator.comparingInt(camera -> camera.getKeyPrefix().length()))
                .orElse(null);
    }

    private ProjectView toProjectView(ProjectEntity entity) {
        long cameraCount = cameraRepository.findByProjectIdOrderById(entity.getId()).size();
        List<ImageEntity> images = imageRepository.findProcessedByProject(entity.getId());
        String lastSnapshot = images.isEmpty() || images.getFirst().getCapturedAt() == null ? "" : images.getFirst().getCapturedAt().toString();
        return new ProjectView(
                String.valueOf(entity.getId()),
                entity.getName(),
                entity.getLocation() == null ? "" : entity.getLocation(),
                0,
                (int) cameraCount,
                lastSnapshot,
                entity.getDropboxPath(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString()
        );
    }

    private CameraView toCameraView(CameraEntity entity) {
        return new CameraView(
                entity.getId(),
                entity.getProjectId(),
                entity.getName(),
                entity.getRoiPolygon(),
                entity.getDropOutside(),
                entity.getKeyPrefix(),
                entity.getCreatedAt() == null ? null : entity.getCreatedAt().toString()
        );
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
