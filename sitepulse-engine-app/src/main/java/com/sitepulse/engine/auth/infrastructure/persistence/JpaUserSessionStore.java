package com.sitepulse.engine.auth.infrastructure.persistence;

import com.sitepulse.engine.auth.domain.model.TokenHash;
import com.sitepulse.engine.auth.domain.model.UserSession;
import com.sitepulse.engine.auth.domain.port.UserSessionStore;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaUserSessionStore implements UserSessionStore {

    private final UserSessionRepository userSessionRepository;
    private final AuthPersistenceMapper authPersistenceMapper;

    @Override
    public UserSession save(UserSession userSession) {
        return authPersistenceMapper.toDomain(userSessionRepository.save(authPersistenceMapper.toEntity(userSession)));
    }

    @Override
    public Optional<UserSession> findBySessionHash(TokenHash sessionHash) {
        return userSessionRepository.findBySessionHash(sessionHash.value()).map(authPersistenceMapper::toDomain);
    }

    @Override
    public void delete(UserSession userSession) {
        userSessionRepository.delete(authPersistenceMapper.toEntity(userSession));
    }

    @Override
    public void deleteBySessionHash(TokenHash sessionHash) {
        userSessionRepository.deleteBySessionHash(sessionHash.value());
    }
}
