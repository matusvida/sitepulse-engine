package com.sitepulse.engine.integration.storage;

import com.sitepulse.engine.common.web.ApiException;
import com.sitepulse.engine.config.SitePulseProperties;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.BucketExistsArgs;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StorageService {

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
        } catch (Exception ex) {
            log.error("Failed to ensure default MinIO bucket exists bucket={}", bucket, ex);
            throw new IllegalStateException("Failed to initialize default MinIO bucket " + bucket, ex);
        }
    }

    public byte[] download(String bucket, String key) {
        log.debug("Downloading object from MinIO bucket={} key={}", bucket, key);
        try (InputStream inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return inputStream.readAllBytes();
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to download s3://" + bucket + "/" + key);
        }
    }

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
        } catch (Exception ex) {
            log.error("Failed to upload object to MinIO bucket={} key={}", bucket, key, ex);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to upload s3://" + bucket + "/" + key);
        }
    }

    public boolean exists(String bucket, String key) {
        try {
            minioClient.statObject(StatObjectArgs.builder().bucket(bucket).object(key).build());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

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
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Failed to presign s3://" + bucket + "/" + key);
        }
    }
}
