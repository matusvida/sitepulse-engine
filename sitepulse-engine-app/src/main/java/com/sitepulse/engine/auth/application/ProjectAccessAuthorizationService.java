package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.exception.ForbiddenException;
import com.sitepulse.engine.auth.infrastructure.persistence.UserProjectAccessEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserProjectAccessRepository;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessAuthorizationService {

    private final UserProjectAccessRepository userProjectAccessRepository;
    private final ProjectCatalogRepository projectCatalogRepository;

    public List<Project> authorizedProjects(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser.isAdmin()) {
            return projectCatalogRepository.findAll();
        }
        Set<Integer> accessibleProjectIds = userProjectAccessRepository.findByUserId(authenticatedUser.id()).stream()
                .map(UserProjectAccessEntity::getProjectId)
                .collect(Collectors.toSet());
        return projectCatalogRepository.findAll().stream()
                .filter(project -> accessibleProjectIds.contains(project.getId()))
                .toList();
    }

    public void requireProjectAccess(AuthenticatedUser authenticatedUser, Integer projectId) {
        if (authenticatedUser.isAdmin()) {
            return;
        }
        if (!userProjectAccessRepository.existsByUserIdAndProjectId(authenticatedUser.id(), projectId)) {
            throw new ForbiddenException("You do not have access to this project");
        }
    }

    public void requireAdmin(AuthenticatedUser authenticatedUser) {
        if (!authenticatedUser.isAdmin()) {
            throw new ForbiddenException("Admin access is required");
        }
    }

    public boolean hasProjectAccess(Authentication authentication, Integer projectId) {
        if (!(authentication != null && authentication.getPrincipal() instanceof com.sitepulse.engine.auth.infrastructure.security.SessionPrincipal principal)) {
            return false;
        }
        AuthenticatedUser authenticatedUser = principal.user();
        return authenticatedUser.isAdmin()
                || userProjectAccessRepository.existsByUserIdAndProjectId(authenticatedUser.id(), projectId);
    }
}
