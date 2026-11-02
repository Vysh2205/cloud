package com.cloudbackup.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_versions", indexes = {
    @Index(name = "idx_file_versions_file_id", columnList = "file_id"),
    @Index(name = "idx_file_versions_checksum", columnList = "checksum_sha256"),
    @Index(name = "idx_file_versions_s3_key", columnList = "s3_object_key")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_file_version", columnNames = {"file_id", "version_number"})
})
public class FileVersion {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    @JsonIgnore
    private BackupFile file;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "s3_bucket", nullable = false, length = 100)
    private String s3Bucket;

    @Column(name = "s3_object_key", nullable = false, length = 500)
    private String s3ObjectKey;

    @Column(name = "s3_version_id", length = 100)
    private String s3VersionId;

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "change_summary", length = 255)
    private String changeSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 30)
    private VerificationStatus verificationStatus = VerificationStatus.VERIFIED;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public FileVersion() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }

    public FileVersion(BackupFile file, Integer versionNumber, String s3Bucket, String s3ObjectKey,
                       String checksumSha256, Long fileSizeBytes, String contentType, String changeSummary) {
        this.id = UUID.randomUUID().toString();
        this.file = file;
        this.versionNumber = versionNumber;
        this.s3Bucket = s3Bucket;
        this.s3ObjectKey = s3ObjectKey;
        this.checksumSha256 = checksumSha256;
        this.fileSizeBytes = fileSizeBytes;
        this.contentType = contentType != null ? contentType : "application/octet-stream";
        this.changeSummary = changeSummary;
        this.verificationStatus = VerificationStatus.VERIFIED;
        this.lastVerifiedAt = Instant.now();
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BackupFile getFile() {
        return file;
    }

    public void setFile(BackupFile file) {
        this.file = file;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public void setS3Bucket(String s3Bucket) {
        this.s3Bucket = s3Bucket;
    }

    public String getS3ObjectKey() {
        return s3ObjectKey;
    }

    public void setS3ObjectKey(String s3ObjectKey) {
        this.s3ObjectKey = s3ObjectKey;
    }

    public String getS3VersionId() {
        return s3VersionId;
    }

    public void setS3VersionId(String s3VersionId) {
        this.s3VersionId = s3VersionId;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Instant getLastVerifiedAt() {
        return lastVerifiedAt;
    }

    public void setLastVerifiedAt(Instant lastVerifiedAt) {
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
