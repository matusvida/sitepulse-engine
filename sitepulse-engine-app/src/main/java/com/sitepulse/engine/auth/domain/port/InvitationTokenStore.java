package com.sitepulse.engine.auth.domain.port;

import com.sitepulse.engine.auth.domain.model.InvitationToken;
import com.sitepulse.engine.auth.domain.model.TokenHash;
import java.util.List;
import java.util.Optional;

public interface InvitationTokenStore {

    InvitationToken save(InvitationToken invitationToken);

    Optional<InvitationToken> findByTokenHash(TokenHash tokenHash);

    List<InvitationToken> findActiveByUserId(Integer userId);
}
