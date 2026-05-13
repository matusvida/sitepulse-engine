package com.sitepulse.engine.auth.domain.model;

public record TokenHash(String value) {

    public TokenHash {
        value = value == null ? "" : value;
    }
}
