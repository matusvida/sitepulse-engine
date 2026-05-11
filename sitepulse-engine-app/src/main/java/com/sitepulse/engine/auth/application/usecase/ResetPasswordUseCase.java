package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.PasswordResetFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {

    private final PasswordResetFlowService passwordResetFlowService;

    public void reset(String token, String password) {
        passwordResetFlowService.resetPassword(token, password);
    }
}
