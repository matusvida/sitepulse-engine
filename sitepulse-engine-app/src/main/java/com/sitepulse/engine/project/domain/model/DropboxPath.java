package com.sitepulse.engine.project.domain.model;

public record DropboxPath(String value) {
    public DropboxPath {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException("Dropbox path must not be blank");
        }
    }
}
