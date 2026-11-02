package com.cloudbackup.platform.service;

import com.cloudbackup.platform.exception.ResourceNotFoundException;
import com.cloudbackup.platform.exception.StorageException;
import com.cloudbackup.platform.util.RetryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.Map;

@Service
public class AwsS3StorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(AwsS3StorageService.class);

    private final S3Client s3Client;
    private final String defaultBucket;

    @Value("${cloud.backup.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Value("${cloud.backup.retry.initial-interval-ms:1000}")
    private long initialRetryIntervalMs;

    @Value("${cloud.backup.retry.multiplier:2.0}")
    private double retryMultiplier;

    public AwsS3StorageService(S3Client s3Client, @Value("${cloud.backup.s3.bucket-name}") String defaultBucket) {
        this.s3Client = s3Client;
        this.defaultBucket = defaultBucket;
    }

    @Override
    public String uploadObject(String bucket, String key, InputStream inputStream, long contentLength,
                               String contentType, Map<String, String> metadata) {
        String targetBucket = (bucket != null && !bucket.isBlank()) ? bucket : defaultBucket;
        log.info("[S3Storage] Uploading object key='{}' to bucket='{}', size={} bytes", key, targetBucket, contentLength);

        try {
            return RetryUtils.executeWithRetry(() -> {
                PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                        .bucket(targetBucket)
                        .key(key)
                        .contentType(contentType != null ? contentType : "application/octet-stream")
                        .contentLength(contentLength);

                if (metadata != null && !metadata.isEmpty()) {
                    requestBuilder.metadata(metadata);
                }

                PutObjectResponse response = s3Client.putObject(
                        requestBuilder.build(),
                        RequestBody.fromInputStream(inputStream, contentLength)
                );

                log.info("[S3Storage] Upload success for key='{}'. ETag={}, S3VersionId={}",
                        key, response.eTag(), response.versionId());

                return response.versionId();
            }, maxRetryAttempts, initialRetryIntervalMs, retryMultiplier, "S3 PutObject: " + key);

        } catch (Exception ex) {
            log.error("[S3Storage] Failed to upload object key='{}' after retries: {}", key, ex.getMessage());
            throw new StorageException("AWS S3 upload failed for key: " + key + ". Reason: " + ex.getMessage(), ex);
        }
    }

    @Override
    public InputStream downloadObjectStream(String bucket, String key) {
        String targetBucket = (bucket != null && !bucket.isBlank()) ? bucket : defaultBucket;
        log.info("[S3Storage] Fetching object stream for key='{}' from bucket='{}'", key, targetBucket);

        try {
            return RetryUtils.executeWithRetry(() -> {
                GetObjectRequest request = GetObjectRequest.builder()
                        .bucket(targetBucket)
                        .key(key)
                        .build();

                ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(request);
                return s3Stream;
            }, maxRetryAttempts, initialRetryIntervalMs, retryMultiplier, "S3 GetObject: " + key);

        } catch (NoSuchKeyException ex) {
            log.warn("[S3Storage] Object not found in S3: bucket='{}', key='{}'", targetBucket, key);
            throw new ResourceNotFoundException("S3 object not found for key: " + key);
        } catch (Exception ex) {
            log.error("[S3Storage] Failed to download object key='{}': {}", key, ex.getMessage());
            throw new StorageException("AWS S3 download failed for key: " + key + ". Reason: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean objectExists(String bucket, String key) {
        String targetBucket = (bucket != null && !bucket.isBlank()) ? bucket : defaultBucket;
        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(targetBucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.warn("[S3Storage] S3 headObject error for key='{}': {}", key, e.getMessage());
            return false;
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        String targetBucket = (bucket != null && !bucket.isBlank()) ? bucket : defaultBucket;
        log.info("[S3Storage] Compensating deletion for key='{}' in bucket='{}'", key, targetBucket);
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(targetBucket).key(key).build());
        } catch (Exception e) {
            log.warn("[S3Storage] S3 deleteObject warning for key='{}': {}", key, e.getMessage());
        }
    }

    @Override
    public String getDefaultBucket() {
        return defaultBucket;
    }
}
