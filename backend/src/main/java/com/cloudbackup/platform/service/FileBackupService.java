package com.cloudbackup.platform.service;

import com.cloudbackup.platform.dto.response.FileResponse;
import com.cloudbackup.platform.dto.response.FileVersionResponse;
import com.cloudbackup.platform.exception.DuplicateFileException;
import com.cloudbackup.platform.exception.QuotaExceededException;
import com.cloudbackup.platform.exception.ResourceNotFoundException;
import com.cloudbackup.platform.exception.StorageException;
import com.cloudbackup.platform.model.*;
import com.cloudbackup.platform.repository.BackupFileRepository;
import com.cloudbackup.platform.repository.BackupHistoryRepository;
import com.cloudbackup.platform.repository.FileVersionRepository;
import com.cloudbackup.platform.repository.UserRepository;
import com.cloudbackup.platform.util.ChecksumUtils;
import com.cloudbackup.platform.util.S3KeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileBackupService {

    private static final Logger log = LoggerFactory.getLogger(FileBackupService.class);

    private final BackupFileRepository fileRepository;
    private final FileVersionRepository versionRepository;
    private final BackupHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final FileStorageService storageService;

    public FileBackupService(
            BackupFileRepository fileRepository,
            FileVersionRepository versionRepository,
            BackupHistoryRepository historyRepository,
            UserRepository userRepository,
            FileStorageService storageService) {
        this.fileRepository = fileRepository;
        this.versionRepository = versionRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    /**
     * Uploads and backs up a file with automatic version incrementing,
     * SHA-256 calculation, idempotency checks, and atomic rollback on failure.
     */
    @Transactional
    public FileResponse uploadFile(MultipartFile multipartFile, String changeSummary, User user) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty.");
        }

        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "file_" + UUID.randomUUID().toString().substring(0, 8);
        }
        String sanitizedFilename = S3KeyGenerator.sanitizeFilename(originalFilename);
        long fileSize = multipartFile.getSize();
        String contentType = multipartFile.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        // 1. Quota Enforcement
        long requiredStorage = user.getUsedStorageBytes() + fileSize;
        if (requiredStorage > user.getStorageQuotaBytes()) {
            log.warn("[BackupService] Quota exceeded for user {}. Used: {}, Required: {}, Quota: {}",
                    user.getUsername(), user.getUsedStorageBytes(), requiredStorage, user.getStorageQuotaBytes());
            throw new QuotaExceededException(String.format(
                    "Storage quota exceeded. Available: %d bytes, Required: %d bytes",
                    user.getStorageQuotaBytes() - user.getUsedStorageBytes(), fileSize));
        }

        // 2. Read bytes and calculate SHA-256 Checksum
        byte[] fileBytes;
        String checksumSha256;
        try {
            fileBytes = multipartFile.getBytes();
            checksumSha256 = ChecksumUtils.calculateSha256(fileBytes);
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file stream: " + e.getMessage(), e);
        }

        log.info("[BackupService] User '{}' uploading file '{}' (size={} bytes, SHA-256={})",
                user.getUsername(), sanitizedFilename, fileSize, checksumSha256);

        // 3. Check for existing file lineage (Versioning vs New File)
        Optional<BackupFile> existingFileOpt = fileRepository.findByUserAndFilenameAndIsDeletedFalse(user, sanitizedFilename);
        BackupFile targetFile;
        int nextVersionNumber;
        boolean isNewFile = false;

        if (existingFileOpt.isPresent()) {
            targetFile = existingFileOpt.get();
            // Idempotency check: if current version has identical checksum, reject duplicate upload
            if (targetFile.getCurrentVersion() != null &&
                    checksumSha256.equalsIgnoreCase(targetFile.getCurrentVersion().getChecksumSha256())) {
                log.info("[BackupService] Idempotency match: file '{}' version {} already has checksum {}",
                        sanitizedFilename, targetFile.getCurrentVersionNumber(), checksumSha256);
                throw new DuplicateFileException(String.format(
                        "File '%s' already exists with identical content (SHA-256: %s) in version %d. No new backup created.",
                        sanitizedFilename, checksumSha256, targetFile.getCurrentVersionNumber()));
            }
            nextVersionNumber = targetFile.getCurrentVersionNumber() + 1;
        } else {
            targetFile = new BackupFile(user, sanitizedFilename, contentType);
            nextVersionNumber = 1;
            isNewFile = true;
        }

        // 4. Record Backup History in-progress
        BackupHistory history = new BackupHistory(
                user,
                targetFile.getId(),
                null,
                sanitizedFilename,
                nextVersionNumber,
                fileSize,
                checksumSha256,
                BackupStatus.IN_PROGRESS,
                null,
                0
        );
        history = historyRepository.save(history);

        // 5. Generate Object Key and Upload to S3
        String s3Key = S3KeyGenerator.generateKey(user.getId(), targetFile.getId(), nextVersionNumber, sanitizedFilename);
        String s3Bucket = storageService.getDefaultBucket();
        String s3VersionId = null;

        Map<String, String> metadata = new HashMap<>();
        metadata.put("sha256-checksum", checksumSha256);
        metadata.put("original-filename", originalFilename);
        metadata.put("version-number", String.valueOf(nextVersionNumber));
        metadata.put("user-id", String.valueOf(user.getId()));

        try (InputStream uploadStream = new ByteArrayInputStream(fileBytes)) {
            s3VersionId = storageService.uploadObject(
                    s3Bucket,
                    s3Key,
                    uploadStream,
                    fileSize,
                    contentType,
                    metadata
            );
        } catch (Exception uploadEx) {
            history.setStatus(BackupStatus.FAILED);
            history.setFailureReason("S3 Upload Failed: " + uploadEx.getMessage());
            historyRepository.save(history);
            log.error("[BackupService] S3 upload failure for key '{}': {}", s3Key, uploadEx.getMessage());
            throw new StorageException("Failed to upload file to cloud storage: " + uploadEx.getMessage(), uploadEx);
        }

        // 6. Persist Metadata with Compensating Transaction Guard
        try {
            if (isNewFile) {
                targetFile = fileRepository.save(targetFile);
            }

            FileVersion version = new FileVersion(
                    targetFile,
                    nextVersionNumber,
                    s3Bucket,
                    s3Key,
                    checksumSha256,
                    fileSize,
                    contentType,
                    changeSummary != null && !changeSummary.isBlank() ? changeSummary : "Version " + nextVersionNumber + " upload"
            );
            version.setS3VersionId(s3VersionId);
            version.setVerificationStatus(VerificationStatus.VERIFIED);
            version.setLastVerifiedAt(Instant.now());

            version = versionRepository.save(version);

            targetFile.addVersion(version);
            targetFile.setStatus(FileStatus.ACTIVE);
            targetFile.setContentType(contentType);
            targetFile = fileRepository.save(targetFile);

            // Update user storage footprint
            user.setUsedStorageBytes(user.getUsedStorageBytes() + fileSize);
            userRepository.save(user);

            // Complete backup history
            history.setVersionId(version.getId());
            history.setStatus(BackupStatus.COMPLETED);
            historyRepository.save(history);

            log.info("[BackupService] Successfully backed up '{}' version {} (Key: '{}')",
                    sanitizedFilename, nextVersionNumber, s3Key);

            return FileResponse.fromEntity(targetFile);

        } catch (Exception dbEx) {
            // Compensating transaction: Clean up orphaned S3 object
            log.error("[BackupService] DB commit failed after S3 upload. Rolling back S3 object '{}'...", s3Key, dbEx);
            try {
                storageService.deleteObject(s3Bucket, s3Key);
                history.setStatus(BackupStatus.ROLLED_BACK);
                history.setFailureReason("Database transaction rollback: " + dbEx.getMessage());
                historyRepository.save(history);
            } catch (Exception cleanupEx) {
                log.error("[BackupService] Failed to clean up orphaned S3 object '{}': {}", s3Key, cleanupEx.getMessage());
            }
            throw new StorageException("Backup failed during metadata registration. S3 object rolled back: " + dbEx.getMessage(), dbEx);
        }
    }

    @Transactional(readOnly = true)
    public List<FileResponse> getUserFiles(User user, String searchKeyword) {
        List<BackupFile> files;
        if (searchKeyword != null && !searchKeyword.isBlank()) {
            files = fileRepository.searchByUserAndFilename(user, searchKeyword.trim());
        } else {
            files = fileRepository.findByUserAndIsDeletedFalseOrderByUpdatedAtDesc(user);
        }
        return files.stream().map(FileResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FileResponse getFileById(String fileId, User user) {
        BackupFile file = fileRepository.findByIdAndUser(fileId, user)
                .orElseThrow(() -> new ResourceNotFoundException("File not found or access denied (ID: " + fileId + ")"));
        return FileResponse.fromEntity(file);
    }

    @Transactional(readOnly = true)
    public List<FileVersionResponse> getFileVersions(String fileId, User user) {
        BackupFile file = fileRepository.findByIdAndUser(fileId, user)
                .orElseThrow(() -> new ResourceNotFoundException("File not found or access denied (ID: " + fileId + ")"));

        List<FileVersion> versions = versionRepository.findByFileOrderByVersionNumberDesc(file);
        return versions.stream().map(FileVersionResponse::fromEntity).collect(Collectors.toList());
    }
}
