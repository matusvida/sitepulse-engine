package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.infrastructure.persistence.UserEntity;

public interface AuthMailer {

    void sendInvitation(UserEntity user, String invitationUrl);

    void sendPasswordReset(UserEntity user, String resetUrl);
}
