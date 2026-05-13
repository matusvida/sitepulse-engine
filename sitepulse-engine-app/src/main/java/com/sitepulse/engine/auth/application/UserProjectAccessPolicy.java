package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.port.UserProjectAccessStore;
import com.sitepulse.engine.auth.exception.ForbiddenException;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProjectAccessPolicy {

    private final UserProjectAccessStore userProjectAccessStore;
    private final ProjectCatalogRepository projectCatalogRepository;

    public List<Project> authorizedProjects(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser.isAdmin()) {
            return projectCatalogRepository.findAll();
        }
        Set<Integer> accessibleProjectIds = userProjectAccessStore.findProjectIdSetByUserId(authenticatedUser.id());
        return projectCatalogRepository.findAll().stream()
                .filter(project -> accessibleProjectIds.contains(project.getId()))
                .toList();
    }

    public void requireProjectAccess(AuthenticatedUser authenticatedUser, Integer projectId) {
        if (authenticatedUser.isAdmin()) {
            return;
        }
        if (!hasProjectAccess(authenticatedUser, projectId)) {
            throw new ForbiddenException("You do not have access to this project");
        }
    }

    public void requireAdmin(AuthenticatedUser authenticatedUser) {
        if (!authenticatedUser.isAdmin()) {
            throw new ForbiddenException("Admin access is required");
        }
    }

    public boolean hasProjectAccess(AuthenticatedUser authenticatedUser, Integer projectId) {
        return authenticatedUser.isAdmin()
                || userProjectAccessStore.existsByUserIdAndProjectId(authenticatedUser.id(), projectId);
    }
}
