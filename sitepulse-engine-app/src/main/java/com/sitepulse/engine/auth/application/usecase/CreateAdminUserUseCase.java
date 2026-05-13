package com.sitepulse.engine.auth.application.usecase;

import com.sitepulse.engine.auth.application.AdminUserResult;
import com.sitepulse.engine.auth.application.AdminUserResultFactory;
import com.sitepulse.engine.auth.application.EmailAddressNormalizer;
import com.sitepulse.engine.auth.application.InvitationFlowService;
import com.sitepulse.engine.auth.application.UserProjectAssignmentService;
import com.sitepulse.engine.auth.domain.model.UserAccount;
import com.sitepulse.engine.auth.domain.port.UserAccountStore;
import com.sitepulse.engine.auth.domain.UserRole;
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

    private final UserAccountStore userAccountStore;
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
        String normalizedEmail = emailAddressNormalizer.normalize(email).value();
        if (userAccountStore.findByEmail(emailAddressNormalizer.normalize(email)).isPresent()) {
            throw new ValidationException("A user with this email already exists");
        }
        OffsetDateTime now = OffsetDateTime.now();
        UserAccount user = userAccountStore.save(UserAccount.invited(
                normalizedEmail,
                normalizeName(firstName),
                normalizeName(lastName),
                role,
                now
        ));
        userProjectAssignmentService.replaceProjectAssignments(user.id(), projectIds);
        String invitationPreviewUrl = invitationFlowService.createInvitationForUser(user, createdByUserId);
        return adminUserResultFactory.create(user, List.copyOf(projectIds), invitationPreviewUrl);
    }

    private String normalizeName(String value) {
        return value == null ? null : value.trim();
    }
}
