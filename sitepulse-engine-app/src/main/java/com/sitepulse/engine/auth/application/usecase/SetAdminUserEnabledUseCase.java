package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.AdminUserResult;
import com.sitepulse.engine.auth.application.AdminUserResultFactory;
import com.sitepulse.engine.auth.application.UserProjectAssignmentService;
import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.infrastructure.persistence.UserEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserRepository;
import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SetAdminUserEnabledUseCase {

    private final UserRepository userRepository;
    private final UserProjectAssignmentService userProjectAssignmentService;
    private final AdminUserResultFactory adminUserResultFactory;

    @Transactional
    public AdminUserResult setEnabled(Integer userId, boolean enabled) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        user.setStatus(enabled ? UserStatus.ACTIVE : UserStatus.DISABLED);
        user.setUpdatedAt(OffsetDateTime.now());
        user = userRepository.save(user);
        return adminUserResultFactory.create(user, userProjectAssignmentService.listProjectIds(userId), null);
    }
}
