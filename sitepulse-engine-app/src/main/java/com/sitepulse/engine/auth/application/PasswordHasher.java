package com.sitepulse.engine.auth.application;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hash(String password) {
        return encoder.encode(password);
    }

    public boolean matches(String rawPassword, String passwordHash) {
        return passwordHash != null && encoder.matches(rawPassword, passwordHash);
    }
}
