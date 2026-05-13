package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.AdminUserResult;
import com.sitepulse.engine.auth.application.AdminUserResultFactory;
import com.sitepulse.engine.auth.application.UserProjectAssignmentService;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetAdminUserEnabledUseCase {

    private final UserAccountStore userAccountStore;
    private final UserProjectAssignmentService userProjectAssignmentService;
    private final AdminUserResultFactory adminUserResultFactory;

    @Transactional
    public AdminUserResult setEnabled(Integer userId, boolean enabled) {
        UserAccount user = userAccountStore.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user = userAccountStore.save(user.withStatus(enabled ? UserStatus.ACTIVE : UserStatus.DISABLED, OffsetDateTime.now()));
        return adminUserResultFactory.create(user, userProjectAssignmentService.listProjectIds(userId), null);
    }
}
