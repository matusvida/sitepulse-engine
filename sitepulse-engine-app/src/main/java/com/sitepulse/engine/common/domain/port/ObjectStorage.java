package com.sitepulse.engine.common.domain.port;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;

public interface ObjectStorage {

    byte[] download(String bucket, String key);

    boolean exists(String bucket, String key);

    default void upload(String bucket, String key, byte[] data, String contentType) {
        upload(bucket, key, new ByteArrayInputStream(data), data.length, contentType);
    }

    void upload(String bucket, String key, InputStream data, long size, String contentType);

    String defaultBucket();

    String presign(String bucket, String key, Duration expiresAfter);
}
