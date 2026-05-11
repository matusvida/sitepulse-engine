package com.sitepulse.engine.auth.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSessionRepository extends JpaRepository<UserSessionEntity, Integer> {

    Optional<UserSessionEntity> findBySessionHash(String sessionHash);

    void deleteBySessionHash(String sessionHash);
}
