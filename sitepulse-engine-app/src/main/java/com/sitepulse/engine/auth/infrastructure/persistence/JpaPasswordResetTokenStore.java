package com.sitepulse.engine.auth.infrastructure.persistence;

import com.sitepulse.engine.auth.domain.model.PasswordResetToken;
import com.sitepulse.engine.auth.domain.model.TokenHash;
import com.sitepulse.engine.auth.domain.port.PasswordResetTokenStore;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaPasswordResetTokenStore implements PasswordResetTokenStore {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthPersistenceMapper authPersistenceMapper;

    @Override
    public PasswordResetToken save(PasswordResetToken passwordResetToken) {
        return authPersistenceMapper.toDomain(passwordResetTokenRepository.save(authPersistenceMapper.toEntity(passwordResetToken)));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(TokenHash tokenHash) {
        return passwordResetTokenRepository.findByTokenHash(tokenHash.value()).map(authPersistenceMapper::toDomain);
    }

    @Override
    public List<PasswordResetToken> findActiveByUserId(Integer userId) {
        return passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(userId).stream().map(authPersistenceMapper::toDomain).toList();
    }
}
