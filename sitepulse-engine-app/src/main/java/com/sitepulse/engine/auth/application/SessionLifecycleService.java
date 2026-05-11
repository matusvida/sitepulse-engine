package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.exception.UnauthorizedException;
import com.sitepulse.engine.auth.infrastructure.persistence.UserEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserRepository;
import com.sitepulse.engine.auth.infrastructure.persistence.UserSessionEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserSessionRepository;
import com.sitepulse.engine.config.SitePulseProperties;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionLifecycleService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final TokenService tokenService;
    private final SitePulseProperties properties;
    private final AuthUserResultFactory authUserResultFactory;

    @Transactional
    public SessionLoginResult createSession(UserEntity user) {
        String rawToken = tokenService.generateOpaqueToken();
        OffsetDateTime now = OffsetDateTime.now();
        userSessionRepository.save(UserSessionEntity.builder()
                .userId(user.getId())
                .sessionHash(tokenService.hash(rawToken))
                .expiresAt(now.plus(properties.auth().sessionTtl()))
                .lastSeenAt(now)
                .createdAt(now)
                .build());
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
        return new SessionLoginResult(rawToken, authUserResultFactory.create(user));
    }

    @Transactional
    public void logout(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            return;
        }
        userSessionRepository.deleteBySessionHash(tokenService.hash(rawSessionToken));
    }

    @Transactional
    public AuthenticatedSession requireAuthenticatedSession(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }
        UserSessionEntity session = userSessionRepository.findBySessionHash(tokenService.hash(rawSessionToken))
                .orElseThrow(() -> new UnauthorizedException("Session is invalid"));
        if (session.getExpiresAt().isBefore(OffsetDateTime.now())) {
            userSessionRepository.delete(session);
            throw new UnauthorizedException("Session is expired");
        }
        UserEntity user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Session user does not exist"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User is not active");
        }
        session.setLastSeenAt(OffsetDateTime.now());
        userSessionRepository.save(session);
        return new AuthenticatedSession(rawSessionToken, new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole()));
    }
}
