package com.sitepulse.engine.auth.infrastructure.security;

import com.sitepulse.engine.auth.application.AuthenticatedSession;
import com.sitepulse.engine.auth.application.usecase.ResolveAuthenticatedSessionUseCase;
import com.sitepulse.engine.auth.exception.UnauthorizedException;
import com.sitepulse.engine.config.SitePulseProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class SessionCookieAuthenticationFilter extends OncePerRequestFilter {

    private final SitePulseProperties properties;
    private final ResolveAuthenticatedSessionUseCase resolveAuthenticatedSessionUseCase;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String rawSessionToken = readSessionCookie(request);
            if (rawSessionToken != null && !rawSessionToken.isBlank()) {
                try {
                    AuthenticatedSession authenticatedSession = resolveAuthenticatedSessionUseCase.resolve(rawSessionToken);
                    SessionPrincipal principal = new SessionPrincipal(authenticatedSession.sessionToken(), authenticatedSession.user());
                    UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                            principal,
                            authenticatedSession.sessionToken(),
                            principal.authorities()
                    );
                    authentication.setDetails(principal);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (UnauthorizedException ignored) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String readSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (properties.auth().sessionCookieName().equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
