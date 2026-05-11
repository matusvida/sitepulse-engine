package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.AuthenticatedSession;
import com.sitepulse.engine.auth.application.SessionLifecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResolveAuthenticatedSessionUseCase {

    private final SessionLifecycleService sessionLifecycleService;

    @Transactional
    public AuthenticatedSession resolve(String sessionToken) {
        return sessionLifecycleService.requireAuthenticatedSession(sessionToken);
    }
}
