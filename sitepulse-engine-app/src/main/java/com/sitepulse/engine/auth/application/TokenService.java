package com.sitepulse.engine.auth.application;

import com.sitepulse.engine.auth.domain.model.RawToken;
import com.sitepulse.engine.auth.domain.model.TokenHash;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class TokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    public RawToken generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return new RawToken(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public TokenHash hash(RawToken rawToken) {
        return hash(rawToken.value());
    }

    public TokenHash hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return new TokenHash(builder.toString());
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest is not available", ex);
        }
    }
}
