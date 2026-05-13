package com.sitepulse.engine.auth.infrastructure.email;

import java.util.List;

public record ResendOutboundEmail(
        String from,
        List<String> to,
        String subject,
        String html,
        String text,
        String replyTo
) {
}
