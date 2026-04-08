package com.sitepulse.engine.common.infrastructure.external.storage;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.ExternalServiceException;
import com.sitepulse.engine.config.SitePulseProperties;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "sitepulse", name = "storage-provider", havingValue = "gcs")
public class GcsObjectStorage implements ObjectStorage {

    private final SitePulseProperties properties;
    private final Storage storage;
    private final ServiceAccountCredentials credentials;

    public GcsObjectStorage(SitePulseProperties properties) {
        this.properties = properties;
        SitePulseProperties.GcsProperties gcs = requireGcsProperties(properties);
        this.credentials = loadCredentials(gcs.credentialsPath());
        this.storage = StorageOptions.newBuilder()
                .setProjectId(resolveProjectId(gcs.projectId(), credentials))
                .setCredentials(credentials)
                .build()
                .getService();
    }

    @PostConstruct
    public void ensureDefaultBucketExists() {
        String bucket = properties.storageDefaultBucket();
        try {
            if (storage.get(bucket) == null) {
                storage.create(BucketInfo.newBuilder(bucket).build());
                log.info("Created default GCS bucket={}", bucket);
            } else {
                log.info("Default GCS bucket already exists bucket={}", bucket);
            }
        } catch (StorageException ex) {
            log.error("Failed to ensure default GCS bucket exists bucket={}", bucket, ex);
            throw new IllegalStateException("Failed to initialize default GCS bucket " + bucket, ex);
        }
    }

    @Override
    public byte[] download(String bucket, String key) {
        log.debug("Downloading object from GCS bucket={} key={}", bucket, key);
        try (ReadChannel reader = storage.reader(BlobId.of(bucket, key));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ByteBuffer buffer = ByteBuffer.allocate(8_192);
            while (true) {
                int read = reader.read(buffer);
                if (read < 0) {
                    break;
                }
                if (read == 0) {
                    continue;
                }
                buffer.flip();
                outputStream.write(buffer.array(), 0, buffer.limit());
                buffer.clear();
            }
            return outputStream.toByteArray();
        } catch (IOException | StorageException ex) {
            throw new ExternalServiceException("Failed to download object bucket=" + bucket + " key=" + key, ex);
        }
    }

    @Override
    public void upload(String bucket, String key, byte[] data, String contentType) {
        log.debug("Uploading object to GCS bucket={} key={} bytes={} contentType={}", bucket, key, data.length, contentType);
        try {
            storage.create(
                    BlobInfo.newBuilder(bucket, key)
                            .setContentType(contentType)
                            .build(),
                    data
            );
        } catch (StorageException ex) {
            log.error("Failed to upload object to GCS bucket={} key={}", bucket, key, ex);
            throw new ExternalServiceException("Failed to upload object bucket=" + bucket + " key=" + key, ex);
        }
    }

    @Override
    public boolean exists(String bucket, String key) {
        try {
            return storage.get(BlobId.of(bucket, key)) != null;
        } catch (StorageException ex) {
            return false;
        }
    }

    @Override
    public String defaultBucket() {
        return properties.storageDefaultBucket();
    }

    @Override
    public String presign(String bucket, String key, Duration expiresAfter) {
        try {
            return storage.signUrl(
                    BlobInfo.newBuilder(bucket, key).build(),
                    expiresAfter.toSeconds(),
                    TimeUnit.SECONDS,
                    Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                    Storage.SignUrlOption.withV4Signature(),
                    Storage.SignUrlOption.signWith(credentials)
            ).toString();
        } catch (StorageException ex) {
            throw new ExternalServiceException("Failed to presign object bucket=" + bucket + " key=" + key, ex);
        }
    }

    private static SitePulseProperties.GcsProperties requireGcsProperties(SitePulseProperties properties) {
        SitePulseProperties.GcsProperties gcs = properties.gcs();
        if (gcs == null) {
            throw new IllegalStateException("Missing GCS configuration");
        }
        if (gcs.credentialsPath() == null || gcs.credentialsPath().isBlank()) {
            throw new IllegalStateException("Missing required storage configuration: GCS_CREDENTIALS_PATH");
        }
        return gcs;
    }

    private static ServiceAccountCredentials loadCredentials(String credentialsPath) {
        Path path = Path.of(credentialsPath);
        try (InputStream inputStream = Files.newInputStream(path)) {
            return ServiceAccountCredentials.fromStream(inputStream);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load GCS credentials from " + credentialsPath, ex);
        }
    }

    private static String resolveProjectId(String configuredProjectId, ServiceAccountCredentials credentials) {
        if (configuredProjectId != null && !configuredProjectId.isBlank()) {
            return configuredProjectId;
        }
        String projectId = credentials.getProjectId();
        if (projectId != null && !projectId.isBlank()) {
            return projectId;
        }
        throw new IllegalStateException("Missing required storage configuration: GCS_PROJECT_ID");
    }
}
