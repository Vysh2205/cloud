package com.cloudbackup.platform.service;

import com.cloudbackup.platform.dto.request.RestoreRequest;
import com.cloudbackup.platform.dto.response.FileResponse;
import com.cloudbackup.platform.exception.ChecksumMismatchException;
import com.cloudbackup.platform.exception.ResourceNotFoundException;
import com.cloudbackup.platform.exception.StorageException;
import com.cloudbackup.platform.model.*;
import com.cloudbackup.platform.repository.BackupFileRepository;
import com.cloudbackup.platform.repository.FileVersionRepository;
import com.cloudbackup.platform.repository.RecoveryHistoryRepository;
import com.cloudbackup.platform.repository.UserRepository;
import com.cloudbackup.platform.util.ChecksumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;

@Service
public class RecoveryService {

    private static final Logger log = LoggerFactory.getLogger(RecoveryService.class);

    private final BackupFileRepository fileRepository;
    private final FileVersionRepository versionRepository;
    private final RecoveryHistoryRepository recoveryHistoryRepository;
    private final UserRepository userRepository;
    private final FileStorageService storageService;

    public RecoveryService(
            BackupFileRepository fileRepository,
            FileVersionRepository versionRepository,
            RecoveryHistoryRepository recoveryHistoryRepository,
            UserRepository userRepository,
            FileStorageService storageService) {
        this.fileRepository = fileRepository;
        this.versionRepository = versionRepository;
        this.recoveryHistoryRepository = recoveryHistoryRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    /**
     * Restores a previously uploaded version of a file.
     * Verifies cryptographic integrity before promoting or restoring the version.
     */
    @Transactional
    public FileResponse restoreVersion(String fileId, String versionId, RestoreRequest request, User user) {
        log.info("[RecoveryService] User '{}' requesting restore for fileId='{}' to versionId='{}'",
                user.getUsername(), fileId, versionId);

        BackupFile file = fileRepository.findByIdAndUser(fileId, user)
                .orElseThrow(() -> new ResourceNotFoundException("File not found or access denied (ID: " + fileId + ")"));

        FileVersion targetVersion = versionRepository.findById(versionId)
                .filter(v -> v.getFile().getId().equals(fileId))
                .orElseThrow(() -> new ResourceNotFoundException("Version not found (ID: " + versionId + ")"));

        // Step 1: Pre-recovery Integrity Verification
        byte[] objectBytes;
        String recalculatedChecksum;
        try (InputStream s3Stream = storageService.downloadObjectStream(targetVersion.getS3Bucket(), targetVersion.getS3ObjectKey())) {
            objectBytes = s3Stream.readAllBytes();
            recalculatedChecksum = ChecksumUtils.calculateSha256(objectBytes);
        } catch (Exception ex) {
            recordRecoveryHistory(user, fileId, versionId, targetVersion.getVersionNumber(),
                    file.getFilename(), RecoveryStatus.FAILED, false, targetVersion.getChecksumSha256(),
                    null, "Failed to read S3 object: " + ex.getMessage());
            throw new StorageException("Recovery aborted: cannot read version data from storage: " + ex.getMessage(), ex);
        }

        if (!recalculatedChecksum.equalsIgnoreCase(targetVersion.getChecksumSha256())) {
            log.error("[RecoveryService] ABORT: Pre-recovery checksum mismatch! Expected: {}, Found: {}",
                    targetVersion.getChecksumSha256(), recalculatedChecksum);

            targetVersion.setVerificationStatus(VerificationStatus.CORRUPTED);
            versionRepository.save(targetVersion);

            recordRecoveryHistory(user, fileId, versionId, targetVersion.getVersionNumber(),
                    file.getFilename(), RecoveryStatus.VERIFICATION_FAILED, false,
                    targetVersion.getChecksumSha256(), recalculatedChecksum, "Checksum mismatch detected during pre-recovery");

            throw new ChecksumMismatchException("Recovery failed: Target file version failed integrity check.",
                    targetVersion.getChecksumSha256(), recalculatedChecksum);
        }

        // Step 2: Restore / Promote Version
        boolean promote = request == null || request.isPromoteToCurrentVersion();
        if (promote) {
            int newVersionNumber = file.getCurrentVersionNumber() + 1;
            log.info("[RecoveryService] Promoting restored content as new version v{} for file '{}'",
                    newVersionNumber, file.getFilename());

            // Create new immutable version reflecting the restoration
            FileVersion restoredVersion = new FileVersion(
                    file,
                    newVersionNumber,
                    targetVersion.getS3Bucket(),
                    targetVersion.getS3ObjectKey(),
                    targetVersion.getChecksumSha256(),
                    targetVersion.getFileSizeBytes(),
                    targetVersion.getContentType(),
                    "Restored from version " + targetVersion.getVersionNumber() +
                            (request != null && request.getRestoreReason() != null ? " (" + request.getRestoreReason() + ")" : "")
            );
            restoredVersion.setS3VersionId(targetVersion.getS3VersionId());
            restoredVersion.setVerificationStatus(VerificationStatus.VERIFIED);
            restoredVersion.setLastVerifiedAt(Instant.now());

            restoredVersion = versionRepository.save(restoredVersion);

            file.addVersion(restoredVersion);
            file.setStatus(FileStatus.ACTIVE);
            file = fileRepository.save(file);
        }

        // Step 3: Record successful recovery audit entry
        recordRecoveryHistory(user, fileId, versionId, targetVersion.getVersionNumber(),
                file.getFilename(), RecoveryStatus.SUCCESS, true, targetVersion.getChecksumSha256(),
                recalculatedChecksum, null);

        log.info("[RecoveryService] Version v{} restored successfully for file '{}'",
                targetVersion.getVersionNumber(), file.getFilename());

        return FileResponse.fromEntity(file);
    }

    /**
     * Downloads file content stream with integrity verification check.
     */
    @Transactional(readOnly = true)
    public DownloadPayload downloadFile(String fileId, String versionId, User user) {
        BackupFile file = fileRepository.findByIdAndUser(fileId, user)
                .orElseThrow(() -> new ResourceNotFoundException("File not found or access denied (ID: " + fileId + ")"));

        FileVersion version;
        if (versionId != null && !versionId.isBlank()) {
            version = versionRepository.findById(versionId)
                    .filter(v -> v.getFile().getId().equals(fileId))
                    .orElseThrow(() -> new ResourceNotFoundException("Version not found (ID: " + versionId + ")"));
        } else {
            version = file.getCurrentVersion();
            if (version == null) {
                throw new ResourceNotFoundException("No current version available to download.");
            }
        }

        try {
            InputStream s3Stream = storageService.downloadObjectStream(version.getS3Bucket(), version.getS3ObjectKey());
            byte[] fileBytes = s3Stream.readAllBytes();

            // Verify checksum before client delivery
            String verifiedChecksum = ChecksumUtils.calculateSha256(fileBytes);
            if (!verifiedChecksum.equalsIgnoreCase(version.getChecksumSha256())) {
                log.warn("[RecoveryService] Checksum mismatch during download for file '{}' v{}. Expected: {}, Got: {}",
                        file.getFilename(), version.getVersionNumber(), version.getChecksumSha256(), verifiedChecksum);
            }

            Resource resource = new InputStreamResource(new ByteArrayInputStream(fileBytes));
            return new DownloadPayload(resource, file.getFilename(), version.getContentType(), version.getFileSizeBytes(), version.getChecksumSha256());

        } catch (Exception ex) {
            log.error("[RecoveryService] Failed to stream download for file '{}': {}", file.getFilename(), ex.getMessage());
            throw new StorageException("Failed to download file from cloud storage: " + ex.getMessage(), ex);
        }
    }

    private void recordRecoveryHistory(User user, String fileId, String versionId, int versionNumber,
                                       String filename, RecoveryStatus status, boolean verified,
                                       String expectedChecksum, String actualChecksum, String failureReason) {
        RecoveryHistory history = new RecoveryHistory(
                user,
                fileId,
                versionId,
                versionNumber,
                filename,
                status,
                verified,
                expectedChecksum,
                actualChecksum,
                failureReason
        );
        recoveryHistoryRepository.save(history);
    }

    public static class DownloadPayload {
        private final Resource resource;
        private final String filename;
        private final String contentType;
        private final long contentLength;
        private final String checksumSha256;

        public DownloadPayload(Resource resource, String filename, String contentType, long contentLength, String checksumSha256) {
            this.resource = resource;
            this.filename = filename;
            this.contentType = contentType;
            this.contentLength = contentLength;
            this.checksumSha256 = checksumSha256;
        }

        public Resource getResource() {
            return resource;
        }

        public String getFilename() {
            return filename;
        }

        public String getContentType() {
            return contentType;
        }

        public long getContentLength() {
            return contentLength;
        }

        public String getChecksumSha256() {
            return checksumSha256;
        }
    }
}
