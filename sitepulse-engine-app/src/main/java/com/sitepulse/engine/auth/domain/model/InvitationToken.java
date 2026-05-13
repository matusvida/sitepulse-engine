package com.sitepulse.engine.auth.domain.model;

import java.time.OffsetDateTime;

public record InvitationToken(
        Integer id,
        Integer userId,
        String tokenHash,
        OffsetDateTime expiresAt,
        OffsetDateTime usedAt,
        Integer createdBy,
        OffsetDateTime createdAt
) {

    public static InvitationToken create(Integer userId, TokenHash tokenHash, OffsetDateTime expiresAt, Integer createdBy, OffsetDateTime createdAt) {
        return new InvitationToken(null, userId, tokenHash.value(), expiresAt, null, createdBy, createdAt);
    }

    public boolean isUnavailableAt(OffsetDateTime now) {
        return usedAt != null || expiresAt.isBefore(now);
    }

    public InvitationToken markUsed(OffsetDateTime now) {
        return new InvitationToken(id, userId, tokenHash, expiresAt, now, createdBy, createdAt);
    }
}
