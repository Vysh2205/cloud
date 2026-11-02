package com.cloudbackup.platform.dto.response;

import com.cloudbackup.platform.model.FileVersion;
import com.cloudbackup.platform.model.VerificationStatus;
import java.time.Instant;

public class FileVersionResponse {

    private String id;
    private String fileId;
    private Integer versionNumber;
    private String s3Bucket;
    private String s3ObjectKey;
    private String s3VersionId;
    private String checksumSha256;
    private Long fileSizeBytes;
    private String contentType;
    private String changeSummary;
    private VerificationStatus verificationStatus;
    private Instant lastVerifiedAt;
    private Instant createdAt;

    public FileVersionResponse() {}

    public static FileVersionResponse fromEntity(FileVersion version) {
        FileVersionResponse dto = new FileVersionResponse();
        dto.setId(version.getId());
        dto.setFileId(version.getFile() != null ? version.getFile().getId() : null);
        dto.setVersionNumber(version.getVersionNumber());
        dto.setS3Bucket(version.getS3Bucket());
        dto.setS3ObjectKey(version.getS3ObjectKey());
        dto.setS3VersionId(version.getS3VersionId());
        dto.setChecksumSha256(version.getChecksumSha256());
        dto.setFileSizeBytes(version.getFileSizeBytes());
        dto.setContentType(version.getContentType());
        dto.setChangeSummary(version.getChangeSummary());
        dto.setVerificationStatus(version.getVerificationStatus());
        dto.setLastVerifiedAt(version.getLastVerifiedAt());
        dto.setCreatedAt(version.getCreatedAt());
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
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
