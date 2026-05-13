package com.sitepulse.engine.auth.domain.model;

public record AuthMailRecipient(
        String email,
        String firstName,
        String lastName
) {
}
