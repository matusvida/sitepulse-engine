package com.sitepulse.engine.auth.web;

import com.sitepulse.engine.auth.application.AuthenticatedSession;
import com.sitepulse.engine.auth.application.AuthenticatedUser;
import com.sitepulse.engine.auth.application.usecase.ResolveAuthenticatedSessionUseCase;
import com.sitepulse.engine.auth.domain.UserRole;
import com.sitepulse.engine.auth.infrastructure.security.SecurityConfig;
import com.sitepulse.engine.auth.infrastructure.security.SecurityErrorResponseWriter;
import com.sitepulse.engine.auth.infrastructure.security.SessionCookieAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.context.TestConfiguration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestSecurityEndpoints.class)
@Import({
        AuthSecurityTestConfig.class,
        SecurityConfig.class,
        SecurityErrorResponseWriter.class,
        SessionCookieAuthenticationFilter.class,
        SecurityConfigWebMvcTest.TestBeans.class
})
class SecurityConfigWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StubProjectAccessAuthorizationService projectAccessAuthorizationService;

    @Test
    void unauthenticatedAdminEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userCannotAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanAccessAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userWithProjectAccessCanAccessProjectEndpoint() throws Exception {
        projectAccessAuthorizationService.allowed = true;

        mockMvc.perform(get("/api/projects/1/activity/summary"))
                .andExpect(status().isOk())
                .andExpect(content().string("project-ok"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userWithoutProjectAccessGets403() throws Exception {
        projectAccessAuthorizationService.allowed = false;

        mockMvc.perform(get("/api/projects/1/activity/summary"))
                .andExpect(status().isForbidden());
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        ResolveAuthenticatedSessionUseCase resolveAuthenticatedSessionUseCase() {
            return new ResolveAuthenticatedSessionUseCase(null) {
                @Override
                public AuthenticatedSession resolve(String sessionToken) {
                    return new AuthenticatedSession(sessionToken, new AuthenticatedUser(1, "admin@example.com", UserRole.ADMIN));
                }
            };
        }

        @Bean(name = "projectAccessAuthorizationService")
        StubProjectAccessAuthorizationService projectAccessAuthorizationService() {
            return new StubProjectAccessAuthorizationService();
        }
    }

    static class StubProjectAccessAuthorizationService {
        private boolean allowed;

        public boolean hasProjectAccess(org.springframework.security.core.Authentication authentication, Integer projectId) {
            return allowed;
        }
    }
}
