package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.exception.InvalidTokenException;
import com.sitepulse.engine.auth.infrastructure.persistence.InvitationTokenEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.InvitationTokenRepository;
import com.sitepulse.engine.auth.infrastructure.persistence.UserEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserRepository;
import com.sitepulse.engine.config.SitePulseProperties;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationFlowService {

    private final InvitationTokenRepository invitationTokenRepository;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final SitePulseProperties properties;
    private final AuthMailer authMailer;

    @Transactional
    public String createInvitationForUser(UserEntity user, Integer createdBy) {
        expireInvitationTokens(user.getId());
        String rawToken = tokenService.generateOpaqueToken();
        invitationTokenRepository.save(InvitationTokenEntity.builder()
                .userId(user.getId())
                .tokenHash(tokenService.hash(rawToken))
                .expiresAt(OffsetDateTime.now().plus(properties.auth().invitationTtl()))
                .createdBy(createdBy)
                .createdAt(OffsetDateTime.now())
                .build());
        String invitationUrl = properties.auth().frontendBaseUrl() + "/invite?token=" + rawToken;
        authMailer.sendInvitation(user, invitationUrl);
        return invitationUrl;
    }

    @Transactional
    public UserEntity consumeInvitation(String token, String firstName, String lastName, String password) {
        InvitationTokenEntity invitationToken = invitationTokenRepository.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new InvalidTokenException("Invitation link is invalid"));
        if (invitationToken.getUsedAt() != null || invitationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new InvalidTokenException("Invitation link is expired or already used");
        }
        UserEntity user = userRepository.findById(invitationToken.getUserId())
                .orElseThrow(() -> new InvalidTokenException("Invitation user does not exist"));
        user.setFirstName(resolveName(firstName, user.getFirstName()));
        user.setLastName(resolveName(lastName, user.getLastName()));
        user.setPasswordHash(passwordHasher.hash(password));
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(OffsetDateTime.now());
        invitationToken.setUsedAt(OffsetDateTime.now());
        invitationTokenRepository.save(invitationToken);
        return userRepository.save(user);
    }

    private void expireInvitationTokens(Integer userId) {
        invitationTokenRepository.findByUserIdAndUsedAtIsNull(userId)
                .forEach(token -> token.setUsedAt(OffsetDateTime.now()));
    }

    private String resolveName(String incomingValue, String existingValue) {
        String normalizedIncoming = normalizeName(incomingValue);
        return normalizedIncoming == null || normalizedIncoming.isBlank() ? existingValue : normalizedIncoming;
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }
}
