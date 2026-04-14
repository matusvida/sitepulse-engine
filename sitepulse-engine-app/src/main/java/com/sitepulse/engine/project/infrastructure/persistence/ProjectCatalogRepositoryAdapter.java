package com.sitepulse.engine.project.infrastructure.persistence;

import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProjectCatalogRepositoryAdapter implements ProjectCatalogRepository {

    private final ProjectRepository projectRepository;

    @Override
    public List<Project> findAll() {
        return projectRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<Project> findById(Integer projectId) {
        return projectRepository.findById(projectId).map(this::toDomain);
    }

    @Override
    public Project save(Project project) {
        ProjectEntity entity = ProjectEntity.builder()
                .id(project.getId())
                .name(project.getName())
                .location(project.getLocation())
                .storageKeyPrefix(project.getStorageKeyPrefix())
                .timezone(project.getTimezone())
                .createdAt(project.getCreatedAt())
                .build();
        return toDomain(projectRepository.save(entity));
    }

    private Project toDomain(ProjectEntity entity) {
        return Project.restore(entity.getId(), entity.getName(), entity.getLocation(), entity.getStorageKeyPrefix(), entity.getTimezone(), entity.getCreatedAt());
    }
}
