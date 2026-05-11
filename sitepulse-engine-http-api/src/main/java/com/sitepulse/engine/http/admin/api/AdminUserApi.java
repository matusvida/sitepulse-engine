package com.sitepulse.engine.http.admin.api;

import com.sitepulse.engine.http.admin.dto.AdminUserCreateRequest;
import com.sitepulse.engine.http.admin.dto.AdminUserProjectsRequest;
import com.sitepulse.engine.http.admin.dto.AdminUserUpdateRequest;
import com.sitepulse.engine.http.admin.dto.AdminUserView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin Users")
@RequestMapping("/api/admin/users")
public interface AdminUserApi {

    @Operation(summary = "List users")
    @GetMapping
    List<AdminUserView> listUsers();

    @Operation(summary = "Create invited user")
    @PostMapping
    AdminUserView createUser(@Valid @RequestBody AdminUserCreateRequest request);

    @Operation(summary = "Update user role or status")
    @PatchMapping("/{userId}")
    AdminUserView updateUser(@PathVariable Integer userId, @Valid @RequestBody AdminUserUpdateRequest request);

    @Operation(summary = "Resend invitation")
    @PostMapping("/{userId}/resend-invite")
    AdminUserView resendInvitation(@PathVariable Integer userId);

    @Operation(summary = "Disable user")
    @PostMapping("/{userId}/disable")
    AdminUserView disableUser(@PathVariable Integer userId);

    @Operation(summary = "Enable user")
    @PostMapping("/{userId}/enable")
    AdminUserView enableUser(@PathVariable Integer userId);

    @Operation(summary = "Replace user project assignments")
    @PutMapping("/{userId}/projects")
    AdminUserView setProjects(@PathVariable Integer userId, @Valid @RequestBody AdminUserProjectsRequest request);
}
