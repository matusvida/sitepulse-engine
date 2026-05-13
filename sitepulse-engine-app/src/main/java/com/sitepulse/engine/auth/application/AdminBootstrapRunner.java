package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.UserRole;
import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
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
    private final UserAccountStore userAccountStore;
    private final PasswordHasher passwordHasher;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.auth().hasSeededAdmin()) {
            return;
        }
        var email = new com.sitepulse.engine.auth.domain.model.EmailAddress(properties.auth().initialAdminEmail().trim().toLowerCase());
        if (userAccountStore.findByEmail(email).isPresent()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        userAccountStore.save(UserAccount.seededAdmin(email.value(), passwordHasher.hash(properties.auth().initialAdminPassword()), now));
        log.info("Seeded initial admin user {}", email.value());
    }
}
