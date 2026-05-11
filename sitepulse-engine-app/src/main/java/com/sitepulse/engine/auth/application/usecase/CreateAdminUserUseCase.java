package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.AdminUserResult;
import com.sitepulse.engine.auth.application.AdminUserResultFactory;
import com.sitepulse.engine.auth.application.EmailAddressNormalizer;
import com.sitepulse.engine.auth.application.InvitationFlowService;
import com.sitepulse.engine.auth.application.UserProjectAssignmentService;
import com.sitepulse.engine.auth.domain.UserRole;
import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.infrastructure.persistence.UserEntity;
import com.sitepulse.engine.auth.infrastructure.persistence.UserRepository;
import com.sitepulse.engine.common.exception.ValidationException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateAdminUserUseCase {

    private final UserRepository userRepository;
    private final EmailAddressNormalizer emailAddressNormalizer;
    private final UserProjectAssignmentService userProjectAssignmentService;
    private final InvitationFlowService invitationFlowService;
    private final AdminUserResultFactory adminUserResultFactory;

    @Transactional
    public AdminUserResult create(
            String email,
            String firstName,
            String lastName,
            UserRole role,
            Collection<Integer> projectIds,
            Integer createdByUserId
    ) {
        String normalizedEmail = emailAddressNormalizer.normalize(email);
        if (userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new ValidationException("A user with this email already exists");
        }
        OffsetDateTime now = OffsetDateTime.now();
        UserEntity user = userRepository.save(UserEntity.builder()
                .email(normalizedEmail)
                .firstName(normalizeName(firstName))
                .lastName(normalizeName(lastName))
                .role(role)
                .status(UserStatus.INVITED)
                .createdAt(now)
                .updatedAt(now)
                .build());
        userProjectAssignmentService.replaceProjectAssignments(user.getId(), projectIds);
        String invitationPreviewUrl = invitationFlowService.createInvitationForUser(user, createdByUserId);
        return adminUserResultFactory.create(user, List.copyOf(projectIds), invitationPreviewUrl);
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }
}
