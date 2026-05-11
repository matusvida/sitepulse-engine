package com.sitepulse.engine.auth.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitationTokenRepository extends JpaRepository<InvitationTokenEntity, Integer> {

    Optional<InvitationTokenEntity> findByTokenHash(String tokenHash);

    List<InvitationTokenEntity> findByUserIdAndUsedAtIsNull(Integer userId);
}
