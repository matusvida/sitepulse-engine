package com.sitepulse.engine.auth.infrastructure.email;

import com.sitepulse.engine.auth.application.AuthMailer;
import com.sitepulse.engine.auth.domain.model.AuthMailRecipient;
import com.sitepulse.engine.config.SitePulseProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ResendAuthMailer implements AuthMailer {

    private final SitePulseProperties properties;
    private final AuthEmailTemplateRenderer templateRenderer;
    private final ResendEmailSender emailSender;

    @Override
    public void sendInvitation(AuthMailRecipient user, String invitationUrl) {
        AuthEmailContent content = templateRenderer.renderInvitation(user, invitationUrl);
        emailSender.send(buildEmail(user, content));
    }

    @Override
    public void sendPasswordReset(AuthMailRecipient user, String resetUrl) {
        AuthEmailContent content = templateRenderer.renderPasswordReset(user, resetUrl);
        emailSender.send(buildEmail(user, content));
    }

    private ResendOutboundEmail buildEmail(AuthMailRecipient user, AuthEmailContent content) {
        return new ResendOutboundEmail(
                properties.auth().mail().normalizedFrom(),
                List.of(user.email()),
                content.subject(),
                content.html(),
                content.text(),
                properties.auth().mail().normalizedReplyTo()
        );
    }
}
