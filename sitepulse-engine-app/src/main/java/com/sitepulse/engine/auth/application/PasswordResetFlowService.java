package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.domain.model.PasswordResetToken;
import com.sitepulse.engine.auth.domain.model.RawToken;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.PasswordResetTokenStore;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
import com.sitepulse.engine.auth.exception.InvalidTokenException;
import com.sitepulse.engine.config.SitePulseProperties;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetFlowService {

    private final UserAccountStore userAccountStore;
    private final PasswordResetTokenStore passwordResetTokenStore;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final SitePulseProperties properties;
    private final AuthMailer authMailer;
    private final EmailAddressNormalizer emailAddressNormalizer;

    @Transactional
    public void sendPasswordReset(String email) {
        userAccountStore.findByEmail(emailAddressNormalizer.normalize(email)).ifPresent(user -> {
            if (user.status() == UserStatus.DISABLED) {
                return;
            }
            expirePasswordResetTokens(user.id());
            OffsetDateTime now = OffsetDateTime.now();
            RawToken rawToken = tokenService.generateOpaqueToken();
            passwordResetTokenStore.save(PasswordResetToken.create(
                    user.id(),
                    tokenService.hash(rawToken),
                    now.plus(properties.auth().passwordResetTtl()),
                    now
            ));
            authMailer.sendPasswordReset(user.asMailRecipient(), properties.auth().frontendBaseUrl() + "/reset-password?token=" + rawToken.value());
        });
    }

    @Transactional
    public void resetPassword(String token, String password) {
        OffsetDateTime now = OffsetDateTime.now();
        PasswordResetToken resetToken = passwordResetTokenStore.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new InvalidTokenException("Password reset link is invalid"));
        if (resetToken.isUnavailableAt(now)) {
            throw new InvalidTokenException("Password reset link is expired or already used");
        }
        UserAccount user = userAccountStore.findById(resetToken.userId())
                .orElseThrow(() -> new InvalidTokenException("Password reset user does not exist"));
        if (user.status() == UserStatus.DISABLED) {
            throw new InvalidTokenException("User account is disabled");
        }
        userAccountStore.save(user.resetPassword(passwordHasher.hash(password), now));
        passwordResetTokenStore.save(resetToken.markUsed(now));
    }

    private void expirePasswordResetTokens(Integer userId) {
        OffsetDateTime now = OffsetDateTime.now();
        passwordResetTokenStore.findActiveByUserId(userId)
                .forEach(token -> passwordResetTokenStore.save(token.markUsed(now)));
    }
}
