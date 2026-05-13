package com.sitepulse.engine.auth.infrastructure.persistence;

import com.sitepulse.engine.auth.domain.model.InvitationToken;
import com.sitepulse.engine.auth.domain.model.PasswordResetToken;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.model.UserSession;
import org.springframework.stereotype.Component;

@Component
public class AuthPersistenceMapper {

    public UserAccount toDomain(UserEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getStatus(),
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserEntity toEntity(UserAccount userAccount) {
        return UserEntity.builder()
                .id(userAccount.id())
                .email(userAccount.email())
                .firstName(userAccount.firstName())
                .lastName(userAccount.lastName())
                .passwordHash(userAccount.passwordHash())
                .role(userAccount.role())
                .status(userAccount.status())
                .lastLoginAt(userAccount.lastLoginAt())
                .createdAt(userAccount.createdAt())
                .updatedAt(userAccount.updatedAt())
                .build();
    }

    public InvitationToken toDomain(InvitationTokenEntity entity) {
        return new InvitationToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }

    public InvitationTokenEntity toEntity(InvitationToken invitationToken) {
        return InvitationTokenEntity.builder()
                .id(invitationToken.id())
                .userId(invitationToken.userId())
                .tokenHash(invitationToken.tokenHash())
                .expiresAt(invitationToken.expiresAt())
                .usedAt(invitationToken.usedAt())
                .createdBy(invitationToken.createdBy())
                .createdAt(invitationToken.createdAt())
                .build();
    }

    public PasswordResetToken toDomain(PasswordResetToken entity) {
        return entity;
    }

    public PasswordResetToken toDomain(PasswordResetTokenEntity entity) {
        return new PasswordResetToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getCreatedAt()
        );
    }

    public PasswordResetTokenEntity toEntity(PasswordResetToken passwordResetToken) {
        return PasswordResetTokenEntity.builder()
                .id(passwordResetToken.id())
                .userId(passwordResetToken.userId())
                .tokenHash(passwordResetToken.tokenHash())
                .expiresAt(passwordResetToken.expiresAt())
                .usedAt(passwordResetToken.usedAt())
                .createdAt(passwordResetToken.createdAt())
                .build();
    }

    public UserSession toDomain(UserSessionEntity entity) {
        return new UserSession(
                entity.getId(),
                entity.getUserId(),
                entity.getSessionHash(),
                entity.getExpiresAt(),
                entity.getLastSeenAt(),
                entity.getCreatedAt()
        );
    }

    public UserSessionEntity toEntity(UserSession userSession) {
        return UserSessionEntity.builder()
                .id(userSession.id())
                .userId(userSession.userId())
                .sessionHash(userSession.sessionHash())
                .expiresAt(userSession.expiresAt())
                .lastSeenAt(userSession.lastSeenAt())
                .createdAt(userSession.createdAt())
                .build();
    }
}
