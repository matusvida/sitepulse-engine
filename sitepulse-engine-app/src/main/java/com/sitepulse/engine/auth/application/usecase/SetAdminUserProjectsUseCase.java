package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.AdminUserResult;
import com.sitepulse.engine.auth.application.AdminUserResultFactory;
import com.sitepulse.engine.auth.application.UserProjectAssignmentService;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetAdminUserProjectsUseCase {

    private final UserAccountStore userAccountStore;
    private final UserProjectAssignmentService userProjectAssignmentService;
    private final AdminUserResultFactory adminUserResultFactory;

    @Transactional
    public AdminUserResult replace(Integer userId, Collection<Integer> projectIds) {
        UserAccount user = userAccountStore.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        userProjectAssignmentService.replaceProjectAssignments(userId, projectIds);
        return adminUserResultFactory.create(user, List.copyOf(projectIds), null);
    }
}
