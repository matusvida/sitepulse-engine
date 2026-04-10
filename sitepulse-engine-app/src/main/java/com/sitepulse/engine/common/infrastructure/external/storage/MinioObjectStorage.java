package com.sitepulse.engine.common.infrastructure.external.storage;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.ExternalServiceException;
import com.sitepulse.engine.config.SitePulseProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MinioObjectStorage implements ObjectStorage {

    private final SitePulseProperties properties;
    private final MinioClient minioClient;
    private final MinioClient presignClient;

    public MinioObjectStorage(SitePulseProperties properties) {
        this.properties = properties;
        validateStorageProvider();
        SitePulseProperties.StorageProperties storage = requireStorageProperties(properties);
        MinioClient.Builder minioBuilder = MinioClient.builder()
                .endpoint(requireValue(storage.endpoint(), "STORAGE_ENDPOINT"))
                .credentials(requireValue(storage.accessKey(), "STORAGE_ACCESS_KEY"), requireValue(storage.secretKey(), "STORAGE_SECRET_KEY"));
        if (storage.region() != null && !storage.region().isBlank()) {
            minioBuilder.region(storage.region());
        }
        this.minioClient = minioBuilder.build();

        MinioClient.Builder presignBuilder = MinioClient.builder()
                .endpoint(requireValue(storage.publicEndpoint(), "STORAGE_PUBLIC_ENDPOINT"))
                .credentials(requireValue(storage.accessKey(), "STORAGE_ACCESS_KEY"), requireValue(storage.secretKey(), "STORAGE_SECRET_KEY"));
        if (storage.region() != null && !storage.region().isBlank()) {
            presignBuilder.region(storage.region());
        }
        this.presignClient = presignBuilder.build();
    }

    @PostConstruct
    public void ensureDefaultBucketExists() {
        String bucket = properties.storageDefaultBucket();
        if (!properties.usesLocalStorageProvisioning()) {
            log.info("Skipping automatic bucket creation for storageProvider={}", normalizedStorageProvider());
            return;
        }
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created default storage bucket={}", bucket);
            } else {
                log.info("Default storage bucket already exists bucket={}", bucket);
            }
        } catch (ErrorResponseException
                 | InsufficientDataException
                 | InternalException
                 | InvalidKeyException
                 | InvalidResponseException
                 | IOException
                 | NoSuchAlgorithmException
                 | ServerException
                 | XmlParserException ex) {
            log.error("Failed to ensure default storage bucket exists bucket={}", bucket, ex);
            throw new IllegalStateException("Failed to initialize default storage bucket " + bucket, ex);
        }
    }

    @Override
    public byte[] download(String bucket, String key) {
        log.debug("Downloading object from storage bucket={} key={}", bucket, key);
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return inputStream.readAllBytes();
        } catch (ErrorResponseException
                 | InsufficientDataException
                 | InternalException
                 | InvalidKeyException
                 | InvalidResponseException
                 | IOException
                 | NoSuchAlgorithmException
                 | ServerException
                 | XmlParserException ex) {
            throw new ExternalServiceException("Failed to download object bucket=" + bucket + " key=" + key, ex);
        }
    }

    @Override
    public void upload(String bucket, String key, byte[] data, String contentType) {
        log.debug("Uploading object to storage bucket={} key={} bytes={} contentType={}", bucket, key, data.length, contentType);
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (ErrorResponseException
                 | InsufficientDataException
                 | InternalException
                 | InvalidKeyException
                 | InvalidResponseException
                 | IOException
                 | NoSuchAlgorithmException
                 | ServerException
                 | XmlParserException ex) {
            log.error("Failed to upload object to storage bucket={} key={}", bucket, key, ex);
            throw new ExternalServiceException("Failed to upload object bucket=" + bucket + " key=" + key, ex);
        }
    }

    @Override
    public boolean exists(String bucket, String key) {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            return true;
        } catch (ErrorResponseException
                 | InsufficientDataException
                 | InternalException
                 | InvalidKeyException
                 | InvalidResponseException
                 | IOException
                 | NoSuchAlgorithmException
                 | ServerException
                 | XmlParserException ex) {
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
            return presignClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .expiry(Math.toIntExact(expiresAfter.toSeconds()), TimeUnit.SECONDS)
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
        } catch (ErrorResponseException
                 | InsufficientDataException
                 | InternalException
                 | InvalidKeyException
                 | InvalidResponseException
                 | IOException
                 | NoSuchAlgorithmException
                 | ServerException
                 | XmlParserException ex) {
            throw new ExternalServiceException("Failed to presign object bucket=" + bucket + " key=" + key, ex);
        }
    }

    private String normalizedStorageProvider() {
        return properties.storageProvider() == null ? "" : properties.storageProvider().trim().toLowerCase(Locale.ROOT);
    }

    private void validateStorageProvider() {
        String provider = normalizedStorageProvider();
        if (!Set.of("minio", "local", "s3", "r2").contains(provider)) {
            throw new IllegalStateException("Unsupported storage provider: " + properties.storageProvider());
        }
    }

    private static SitePulseProperties.StorageProperties requireStorageProperties(SitePulseProperties properties) {
        SitePulseProperties.StorageProperties storage = properties.storage();
        if (storage == null) {
            throw new IllegalStateException("Missing storage configuration");
        }
        requireValue(storage.endpoint(), "STORAGE_ENDPOINT");
        requireValue(storage.publicEndpoint(), "STORAGE_PUBLIC_ENDPOINT");
        requireValue(storage.accessKey(), "STORAGE_ACCESS_KEY");
        requireValue(storage.secretKey(), "STORAGE_SECRET_KEY");
        return storage;
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required storage configuration: " + name);
        }
        return value;
    }
}
