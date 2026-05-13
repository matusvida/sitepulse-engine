package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.AdminUserResult;
import com.sitepulse.engine.auth.application.AdminUserResultFactory;
import com.sitepulse.engine.auth.application.UserProjectAssignmentService;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAdminUsersQuery {

    private final UserAccountStore userAccountStore;
    private final UserProjectAssignmentService userProjectAssignmentService;
    private final AdminUserResultFactory adminUserResultFactory;

    @Transactional(readOnly = true)
    public List<AdminUserResult> list() {
        List<UserAccount> users = userAccountStore.findAll().stream()
                .sorted((left, right) -> left.email().compareToIgnoreCase(right.email()))
                .toList();
        Map<Integer, List<Integer>> projectMap = userProjectAssignmentService.groupProjectIds(users.stream().map(UserAccount::id).toList());
        return users.stream()
                .map(user -> adminUserResultFactory.create(user, projectMap.getOrDefault(user.id(), List.of()), null))
                .toList();
    }
}
