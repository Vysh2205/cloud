package com.cloudbackup.platform.dto.response;

import com.cloudbackup.platform.model.RecoveryHistory;
import com.cloudbackup.platform.model.RecoveryStatus;
import java.time.Instant;

public class RecoveryHistoryResponse {

    private Long id;
    private String fileId;
    private String restoredVersionId;
    private Integer restoredVersionNumber;
    private String filename;
    private RecoveryStatus status;
    private Boolean checksumVerified;
    private String expectedChecksum;
    private String actualChecksum;
    private String failureReason;
    private Instant restoredAt;

    public RecoveryHistoryResponse() {}

    public static RecoveryHistoryResponse fromEntity(RecoveryHistory history) {
        RecoveryHistoryResponse dto = new RecoveryHistoryResponse();
        dto.setId(history.getId());
        dto.setFileId(history.getFileId());
        dto.setRestoredVersionId(history.getRestoredVersionId());
        dto.setRestoredVersionNumber(history.getRestoredVersionNumber());
        dto.setFilename(history.getFilename());
        dto.setStatus(history.getStatus());
        dto.setChecksumVerified(history.getChecksumVerified());
        dto.setExpectedChecksum(history.getExpectedChecksum());
        dto.setActualChecksum(history.getActualChecksum());
        dto.setFailureReason(history.getFailureReason());
        dto.setRestoredAt(history.getRestoredAt());
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

    public String getRestoredVersionId() {
        return restoredVersionId;
    }

    public void setRestoredVersionId(String restoredVersionId) {
        this.restoredVersionId = restoredVersionId;
    }

    public Integer getRestoredVersionNumber() {
        return restoredVersionNumber;
    }

    public void setRestoredVersionNumber(Integer restoredVersionNumber) {
        this.restoredVersionNumber = restoredVersionNumber;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public RecoveryStatus getStatus() {
        return status;
    }

    public void setStatus(RecoveryStatus status) {
        this.status = status;
    }

    public Boolean getChecksumVerified() {
        return checksumVerified;
    }

    public void setChecksumVerified(Boolean checksumVerified) {
        this.checksumVerified = checksumVerified;
    }

    public String getExpectedChecksum() {
        return expectedChecksum;
    }

    public void setExpectedChecksum(String expectedChecksum) {
        this.expectedChecksum = expectedChecksum;
    }

    public String getActualChecksum() {
        return actualChecksum;
    }

    public void setActualChecksum(String actualChecksum) {
        this.actualChecksum = actualChecksum;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getRestoredAt() {
        return restoredAt;
    }

    public void setRestoredAt(Instant restoredAt) {
        this.restoredAt = restoredAt;
    }
}
