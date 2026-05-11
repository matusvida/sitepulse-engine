package com.sitepulse.engine.auth.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Integer> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);

    List<PasswordResetTokenEntity> findByUserIdAndUsedAtIsNull(Integer userId);
}
