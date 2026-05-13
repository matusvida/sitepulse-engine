package com.sitepulse.engine.auth.infrastructure.email;

public interface ResendEmailSender {

    void send(ResendOutboundEmail email);
}
