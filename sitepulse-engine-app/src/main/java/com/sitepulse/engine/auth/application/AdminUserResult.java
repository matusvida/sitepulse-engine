package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.UserRole;
import com.sitepulse.engine.auth.domain.UserStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminUserResult(
        Integer id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        UserStatus status,
        List<Integer> projectIds,
        OffsetDateTime lastLoginAt,
        String invitationPreviewUrl
) {
}
