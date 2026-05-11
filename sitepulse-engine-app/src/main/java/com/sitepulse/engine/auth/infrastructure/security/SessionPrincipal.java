package com.sitepulse.engine.auth.infrastructure.security;

import com.sitepulse.engine.auth.application.AuthenticatedUser;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public record SessionPrincipal(
        String sessionToken,
        AuthenticatedUser user
) {

    public List<GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name()));
    }
}
