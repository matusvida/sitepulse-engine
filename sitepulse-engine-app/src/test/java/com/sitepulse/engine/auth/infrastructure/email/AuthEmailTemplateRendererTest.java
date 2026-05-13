package com.sitepulse.engine.auth.infrastructure.email;

import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.auth.domain.model.AuthMailRecipient;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthEmailTemplateRendererTest {

    @Test
    void invitationTemplateIncludesUserNameAndInvitationLink() {
        AuthEmailTemplateRenderer renderer = new AuthEmailTemplateRenderer(new DefaultResourceLoader(), testProperties());

        AuthEmailContent content = renderer.renderInvitation(user(), "https://app.sitepulse.ai/invite?token=abc123");

        assertTrue(content.subject().contains("SitePulse"));
        assertTrue(content.html().contains("Paula"));
        assertTrue(content.html().contains("https://app.sitepulse.ai/invite?token=abc123"));
        assertTrue(content.text().contains("72 hours"));
    }

    @Test
    void passwordResetTemplateFallsBackWhenFirstNameMissing() {
        AuthEmailTemplateRenderer renderer = new AuthEmailTemplateRenderer(new DefaultResourceLoader(), testProperties());
        AuthMailRecipient user = new AuthMailRecipient("planner@example.com", null, "Planner");

        AuthEmailContent content = renderer.renderPasswordReset(user, "https://app.sitepulse.ai/reset-password?token=reset123");

        assertTrue(content.html().contains("Hi there"));
        assertTrue(content.text().contains("reset-password?token=reset123"));
        assertTrue(content.text().contains("1 hour"));
    }

    private static AuthMailRecipient user() {
        return new AuthMailRecipient("planner@example.com", "Paula", "Planner");
    }

    private static SitePulseProperties testProperties() {
        return new SitePulseProperties(
                "postgresql://sitepulse:sitepulse@localhost:5432/sitepulse",
                "http://localhost:3000",
                "minio",
                "tower-tl",
                60,
                new SitePulseProperties.StorageProperties(
                        "http://minio:9000",
                        "http://localhost:9001",
                        "admin",
                        "password123",
                        "eu-central-1"
                ),
                "yolov8x.pt",
                0.35,
                "{}",
                400d,
                false,
                "roi_config.json",
                50d,
                30,
                240,
                false,
                "0 0/10 * * * *",
                "0 5/10 * * * *",
                "openai",
                "0 0 2 * * *",
                3,
                null,
                null,
                null,
                null,
                null,
                "gpt-4.1",
                52_428_800L,
                "http://python-yolo:8000",
                new SitePulseProperties.ImageWebSnapshotsProperties(true, 1920, 75, ImageFormat.WEBP, LocalTime.of(17, 0)),
                new SitePulseProperties.AuthProperties(
                        "http://localhost:3000",
                        "sitepulse_session",
                        false,
                        "Lax",
                        168,
                        72,
                        1,
                        null,
                        null,
                        new SitePulseProperties.MailProperties(
                                true,
                                "SitePulse",
                                "SitePulse <noreply@example.com>",
                                "support@example.com",
                                new SitePulseProperties.ResendProperties(
                                        true,
                                        "https://api.resend.com",
                                        "re_test"
                                )
                        )
                )
        );
    }
}
