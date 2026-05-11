package com.sitepulse.engine.auth.web;

import com.sitepulse.engine.auth.application.AdminUserResult;
import com.sitepulse.engine.auth.application.AuthenticatedUserAccessor;
import com.sitepulse.engine.auth.domain.UserRole;
import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.application.usecase.CreateAdminUserUseCase;
import com.sitepulse.engine.auth.application.usecase.ListAdminUsersQuery;
import com.sitepulse.engine.auth.application.usecase.ResendAdminInvitationUseCase;
import com.sitepulse.engine.auth.application.usecase.SetAdminUserEnabledUseCase;
import com.sitepulse.engine.auth.application.usecase.SetAdminUserProjectsUseCase;
import com.sitepulse.engine.auth.application.usecase.UpdateAdminUserUseCase;
import com.sitepulse.engine.http.admin.api.AdminUserApi;
import com.sitepulse.engine.http.admin.dto.AdminUserCreateRequest;
import com.sitepulse.engine.http.admin.dto.AdminUserProjectsRequest;
import com.sitepulse.engine.http.admin.dto.AdminUserUpdateRequest;
import com.sitepulse.engine.http.admin.dto.AdminUserView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminUserController implements AdminUserApi {

    private final ListAdminUsersQuery listAdminUsersQuery;
    private final CreateAdminUserUseCase createAdminUserUseCase;
    private final UpdateAdminUserUseCase updateAdminUserUseCase;
    private final ResendAdminInvitationUseCase resendAdminInvitationUseCase;
    private final SetAdminUserEnabledUseCase setAdminUserEnabledUseCase;
    private final SetAdminUserProjectsUseCase setAdminUserProjectsUseCase;
    private final AuthenticatedUserAccessor authenticatedUserAccessor;

    @Override
    public List<AdminUserView> listUsers() {
        return listAdminUsersQuery.list().stream().map(this::toView).toList();
    }

    @Override
    public AdminUserView createUser(AdminUserCreateRequest request) {
        return toView(createAdminUserUseCase.create(
                request.getEmail(),
                request.getFirstName(),
                request.getLastName(),
                UserRole.valueOf(request.getRole().trim().toUpperCase()),
                request.getProjectIds(),
                authenticatedUserAccessor.requireCurrentUser().id()
        ));
    }

    @Override
    public AdminUserView updateUser(Integer userId, AdminUserUpdateRequest request) {
        return toView(updateAdminUserUseCase.update(
                userId,
                UserRole.valueOf(request.getRole().trim().toUpperCase()),
                UserStatus.valueOf(request.getStatus().trim().toUpperCase())
        ));
    }

    @Override
    public AdminUserView resendInvitation(Integer userId) {
        return toView(resendAdminInvitationUseCase.resend(userId, authenticatedUserAccessor.requireCurrentUser().id()));
    }

    @Override
    public AdminUserView disableUser(Integer userId) {
        return toView(setAdminUserEnabledUseCase.setEnabled(userId, false));
    }

    @Override
    public AdminUserView enableUser(Integer userId) {
        return toView(setAdminUserEnabledUseCase.setEnabled(userId, true));
    }

    @Override
    public AdminUserView setProjects(Integer userId, AdminUserProjectsRequest request) {
        return toView(setAdminUserProjectsUseCase.replace(userId, request.getProjectIds()));
    }

    private AdminUserView toView(AdminUserResult result) {
        return new AdminUserView(
                result.id(),
                result.email(),
                result.firstName(),
                result.lastName(),
                result.role().name(),
                result.status().name(),
                result.projectIds(),
                result.lastLoginAt() == null ? null : result.lastLoginAt().toString(),
                result.invitationPreviewUrl()
        );
    }
}
