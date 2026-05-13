package com.sitepulse.engine.auth.domain.model;

public record EmailAddress(String value) {

    public EmailAddress {
        value = value == null ? "" : value;
    }
}
