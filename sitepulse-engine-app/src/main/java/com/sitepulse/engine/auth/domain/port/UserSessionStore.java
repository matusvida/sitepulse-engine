package com.sitepulse.engine.auth.domain.port;

import com.sitepulse.engine.auth.domain.model.TokenHash;
import com.sitepulse.engine.auth.domain.model.UserSession;
import java.util.Optional;

public interface UserSessionStore {

    UserSession save(UserSession userSession);

    Optional<UserSession> findBySessionHash(TokenHash sessionHash);

    void delete(UserSession userSession);

    void deleteBySessionHash(TokenHash sessionHash);
}
