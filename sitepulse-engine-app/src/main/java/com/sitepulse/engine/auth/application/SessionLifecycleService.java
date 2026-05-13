package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.domain.model.TokenHash;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.model.UserSession;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
import com.sitepulse.engine.auth.domain.port.UserSessionStore;
import com.sitepulse.engine.auth.exception.UnauthorizedException;
import com.sitepulse.engine.config.SitePulseProperties;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionLifecycleService {

    private final UserAccountStore userAccountStore;
    private final UserSessionStore userSessionStore;
    private final TokenService tokenService;
    private final SitePulseProperties properties;
    private final AuthUserResultFactory authUserResultFactory;

    @Transactional
    public SessionLoginResult createSession(UserAccount user) {
        var rawToken = tokenService.generateOpaqueToken();
        OffsetDateTime now = OffsetDateTime.now();
        userSessionStore.save(UserSession.create(user.id(), tokenService.hash(rawToken), now.plus(properties.auth().sessionTtl()), now));
        UserAccount updatedUser = userAccountStore.save(user.recordLogin(now));
        return new SessionLoginResult(rawToken.value(), authUserResultFactory.create(updatedUser));
    }

    @Transactional
    public void logout(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            return;
        }
        userSessionStore.deleteBySessionHash(tokenService.hash(rawSessionToken));
    }

    @Transactional
    public AuthenticatedSession requireAuthenticatedSession(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            throw new UnauthorizedException("Authentication is required");
        }
        UserSession session = userSessionStore.findBySessionHash(tokenService.hash(rawSessionToken))
                .orElseThrow(() -> new UnauthorizedException("Session is invalid"));
        OffsetDateTime now = OffsetDateTime.now();
        if (session.isExpiredAt(now)) {
            userSessionStore.delete(session);
            throw new UnauthorizedException("Session is expired");
        }
        UserAccount user = userAccountStore.findById(session.userId())
                .orElseThrow(() -> new UnauthorizedException("Session user does not exist"));
        if (user.status() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User is not active");
        }
        userSessionStore.save(session.touch(now));
        return new AuthenticatedSession(rawSessionToken, new AuthenticatedUser(user.id(), user.email(), user.role()));
    }
}
