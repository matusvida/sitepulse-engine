package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.model.AuthMailRecipient;

public interface AuthMailer {

    void sendInvitation(AuthMailRecipient recipient, String invitationUrl);

    void sendPasswordReset(AuthMailRecipient recipient, String resetUrl);
}
