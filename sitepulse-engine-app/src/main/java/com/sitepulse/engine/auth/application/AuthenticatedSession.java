package com.sitepulse.engine.auth.application;

public record AuthenticatedSession(
        String sessionToken,
        AuthenticatedUser user
) {
}
