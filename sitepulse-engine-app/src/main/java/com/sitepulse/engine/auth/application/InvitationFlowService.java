package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.domain.model.InvitationToken;
import com.sitepulse.engine.auth.domain.model.RawToken;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.InvitationTokenStore;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
import com.sitepulse.engine.auth.exception.InvalidTokenException;
import com.sitepulse.engine.config.SitePulseProperties;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationFlowService {

    private final InvitationTokenStore invitationTokenStore;
    private final UserAccountStore userAccountStore;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final SitePulseProperties properties;
    private final AuthMailer authMailer;

    @Transactional
    public String createInvitationForUser(UserAccount user, Integer createdBy) {
        expireInvitationTokens(user.id());
        OffsetDateTime now = OffsetDateTime.now();
        RawToken rawToken = tokenService.generateOpaqueToken();
        invitationTokenStore.save(InvitationToken.create(
                user.id(),
                tokenService.hash(rawToken),
                now.plus(properties.auth().invitationTtl()),
                createdBy,
                now
        ));
        String invitationUrl = properties.auth().frontendBaseUrl() + "/invite?token=" + rawToken.value();
        authMailer.sendInvitation(user.asMailRecipient(), invitationUrl);
        return invitationUrl;
    }

    @Transactional
    public UserAccount consumeInvitation(String token, String firstName, String lastName, String password) {
        OffsetDateTime now = OffsetDateTime.now();
        InvitationToken invitationToken = invitationTokenStore.findByTokenHash(tokenService.hash(token))
                .orElseThrow(() -> new InvalidTokenException("Invitation link is invalid"));
        if (invitationToken.isUnavailableAt(now)) {
            throw new InvalidTokenException("Invitation link is expired or already used");
        }
        UserAccount user = userAccountStore.findById(invitationToken.userId())
                .orElseThrow(() -> new InvalidTokenException("Invitation user does not exist"));
        UserAccount updatedUser = user.acceptInvitation(
                resolveName(firstName, user.firstName()),
                resolveName(lastName, user.lastName()),
                passwordHasher.hash(password),
                now
        );
        invitationTokenStore.save(invitationToken.markUsed(now));
        return userAccountStore.save(updatedUser);
    }

    private void expireInvitationTokens(Integer userId) {
        OffsetDateTime now = OffsetDateTime.now();
        invitationTokenStore.findActiveByUserId(userId)
                .forEach(token -> invitationTokenStore.save(token.markUsed(now)));
    }

    private String resolveName(String incomingValue, String existingValue) {
        String normalizedIncoming = normalizeName(incomingValue);
        return normalizedIncoming == null || normalizedIncoming.isBlank() ? existingValue : normalizedIncoming;
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }
}
