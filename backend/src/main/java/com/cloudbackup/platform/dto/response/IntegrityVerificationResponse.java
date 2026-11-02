package com.cloudbackup.platform.dto.response;

import com.cloudbackup.platform.model.VerificationStatus;
import java.time.Instant;

public class IntegrityVerificationResponse {

    private String fileId;
    private String versionId;
    private String filename;
    private Integer versionNumber;
    private String expectedChecksum;
    private String actualChecksum;
    private boolean verified;
    private VerificationStatus status;
    private Instant verifiedAt;
    private String message;

    public IntegrityVerificationResponse() {
        this.verifiedAt = Instant.now();
    }

    public IntegrityVerificationResponse(String fileId, String versionId, String filename,
                                         Integer versionNumber, String expectedChecksum,
                                         String actualChecksum, boolean verified,
                                         VerificationStatus status, String message) {
        this.fileId = fileId;
        this.versionId = versionId;
        this.filename = filename;
        this.versionNumber = versionNumber;
        this.expectedChecksum = expectedChecksum;
        this.actualChecksum = actualChecksum;
        this.verified = verified;
        this.status = status;
        this.verifiedAt = Instant.now();
        this.message = message;
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

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
