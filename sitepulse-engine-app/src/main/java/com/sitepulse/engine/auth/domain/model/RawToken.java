package com.sitepulse.engine.auth.domain.model;

public record RawToken(String value) {

    public RawToken {
        value = value == null ? "" : value;
    }
}
