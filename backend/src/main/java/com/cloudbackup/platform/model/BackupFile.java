package com.cloudbackup.platform.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "files", indexes = {
    @Index(name = "idx_files_user_id", columnList = "user_id"),
    @Index(name = "idx_files_filename", columnList = "filename"),
    @Index(name = "idx_files_status", columnList = "status")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_user_filename", columnNames = {"user_id", "filename"})
})
public class BackupFile {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType = "application/octet-stream";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private FileVersion currentVersion;

    @Column(name = "current_version_number", nullable = false)
    private Integer currentVersionNumber = 1;

    @Column(name = "total_versions", nullable = false)
    private Integer totalVersions = 1;

    @Column(name = "total_size_bytes", nullable = false)
    private Long totalSizeBytes = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FileStatus status = FileStatus.ACTIVE;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("versionNumber DESC")
    private List<FileVersion> versions = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public BackupFile() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public BackupFile(User user, String filename, String contentType) {
        this.id = UUID.randomUUID().toString();
        this.user = user;
        this.filename = filename;
        this.contentType = contentType != null ? contentType : "application/octet-stream";
        this.status = FileStatus.ACTIVE;
        this.isDeleted = false;
        this.currentVersionNumber = 1;
        this.totalVersions = 1;
        this.totalSizeBytes = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addVersion(FileVersion version) {
        this.versions.add(version);
        version.setFile(this);
        this.currentVersion = version;
        this.currentVersionNumber = version.getVersionNumber();
        this.totalVersions = this.versions.size();
        this.totalSizeBytes = version.getFileSizeBytes();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public FileVersion getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(FileVersion currentVersion) {
        this.currentVersion = currentVersion;
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

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public List<FileVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<FileVersion> versions) {
        this.versions = versions;
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
