package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.SessionLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutSessionUseCase {

    private final SessionLifecycleService sessionLifecycleService;

    public void logout(String sessionToken) {
        sessionLifecycleService.logout(sessionToken);
    }
}
