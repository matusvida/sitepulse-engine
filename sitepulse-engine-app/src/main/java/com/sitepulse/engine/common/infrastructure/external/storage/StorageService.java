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
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import jakarta.annotation.PostConstruct;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StorageService implements ObjectStorage {

    private final SitePulseProperties properties;
    private final MinioClient minioClient;

    public StorageService(SitePulseProperties properties) {
        this.properties = properties;
        this.minioClient = MinioClient.builder()
                .endpoint(properties.minioEndpoint())
                .credentials(properties.minioAccessKey(), properties.minioSecretKey())
                .build();
    }

    @PostConstruct
    public void ensureDefaultBucketExists() {
        String bucket = properties.minioBucketDefault();
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
            throw new ExternalServiceException("Failed to download s3://" + bucket + "/" + key, ex);
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
            throw new ExternalServiceException("Failed to upload s3://" + bucket + "/" + key, ex);
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
        return properties.minioBucketDefault();
    }

    public String presign(String bucket, String key) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
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
            throw new ExternalServiceException("Failed to presign s3://" + bucket + "/" + key, ex);
        }
    }
}
