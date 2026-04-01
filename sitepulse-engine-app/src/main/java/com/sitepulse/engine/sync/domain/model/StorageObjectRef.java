package com.sitepulse.engine.sync.domain.model;

public record StorageObjectRef(String bucket, String key) {
    public StorageObjectRef {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Storage bucket must not be blank");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Storage key must not be blank");
        }
    }
}
