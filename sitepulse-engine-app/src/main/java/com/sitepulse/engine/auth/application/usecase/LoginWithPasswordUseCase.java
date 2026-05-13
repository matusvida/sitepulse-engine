package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.EmailAddressNormalizer;
import com.sitepulse.engine.auth.application.PasswordHasher;
import com.sitepulse.engine.auth.application.SessionLifecycleService;
import com.sitepulse.engine.auth.application.SessionLoginResult;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginWithPasswordUseCase {

    private final UserAccountStore userAccountStore;
    private final EmailAddressNormalizer emailAddressNormalizer;
    private final PasswordHasher passwordHasher;
    private final SessionLifecycleService sessionLifecycleService;

    @Transactional
    public SessionLoginResult login(String email, String password) {
        UserAccount user = userAccountStore.findByEmail(emailAddressNormalizer.normalize(email))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (user.status() != UserStatus.ACTIVE || !passwordHasher.matches(password, user.passwordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return sessionLifecycleService.createSession(user);
    }
}
