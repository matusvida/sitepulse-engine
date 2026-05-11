package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.config.SitePulseProperties;
import org.springframework.http.ResponseCookie;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthCookieService {

    private final SitePulseProperties properties;

    public String createSessionCookie(String token) {
        return ResponseCookie.from(properties.auth().sessionCookieName(), token)
                .httpOnly(true)
                .secure(properties.auth().sessionCookieSecure())
                .sameSite(properties.auth().sessionCookieSameSite())
                .path("/")
                .maxAge(properties.auth().sessionTtl())
                .build()
                .toString();
    }

    public String clearSessionCookie() {
        return ResponseCookie.from(properties.auth().sessionCookieName(), "")
                .httpOnly(true)
                .secure(properties.auth().sessionCookieSecure())
                .sameSite(properties.auth().sessionCookieSameSite())
                .path("/")
                .maxAge(0)
                .build()
                .toString();
    }
}
