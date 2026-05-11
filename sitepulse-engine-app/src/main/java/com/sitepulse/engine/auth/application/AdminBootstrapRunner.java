package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.UserRole;
import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.infrastructure.persistence.UserEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserRepository;
import com.sitepulse.engine.config.SitePulseProperties;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private final SitePulseProperties properties;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.auth().hasSeededAdmin()) {
            return;
        }
        String email = properties.auth().initialAdminEmail().trim().toLowerCase();
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            return;
        }
        userRepository.save(UserEntity.builder()
                .email(email)
                .passwordHash(passwordHasher.hash(properties.auth().initialAdminPassword()))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .lastLoginAt(null)
                .build());
        log.info("Seeded initial admin user {}", email);
    }
}
