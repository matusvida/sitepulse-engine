package com.sitepulse.engine.auth.domain.port;

import com.sitepulse.engine.auth.domain.model.PasswordResetToken;
import com.sitepulse.engine.auth.domain.model.TokenHash;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenStore {

    PasswordResetToken save(PasswordResetToken passwordResetToken);

    Optional<PasswordResetToken> findByTokenHash(TokenHash tokenHash);

    List<PasswordResetToken> findActiveByUserId(Integer userId);
}
