package com.sitepulse.engine.auth.infrastructure;

import com.sitepulse.engine.auth.application.AuthMailer;
import com.sitepulse.engine.auth.domain.model.AuthMailRecipient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingAuthMailer implements AuthMailer {

    @Override
    public void sendInvitation(AuthMailRecipient recipient, String invitationUrl) {
        log.info("Invitation for {} -> {}", recipient.email(), invitationUrl);
    }

    @Override
    public void sendPasswordReset(AuthMailRecipient recipient, String resetUrl) {
        log.info("Password reset for {} -> {}", recipient.email(), resetUrl);
    }
}
