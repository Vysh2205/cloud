package com.cloudbackup.platform.dto.response;

import com.cloudbackup.platform.model.BackupHistory;
import com.cloudbackup.platform.model.BackupStatus;
import java.time.Instant;

public class BackupHistoryResponse {

    private Long id;
    private String fileId;
    private String versionId;
    private String filename;
    private Integer versionNumber;
    private Long fileSizeBytes;
    private String checksumSha256;
    private BackupStatus status;
    private String failureReason;
    private Integer retryCount;
    private Instant createdAt;

    public BackupHistoryResponse() {}

    public static BackupHistoryResponse fromEntity(BackupHistory history) {
        BackupHistoryResponse dto = new BackupHistoryResponse();
        dto.setId(history.getId());
        dto.setFileId(history.getFileId());
        dto.setVersionId(history.getVersionId());
        dto.setFilename(history.getFilename());
        dto.setVersionNumber(history.getVersionNumber());
        dto.setFileSizeBytes(history.getFileSizeBytes());
        dto.setChecksumSha256(history.getChecksumSha256());
        dto.setStatus(history.getStatus());
        dto.setFailureReason(history.getFailureReason());
        dto.setRetryCount(history.getRetryCount());
        dto.setCreatedAt(history.getCreatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public BackupStatus getStatus() {
        return status;
    }

    public void setStatus(BackupStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
