package com.sitepulse.engine.auth.domain.model;

import java.time.OffsetDateTime;

public record PasswordResetToken(
        Integer id,
        Integer userId,
        String tokenHash,
        OffsetDateTime expiresAt,
        OffsetDateTime usedAt,
        OffsetDateTime createdAt
) {

    public static PasswordResetToken create(Integer userId, TokenHash tokenHash, OffsetDateTime expiresAt, OffsetDateTime createdAt) {
        return new PasswordResetToken(null, userId, tokenHash.value(), expiresAt, null, createdAt);
    }

    public boolean isUnavailableAt(OffsetDateTime now) {
        return usedAt != null || expiresAt.isBefore(now);
    }

    public PasswordResetToken markUsed(OffsetDateTime now) {
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, now, createdAt);
    }
}
