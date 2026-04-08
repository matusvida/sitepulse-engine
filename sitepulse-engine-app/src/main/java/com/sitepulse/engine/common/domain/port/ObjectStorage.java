package com.sitepulse.engine.common.domain.port;

import java.time.Duration;

public interface ObjectStorage {

    byte[] download(String bucket, String key);

    boolean exists(String bucket, String key);

    void upload(String bucket, String key, byte[] data, String contentType);

    String defaultBucket();

    String presign(String bucket, String key, Duration expiresAfter);
}
