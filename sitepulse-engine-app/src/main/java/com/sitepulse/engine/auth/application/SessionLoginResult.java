package com.sitepulse.engine.auth.application;

public record SessionLoginResult(
        String sessionToken,
        AuthUserResult user
) {
}
