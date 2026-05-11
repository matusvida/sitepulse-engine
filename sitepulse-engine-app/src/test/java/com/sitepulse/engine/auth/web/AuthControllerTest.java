package com.sitepulse.engine.auth.web;

import com.sitepulse.engine.auth.application.AuthCookieService;
import com.sitepulse.engine.auth.application.AuthUserResult;
import com.sitepulse.engine.auth.application.AuthenticatedUser;
import com.sitepulse.engine.auth.application.AuthenticatedUserAccessor;
import com.sitepulse.engine.auth.application.SessionLoginResult;
import com.sitepulse.engine.auth.application.usecase.ConsumeInvitationUseCase;
import com.sitepulse.engine.auth.application.usecase.GetCurrentAuthSessionQuery;
import com.sitepulse.engine.auth.application.usecase.LoginWithPasswordUseCase;
import com.sitepulse.engine.auth.application.usecase.LogoutSessionUseCase;
import com.sitepulse.engine.auth.application.usecase.RequestPasswordResetUseCase;
import com.sitepulse.engine.auth.application.usecase.ResetPasswordUseCase;
import com.sitepulse.engine.auth.domain.UserRole;
import com.sitepulse.engine.auth.domain.UserStatus;
import com.sitepulse.engine.auth.infrastructure.security.SessionPrincipal;
import com.sitepulse.engine.http.auth.dto.ConsumeInvitationRequest;
import com.sitepulse.engine.http.auth.dto.ForgotPasswordRequest;
import com.sitepulse.engine.http.auth.dto.LoginRequest;
import com.sitepulse.engine.http.auth.dto.ResetPasswordRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginReturnsSessionCookieAndUserPayload() {
        StubLoginWithPasswordUseCase login = new StubLoginWithPasswordUseCase();
        login.result = new SessionLoginResult("session-token", authUser());
        AuthController controller = new AuthController(
                login,
                new StubConsumeInvitationUseCase(),
                new StubGetCurrentAuthSessionQuery(),
                new StubRequestPasswordResetUseCase(),
                new StubResetPasswordUseCase(),
                new StubLogoutSessionUseCase(),
                new AuthCookieService(new AuthSecurityTestConfig().sitePulseProperties()),
                new AuthenticatedUserAccessor()
        );

        var response = controller.login(new LoginRequest("planner@example.com", "secret123"));

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("sitepulse_session=session-token"));
        assertEquals("planner@example.com", response.getBody().getUser().getEmail());
        assertEquals("Paula", response.getBody().getUser().getFirstName());
        assertEquals("Planner", response.getBody().getUser().getLastName());
        assertEquals("planner@example.com", login.email);
    }

    @Test
    void consumeInvitationReturnsSessionCookieAndUserPayload() {
        StubConsumeInvitationUseCase consume = new StubConsumeInvitationUseCase();
        consume.result = new SessionLoginResult("invite-session", authUser());
        AuthController controller = new AuthController(
                new StubLoginWithPasswordUseCase(),
                consume,
                new StubGetCurrentAuthSessionQuery(),
                new StubRequestPasswordResetUseCase(),
                new StubResetPasswordUseCase(),
                new StubLogoutSessionUseCase(),
                new AuthCookieService(new AuthSecurityTestConfig().sitePulseProperties()),
                new AuthenticatedUserAccessor()
        );

        var response = controller.consumeInvitation(new ConsumeInvitationRequest("invite-token", "Paula", "Planner", "secret123"));

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("sitepulse_session=invite-session"));
        assertEquals("invite-token", consume.token);
        assertEquals("Paula", consume.firstName);
        assertEquals("Planner", consume.lastName);
    }

    @Test
    void logoutClearsSessionCookieAndDelegatesToUseCase() {
        StubLogoutSessionUseCase logout = new StubLogoutSessionUseCase();
        AuthController controller = new AuthController(
                new StubLoginWithPasswordUseCase(),
                new StubConsumeInvitationUseCase(),
                new StubGetCurrentAuthSessionQuery(),
                new StubRequestPasswordResetUseCase(),
                new StubResetPasswordUseCase(),
                logout,
                new AuthCookieService(new AuthSecurityTestConfig().sitePulseProperties()),
                new AuthenticatedUserAccessor()
        );
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new SessionPrincipal("session-token", new AuthenticatedUser(7, "planner@example.com", UserRole.USER)),
                "session-token",
                List.of()
        ));

        var response = controller.logout();

        assertEquals("session-token", logout.sessionToken);
        assertTrue(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE).contains("Max-Age=0"));
    }

    @Test
    void forgotPasswordDelegates() {
        StubRequestPasswordResetUseCase forgot = new StubRequestPasswordResetUseCase();
        AuthController controller = new AuthController(
                new StubLoginWithPasswordUseCase(),
                new StubConsumeInvitationUseCase(),
                new StubGetCurrentAuthSessionQuery(),
                forgot,
                new StubResetPasswordUseCase(),
                new StubLogoutSessionUseCase(),
                new AuthCookieService(new AuthSecurityTestConfig().sitePulseProperties()),
                new AuthenticatedUserAccessor()
        );

        var response = controller.forgotPassword(new ForgotPasswordRequest("planner@example.com"));

        assertEquals("planner@example.com", forgot.email);
        assertEquals("ok", response.getStatus());
    }

    @Test
    void resetPasswordDelegates() {
        StubResetPasswordUseCase reset = new StubResetPasswordUseCase();
        AuthController controller = new AuthController(
                new StubLoginWithPasswordUseCase(),
                new StubConsumeInvitationUseCase(),
                new StubGetCurrentAuthSessionQuery(),
                new StubRequestPasswordResetUseCase(),
                reset,
                new StubLogoutSessionUseCase(),
                new AuthCookieService(new AuthSecurityTestConfig().sitePulseProperties()),
                new AuthenticatedUserAccessor()
        );

        var response = controller.resetPassword(new ResetPasswordRequest("reset-token", "secret123"));

        assertEquals("reset-token", reset.token);
        assertEquals("secret123", reset.password);
        assertEquals("ok", response.getStatus());
    }

    @Test
    void meReturnsAuthenticatedUserView() {
        StubGetCurrentAuthSessionQuery me = new StubGetCurrentAuthSessionQuery();
        me.result = authUser();
        AuthController controller = new AuthController(
                new StubLoginWithPasswordUseCase(),
                new StubConsumeInvitationUseCase(),
                me,
                new StubRequestPasswordResetUseCase(),
                new StubResetPasswordUseCase(),
                new StubLogoutSessionUseCase(),
                new AuthCookieService(new AuthSecurityTestConfig().sitePulseProperties()),
                new AuthenticatedUserAccessor()
        );
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new SessionPrincipal("session-token", new AuthenticatedUser(7, "planner@example.com", UserRole.USER)),
                "session-token",
                List.of()
        ));

        var response = controller.me();

        assertEquals("planner@example.com", response.getUser().getEmail());
        assertEquals("Paula", response.getUser().getFirstName());
        assertEquals("Planner", response.getUser().getLastName());
        assertEquals("USER", response.getUser().getRole());
    }

    private AuthUserResult authUser() {
        return new AuthUserResult(7, "planner@example.com", "Paula", "Planner", UserRole.USER, UserStatus.ACTIVE, List.of(1));
    }

    private static final class StubLoginWithPasswordUseCase extends LoginWithPasswordUseCase {
        private String email;
        private String password;
        private SessionLoginResult result;

        private StubLoginWithPasswordUseCase() {
            super(null, null, null, null);
        }

        @Override
        public SessionLoginResult login(String email, String password) {
            this.email = email;
            this.password = password;
            return result;
        }
    }

    private static final class StubConsumeInvitationUseCase extends ConsumeInvitationUseCase {
        private String token;
        private String firstName;
        private String lastName;
        private String password;
        private SessionLoginResult result;

        private StubConsumeInvitationUseCase() {
            super(null, null);
        }

        @Override
        public SessionLoginResult consume(String token, String firstName, String lastName, String password) {
            this.token = token;
            this.firstName = firstName;
            this.lastName = lastName;
            this.password = password;
            return result;
        }
    }

    private static final class StubGetCurrentAuthSessionQuery extends GetCurrentAuthSessionQuery {
        private AuthUserResult result;

        private StubGetCurrentAuthSessionQuery() {
            super(null, null);
        }

        @Override
        public AuthUserResult get(AuthenticatedUser authenticatedUser) {
            return result;
        }
    }

    private static final class StubRequestPasswordResetUseCase extends RequestPasswordResetUseCase {
        private String email;

        private StubRequestPasswordResetUseCase() {
            super(null);
        }

        @Override
        public void request(String email) {
            this.email = email;
        }
    }

    private static final class StubResetPasswordUseCase extends ResetPasswordUseCase {
        private String token;
        private String password;

        private StubResetPasswordUseCase() {
            super(null);
        }

        @Override
        public void reset(String token, String password) {
            this.token = token;
            this.password = password;
        }
    }

    private static final class StubLogoutSessionUseCase extends LogoutSessionUseCase {
        private String sessionToken;

        private StubLogoutSessionUseCase() {
            super(null);
        }

        @Override
        public void logout(String sessionToken) {
            this.sessionToken = sessionToken;
        }
    }
}
