package com.cloudbackup.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "recovery_history", indexes = {
    @Index(name = "idx_recovery_history_user_id", columnList = "user_id"),
    @Index(name = "idx_recovery_history_file_id", columnList = "file_id"),
    @Index(name = "idx_recovery_history_restored_at", columnList = "restored_at DESC")
})
public class RecoveryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "file_id", nullable = false, length = 36)
    private String fileId;

    @Column(name = "restored_version_id", nullable = false, length = 36)
    private String restoredVersionId;

    @Column(name = "restored_version_number", nullable = false)
    private Integer restoredVersionNumber;

    @Column(nullable = false, length = 255)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RecoveryStatus status = RecoveryStatus.SUCCESS;

    @Column(name = "checksum_verified", nullable = false)
    private Boolean checksumVerified = false;

    @Column(name = "expected_checksum", length = 64)
    private String expectedChecksum;

    @Column(name = "actual_checksum", length = 64)
    private String actualChecksum;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "restored_at", nullable = false, updatable = false)
    private Instant restoredAt = Instant.now();

    public RecoveryHistory() {}

    public RecoveryHistory(User user, String fileId, String restoredVersionId,
                           Integer restoredVersionNumber, String filename, RecoveryStatus status,
                           Boolean checksumVerified, String expectedChecksum, String actualChecksum,
                           String failureReason) {
        this.user = user;
        this.fileId = fileId;
        this.restoredVersionId = restoredVersionId;
        this.restoredVersionNumber = restoredVersionNumber;
        this.filename = filename;
        this.status = status;
        this.checksumVerified = checksumVerified != null ? checksumVerified : false;
        this.expectedChecksum = expectedChecksum;
        this.actualChecksum = actualChecksum;
        this.failureReason = failureReason;
        this.restoredAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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
