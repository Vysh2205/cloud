package com.cloudbackup.platform.dto.response;

import com.cloudbackup.platform.model.BackupFile;
import com.cloudbackup.platform.model.FileStatus;
import java.time.Instant;

public class FileResponse {

    private String id;
    private String filename;
    private String contentType;
    private String currentVersionId;
    private Integer currentVersionNumber;
    private Integer totalVersions;
    private Long totalSizeBytes;
    private FileStatus status;
    private String checksumSha256;
    private Instant createdAt;
    private Instant updatedAt;

    public FileResponse() {}

    public static FileResponse fromEntity(BackupFile file) {
        FileResponse dto = new FileResponse();
        dto.setId(file.getId());
        dto.setFilename(file.getFilename());
        dto.setContentType(file.getContentType());
        dto.setCurrentVersionNumber(file.getCurrentVersionNumber());
        dto.setTotalVersions(file.getTotalVersions());
        dto.setTotalSizeBytes(file.getTotalSizeBytes());
        dto.setStatus(file.getStatus());
        dto.setCreatedAt(file.getCreatedAt());
        dto.setUpdatedAt(file.getUpdatedAt());

        if (file.getCurrentVersion() != null) {
            dto.setCurrentVersionId(file.getCurrentVersion().getId());
            dto.setChecksumSha256(file.getCurrentVersion().getChecksumSha256());
        }
        return dto;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(String currentVersionId) {
        this.currentVersionId = currentVersionId;
    }

    public Integer getCurrentVersionNumber() {
        return currentVersionNumber;
    }

    public void setCurrentVersionNumber(Integer currentVersionNumber) {
        this.currentVersionNumber = currentVersionNumber;
    }

    public Integer getTotalVersions() {
        return totalVersions;
    }

    public void setTotalVersions(Integer totalVersions) {
        this.totalVersions = totalVersions;
    }

    public Long getTotalSizeBytes() {
        return totalSizeBytes;
    }

    public void setTotalSizeBytes(Long totalSizeBytes) {
        this.totalSizeBytes = totalSizeBytes;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
