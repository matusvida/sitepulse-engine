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
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "sitepulse", name = "storage-provider", havingValue = "minio", matchIfMissing = true)
public class MinioObjectStorage implements ObjectStorage {

    private final SitePulseProperties properties;
    private final MinioClient minioClient;
    private final MinioClient presignClient;

    public MinioObjectStorage(SitePulseProperties properties) {
        this.properties = properties;
        SitePulseProperties.MinioProperties minio = requireMinioProperties(properties);
        this.minioClient = MinioClient.builder()
                .endpoint(requireValue(minio.endpoint(), "MINIO_ENDPOINT"))
                .credentials(requireValue(minio.accessKey(), "MINIO_ACCESS_KEY"), requireValue(minio.secretKey(), "MINIO_SECRET_KEY"))
                .build();
        this.presignClient = MinioClient.builder()
                .endpoint(requireValue(minio.publicEndpoint(), "MINIO_PUBLIC_ENDPOINT"))
                .credentials(requireValue(minio.accessKey(), "MINIO_ACCESS_KEY"), requireValue(minio.secretKey(), "MINIO_SECRET_KEY"))
                .build();
    }

    @PostConstruct
    public void ensureDefaultBucketExists() {
        String bucket = properties.storageDefaultBucket();
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created default MinIO bucket={}", bucket);
            } else {
                log.info("Default MinIO bucket already exists bucket={}", bucket);
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
            log.error("Failed to ensure default MinIO bucket exists bucket={}", bucket, ex);
            throw new IllegalStateException("Failed to initialize default MinIO bucket " + bucket, ex);
        }
    }

    @Override
    public byte[] download(String bucket, String key) {
        log.debug("Downloading object from MinIO bucket={} key={}", bucket, key);
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
        log.debug("Uploading object to MinIO bucket={} key={} bytes={} contentType={}", bucket, key, data.length, contentType);
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
            log.error("Failed to upload object to MinIO bucket={} key={}", bucket, key, ex);
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

    private static SitePulseProperties.MinioProperties requireMinioProperties(SitePulseProperties properties) {
        SitePulseProperties.MinioProperties minio = properties.minio();
        if (minio == null) {
            throw new IllegalStateException("Missing MinIO configuration");
        }
        requireValue(minio.endpoint(), "MINIO_ENDPOINT");
        requireValue(minio.publicEndpoint(), "MINIO_PUBLIC_ENDPOINT");
        requireValue(minio.accessKey(), "MINIO_ACCESS_KEY");
        requireValue(minio.secretKey(), "MINIO_SECRET_KEY");
        return minio;
    }

    private static String requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required storage configuration: " + name);
        }
        return value;
    }
}
