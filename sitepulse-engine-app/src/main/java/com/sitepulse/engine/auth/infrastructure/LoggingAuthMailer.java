package com.sitepulse.engine.auth.infrastructure;

import com.sitepulse.engine.auth.application.AuthMailer;
import com.sitepulse.engine.auth.infrastructure.persistence.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingAuthMailer implements AuthMailer {

    @Override
    public void sendInvitation(UserEntity user, String invitationUrl) {
        log.info("Invitation for {} -> {}", user.getEmail(), invitationUrl);
    }

    @Override
    public void sendPasswordReset(UserEntity user, String resetUrl) {
        log.info("Password reset for {} -> {}", user.getEmail(), resetUrl);
    }
}
