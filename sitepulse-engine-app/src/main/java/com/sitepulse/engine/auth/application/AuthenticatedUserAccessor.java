package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.exception.UnauthorizedException;
import com.sitepulse.engine.auth.infrastructure.security.SessionPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Component
public class AuthenticatedUserAccessor {

    public AuthenticatedUser requireCurrentUser() {
        return requireCurrentPrincipal().user();
    }

    public String requireCurrentSessionToken() {
        return requireCurrentPrincipal().sessionToken();
    }

    private SessionPrincipal requireCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SessionPrincipal principal) {
            return principal;
        }
        throw new UnauthorizedException("Authentication is required");
    }
}
