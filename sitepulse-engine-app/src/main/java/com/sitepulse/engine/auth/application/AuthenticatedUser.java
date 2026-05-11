package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.UserRole;

public record AuthenticatedUser(
        Integer id,
        String email,
        UserRole role
) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
