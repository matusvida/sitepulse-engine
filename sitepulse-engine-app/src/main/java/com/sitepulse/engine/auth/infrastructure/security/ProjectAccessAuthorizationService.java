package com.sitepulse.engine.auth.infrastructure.security;

import com.sitepulse.engine.auth.application.UserProjectAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("projectAccessAuthorizationService")
@RequiredArgsConstructor
public class ProjectAccessAuthorizationService {

    private final UserProjectAccessPolicy userProjectAccessPolicy;

    public boolean hasProjectAccess(Authentication authentication, Integer projectId) {
        if (!(authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal)) {
            return false;
        }
        return userProjectAccessPolicy.hasProjectAccess(principal.user(), projectId);
    }
}
