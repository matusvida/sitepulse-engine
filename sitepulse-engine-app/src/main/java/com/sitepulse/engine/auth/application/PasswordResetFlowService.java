package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.exception.InvalidTokenException;
import com.sitepulse.engine.auth.infrastructure.persistence.PasswordResetTokenEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.PasswordResetTokenRepository;
import com.sitepulse.engine.auth.infrastructure.persistence.UserEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserRepository;
import com.sitepulse.engine.config.SitePulseProperties;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetFlowService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final SitePulseProperties properties;
    private final AuthMailer authMailer;
    private final EmailAddressNormalizer emailAddressNormalizer;

    @Transactional
    public void sendPasswordReset(String email) {
        userRepository.findByEmailIgnoreCase(emailAddressNormalizer.normalize(email)).ifPresent(user -> {
            if (user.getStatus() == UserStatus.DISABLED) {
                return;
            }
            expirePasswordResetTokens(user.getId());
            String rawToken = tokenService.generateOpaqueToken();
            passwordResetTokenRepository.save(PasswordResetTokenEntity.builder()
                    .userId(user.getId())
                    .tokenHash(tokenService.hash(rawToken))
                    .expiresAt(OffsetDateTime.now().plus(properties.auth().passwordResetTtl()))
                    .createdAt(OffsetDateTime.now())
                    .build());
            authMailer.sendPasswordReset(user, properties.auth().frontendBaseUrl() + "/reset-password?token=" + rawToken);
        });
    }

    @Transactional
    public void resetPassword(String token, String password) {
        PasswordResetTokenEntity resetToken = passwordResetTokenRepository.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new InvalidTokenException("Password reset link is invalid"));
        if (resetToken.getUsedAt() != null || resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidTokenException("Password reset link is expired or already used");
        }
        UserEntity user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("Password reset user does not exist"));
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new InvalidTokenException("User account is disabled");
        }
        user.setPasswordHash(passwordHasher.hash(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(OffsetDateTime.now());
        resetToken.setUsedAt(OffsetDateTime.now());
        userRepository.save(user);
        passwordResetTokenRepository.save(resetToken);
    }

    private void expirePasswordResetTokens(Integer userId) {
        passwordResetTokenRepository.findByUserIdAndUsedAtIsNull(userId)
                .forEach(token -> token.setUsedAt(OffsetDateTime.now()));
    }
}
