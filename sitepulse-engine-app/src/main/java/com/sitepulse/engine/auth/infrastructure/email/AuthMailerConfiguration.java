package com.sitepulse.engine.auth.infrastructure.email;

import com.sitepulse.engine.auth.application.AuthMailer;
import com.sitepulse.engine.auth.infrastructure.LoggingAuthMailer;
import com.sitepulse.engine.config.SitePulseProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class AuthMailerConfiguration {

    @Bean
    AuthMailer authMailer(SitePulseProperties properties, AuthEmailTemplateRenderer templateRenderer) {
        if (!properties.auth().mail().canUseResend()) {
            log.info("Using logging auth mailer fallback because Resend mail configuration is incomplete or disabled");
            return new LoggingAuthMailer();
        }
        log.info("Using Resend auth mailer for transactional auth emails");
        return new ResendAuthMailer(
                properties,
                templateRenderer,
                new ResendApiEmailSender(properties)
        );
    }
}
