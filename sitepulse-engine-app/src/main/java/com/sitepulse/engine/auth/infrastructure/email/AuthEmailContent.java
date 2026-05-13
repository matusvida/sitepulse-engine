package com.sitepulse.engine.auth.infrastructure.email;

public record AuthEmailContent(
        String subject,
        String html,
        String text
) {
}
