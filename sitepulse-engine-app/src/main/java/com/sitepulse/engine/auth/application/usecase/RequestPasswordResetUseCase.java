package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.PasswordResetFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestPasswordResetUseCase {

    private final PasswordResetFlowService passwordResetFlowService;

    public void request(String email) {
        passwordResetFlowService.sendPasswordReset(email);
    }
}
