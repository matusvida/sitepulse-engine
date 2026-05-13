package com.sitepulse.engine.auth.domain.model;

import java.time.OffsetDateTime;

public record UserSession(
        Integer id,
        Integer userId,
        String sessionHash,
        OffsetDateTime expiresAt,
        OffsetDateTime lastSeenAt,
        OffsetDateTime createdAt
) {

    public static UserSession create(Integer userId, TokenHash sessionHash, OffsetDateTime expiresAt, OffsetDateTime createdAt) {
        return new UserSession(null, userId, sessionHash.value(), expiresAt, createdAt, createdAt);
    }

    public boolean isExpiredAt(OffsetDateTime now) {
        return expiresAt.isBefore(now);
    }

    public UserSession touch(OffsetDateTime now) {
        return new UserSession(id, userId, sessionHash, expiresAt, now, createdAt);
    }
}
