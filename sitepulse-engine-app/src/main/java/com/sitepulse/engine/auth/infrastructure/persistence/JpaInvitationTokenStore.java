package com.sitepulse.engine.auth.infrastructure.persistence;

import com.sitepulse.engine.auth.domain.model.InvitationToken;
import com.sitepulse.engine.auth.domain.model.TokenHash;
import com.sitepulse.engine.auth.domain.port.InvitationTokenStore;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaInvitationTokenStore implements InvitationTokenStore {

    private final InvitationTokenRepository invitationTokenRepository;
    private final AuthPersistenceMapper authPersistenceMapper;

    @Override
    public InvitationToken save(InvitationToken invitationToken) {
        return authPersistenceMapper.toDomain(invitationTokenRepository.save(authPersistenceMapper.toEntity(invitationToken)));
    }

    @Override
    public Optional<InvitationToken> findByTokenHash(TokenHash tokenHash) {
        return invitationTokenRepository.findByTokenHash(tokenHash.value()).map(authPersistenceMapper::toDomain);
    }

    @Override
    public List<InvitationToken> findActiveByUserId(Integer userId) {
        return invitationTokenRepository.findByUserIdAndUsedAtIsNull(userId).stream().map(authPersistenceMapper::toDomain).toList();
    }
}
