package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.infrastructure.persistence.UserEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdminUserResultFactory {

    public AdminUserResult create(UserEntity user, List<Integer> projectIds, String invitationPreviewUrl) {
        return new AdminUserResult(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getStatus(),
                projectIds,
                user.getLastLoginAt(),
                invitationPreviewUrl
        );
    }
}
