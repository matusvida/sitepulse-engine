package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.model.UserAccount;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AdminUserResultFactory {

    public AdminUserResult create(UserAccount user, List<Integer> projectIds, String invitationPreviewUrl) {
        return new AdminUserResult(
                user.id(),
                user.email(),
                user.firstName(),
                user.lastName(),
                user.role(),
                user.status(),
                projectIds,
                user.lastLoginAt(),
                invitationPreviewUrl
        );
    }
}
