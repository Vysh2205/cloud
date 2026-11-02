package com.cloudbackup.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import java.net.URI;
import java.time.Duration;

@Configuration
public class AwsS3Config {

    private static final Logger log = LoggerFactory.getLogger(AwsS3Config.class);

    @Value("${cloud.backup.s3.bucket-name}")
    private String bucketName;

    @Value("${cloud.backup.s3.region:us-east-1}")
    private String region;

    @Value("${cloud.backup.s3.endpoint:}")
    private String endpoint;

    @Value("${cloud.backup.s3.access-key}")
    private String accessKey;

    @Value("${cloud.backup.s3.secret-key}")
    private String secretKey;

    @Value("${cloud.backup.s3.path-style-access-enabled:true}")
    private boolean pathStyleAccessEnabled;

    @Value("${cloud.backup.s3.auto-create-bucket:true}")
    private boolean autoCreateBucket;

    @Value("${cloud.backup.s3.enable-versioning:true}")
    private boolean enableVersioning;

    @Bean
    public S3Client s3Client() {
        log.info("[S3Config] Initializing S3 Client. Region: {}, Bucket: {}, Endpoint: {}",
                region, bucketName, endpoint.isBlank() ? "AWS Default" : endpoint);

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccessEnabled)
                .checksumValidationEnabled(true)
                .build();

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(serviceConfiguration)
                .httpClientBuilder(ApacheHttpClient.builder()
                        .maxConnections(100)
                        .connectionTimeout(Duration.ofSeconds(10))
                        .socketTimeout(Duration.ofSeconds(60)));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        S3Client client = builder.build();

        if (autoCreateBucket) {
            initBucket(client);
        }

        return client;
    }

    private void initBucket(S3Client client) {
        try {
            boolean bucketExists = true;
            try {
                client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
                log.info("[S3Config] S3 Bucket '{}' already exists and is accessible.", bucketName);
            } catch (NoSuchBucketException e) {
                bucketExists = false;
            } catch (Exception e) {
                // If 404 is returned as generic S3Exception or connection issue
                log.warn("[S3Config] Checking bucket '{}' returned: {}. Attempting create...", bucketName, e.getMessage());
                bucketExists = false;
            }

            if (!bucketExists) {
                log.info("[S3Config] Creating dedicated S3 Bucket '{}'...", bucketName);
                client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("[S3Config] S3 Bucket '{}' created successfully.", bucketName);
            }

            // Enable AWS S3 Object Versioning on the bucket as defense-in-depth
            if (enableVersioning) {
                try {
                    client.putBucketVersioning(PutBucketVersioningRequest.builder()
                            .bucket(bucketName)
                            .versioningConfiguration(VersioningConfiguration.builder()
                                    .status(BucketVersioningStatus.ENABLED)
                                    .build())
                            .build());
                    log.info("[S3Config] S3 Bucket-level versioning enabled for '{}'.", bucketName);
                } catch (Exception e) {
                    log.warn("[S3Config] Could not set bucket-level versioning: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("[S3Config] Note: Bucket auto-initialization deferred or skipped: {}. (S3 client is ready)", e.getMessage());
        }
    }

    public String getBucketName() {
        return bucketName;
    }
}
