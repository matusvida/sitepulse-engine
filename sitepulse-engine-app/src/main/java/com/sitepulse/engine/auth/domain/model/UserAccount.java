package com.sitepulse.engine.auth.domain.model;

import com.sitepulse.engine.auth.domain.UserRole;
import com.sitepulse.engine.auth.domain.UserStatus;
import java.time.OffsetDateTime;

public record UserAccount(
        Integer id,
        String email,
        String firstName,
        String lastName,
        String passwordHash,
        UserRole role,
        UserStatus status,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    public static UserAccount invited(
            String email,
            String firstName,
            String lastName,
            UserRole role,
            OffsetDateTime now
    ) {
        return new UserAccount(null, email, firstName, lastName, null, role, UserStatus.INVITED, null, now, now);
    }

    public static UserAccount seededAdmin(String email, String passwordHash, OffsetDateTime now) {
        return new UserAccount(null, email, null, null, passwordHash, UserRole.ADMIN, UserStatus.ACTIVE, null, now, now);
    }

    public UserAccount acceptInvitation(String resolvedFirstName, String resolvedLastName, String encodedPassword, OffsetDateTime now) {
        return new UserAccount(id, email, resolvedFirstName, resolvedLastName, encodedPassword, role, UserStatus.ACTIVE, lastLoginAt, createdAt, now);
    }

    public UserAccount resetPassword(String encodedPassword, OffsetDateTime now) {
        return new UserAccount(id, email, firstName, lastName, encodedPassword, role, UserStatus.ACTIVE, lastLoginAt, createdAt, now);
    }

    public UserAccount withRoleAndStatus(UserRole nextRole, UserStatus nextStatus, OffsetDateTime now) {
        return new UserAccount(id, email, firstName, lastName, passwordHash, nextRole, nextStatus, lastLoginAt, createdAt, now);
    }

    public UserAccount withStatus(UserStatus nextStatus, OffsetDateTime now) {
        return new UserAccount(id, email, firstName, lastName, passwordHash, role, nextStatus, lastLoginAt, createdAt, now);
    }

    public UserAccount recordLogin(OffsetDateTime now) {
        return new UserAccount(id, email, firstName, lastName, passwordHash, role, status, now, createdAt, now);
    }

    public AuthMailRecipient asMailRecipient() {
        return new AuthMailRecipient(email, firstName, lastName);
    }
}
