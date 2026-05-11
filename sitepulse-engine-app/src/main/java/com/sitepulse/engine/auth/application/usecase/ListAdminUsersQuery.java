package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.AdminUserResult;
import com.sitepulse.engine.auth.application.AdminUserResultFactory;
import com.sitepulse.engine.auth.application.UserProjectAssignmentService;
import com.sitepulse.engine.auth.infrastructure.persistence.UserEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListAdminUsersQuery {

    private final UserRepository userRepository;
    private final UserProjectAssignmentService userProjectAssignmentService;
    private final AdminUserResultFactory adminUserResultFactory;

    @Transactional(readOnly = true)
    public List<AdminUserResult> list() {
        List<UserEntity> users = userRepository.findAll().stream()
                .sorted((left, right) -> left.getEmail().compareToIgnoreCase(right.getEmail()))
                .toList();
        Map<Integer, List<Integer>> projectMap = userProjectAssignmentService.groupProjectIds(users.stream().map(UserEntity::getId).toList());
        return users.stream()
                .map(user -> adminUserResultFactory.create(user, projectMap.getOrDefault(user.getId(), List.of()), null))
                .toList();
    }
}
