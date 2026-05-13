package com.sitepulse.engine.auth.web;

import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.config.SitePulseProperties;
import java.time.LocalTime;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class AuthSecurityTestConfig {

    @Bean
    @Primary
    SitePulseProperties sitePulseProperties() {
        return new SitePulseProperties(
                "postgresql://sitepulse:sitepulse@localhost:5432/sitepulse",
                "http://localhost:3000",
                "minio",
                "bucket",
                60,
                new SitePulseProperties.StorageProperties(
                        "http://localhost:9000",
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
                                null,
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
