package com.sitepulse.engine.auth.web;

import com.sitepulse.engine.auth.application.AuthCookieService;
import com.sitepulse.engine.auth.application.AuthUserResult;
import com.sitepulse.engine.auth.application.AuthenticatedUserAccessor;
import com.sitepulse.engine.auth.application.SessionLoginResult;
import com.sitepulse.engine.auth.application.usecase.ConsumeInvitationUseCase;
import com.sitepulse.engine.auth.application.usecase.GetCurrentAuthSessionQuery;
import com.sitepulse.engine.auth.application.usecase.LoginWithPasswordUseCase;
import com.sitepulse.engine.auth.application.usecase.LogoutSessionUseCase;
import com.sitepulse.engine.auth.application.usecase.RequestPasswordResetUseCase;
import com.sitepulse.engine.auth.application.usecase.ResetPasswordUseCase;
import com.sitepulse.engine.http.auth.api.AuthApi;
import com.sitepulse.engine.http.auth.dto.AuthSessionView;
import com.sitepulse.engine.http.auth.dto.AuthUserView;
import com.sitepulse.engine.http.auth.dto.ConsumeInvitationRequest;
import com.sitepulse.engine.http.auth.dto.ForgotPasswordRequest;
import com.sitepulse.engine.http.auth.dto.LoginRequest;
import com.sitepulse.engine.http.auth.dto.ResetPasswordRequest;
import com.sitepulse.engine.http.common.dto.ActionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final LoginWithPasswordUseCase loginWithPasswordUseCase;
    private final ConsumeInvitationUseCase consumeInvitationUseCase;
    private final GetCurrentAuthSessionQuery getCurrentAuthSessionQuery;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final LogoutSessionUseCase logoutSessionUseCase;
    private final AuthCookieService authCookieService;
    private final AuthenticatedUserAccessor authenticatedUserAccessor;

    @Override
    public ResponseEntity<AuthSessionView> login(LoginRequest request) {
        SessionLoginResult loginResult = loginWithPasswordUseCase.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createSessionCookie(loginResult.sessionToken()))
                .header("Cache-Control", "no-store")
                .body(toSessionView(loginResult));
    }

    @Override
    public ResponseEntity<ActionResponse> logout() {
        logoutSessionUseCase.logout(authenticatedUserAccessor.requireCurrentSessionToken());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearSessionCookie())
                .body(new ActionResponse("ok", "Logged out", null));
    }

    @Override
    public AuthSessionView me() {
        return new AuthSessionView(toUserView(getCurrentAuthSessionQuery.get(authenticatedUserAccessor.requireCurrentUser())));
    }

    @Override
    public ResponseEntity<AuthSessionView> consumeInvitation(ConsumeInvitationRequest request) {
        SessionLoginResult loginResult = consumeInvitationUseCase.consume(
                request.getToken(),
                request.getFirstName(),
                request.getLastName(),
                request.getPassword()
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookieService.createSessionCookie(loginResult.sessionToken()))
                .header("Cache-Control", "no-store")
                .body(toSessionView(loginResult));
    }

    @Override
    public ActionResponse forgotPassword(ForgotPasswordRequest request) {
        requestPasswordResetUseCase.request(request.getEmail());
        return new ActionResponse("ok", "If the email exists, a reset link has been sent", null);
    }

    @Override
    public ActionResponse resetPassword(ResetPasswordRequest request) {
        resetPasswordUseCase.reset(request.getToken(), request.getPassword());
        return new ActionResponse("ok", "Password has been reset", null);
    }

    private AuthSessionView toSessionView(SessionLoginResult loginResult) {
        return new AuthSessionView(toUserView(loginResult.user()));
    }

    private AuthUserView toUserView(AuthUserResult result) {
        return new AuthUserView(
                result.id(),
                result.email(),
                result.firstName(),
                result.lastName(),
                result.role().name(),
                result.status().name(),
                result.projectIds()
        );
    }
}
