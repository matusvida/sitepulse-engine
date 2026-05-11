package com.sitepulse.engine.http.auth.api;

import com.sitepulse.engine.http.auth.dto.AuthSessionView;
import com.sitepulse.engine.http.auth.dto.ConsumeInvitationRequest;
import com.sitepulse.engine.http.auth.dto.ForgotPasswordRequest;
import com.sitepulse.engine.http.auth.dto.LoginRequest;
import com.sitepulse.engine.http.auth.dto.ResetPasswordRequest;
import com.sitepulse.engine.http.common.dto.ActionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Authentication")
@RequestMapping("/api/auth")
public interface AuthApi {

    @Operation(summary = "Log in with email and password")
    @PostMapping("/login")
    ResponseEntity<AuthSessionView> login(@Valid @RequestBody LoginRequest request);

    @Operation(summary = "Log out current session")
    @PostMapping("/logout")
    ResponseEntity<ActionResponse> logout();

    @Operation(summary = "Get current authenticated user")
    @GetMapping("/me")
    AuthSessionView me();

    @Operation(summary = "Consume invitation token and set password")
    @PostMapping("/invitations/consume")
    ResponseEntity<AuthSessionView> consumeInvitation(@Valid @RequestBody ConsumeInvitationRequest request);

    @Operation(summary = "Request password reset")
    @PostMapping("/password/forgot")
    ActionResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request);

    @Operation(summary = "Reset password with token")
    @PostMapping("/password/reset")
    ActionResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request);
}
