package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.AdminUserResult;
import com.sitepulse.engine.auth.application.AdminUserResultFactory;
import com.sitepulse.engine.auth.application.InvitationFlowService;
import com.sitepulse.engine.auth.application.UserProjectAssignmentService;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResendAdminInvitationUseCase {

    private final UserAccountStore userAccountStore;
    private final InvitationFlowService invitationFlowService;
    private final UserProjectAssignmentService userProjectAssignmentService;
    private final AdminUserResultFactory adminUserResultFactory;

    @Transactional
    public AdminUserResult resend(Integer userId, Integer createdByUserId) {
        UserAccount user = userAccountStore.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        String invitationPreviewUrl = invitationFlowService.createInvitationForUser(user, createdByUserId);
        return adminUserResultFactory.create(user, userProjectAssignmentService.listProjectIds(userId), invitationPreviewUrl);
    }
}
