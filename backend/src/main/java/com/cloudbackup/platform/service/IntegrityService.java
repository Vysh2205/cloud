package com.cloudbackup.platform.service;

import com.cloudbackup.platform.dto.response.IntegrityVerificationResponse;
import com.cloudbackup.platform.exception.ResourceNotFoundException;
import com.cloudbackup.platform.model.*;
import com.cloudbackup.platform.repository.BackupFileRepository;
import com.cloudbackup.platform.repository.FileVersionRepository;
import com.cloudbackup.platform.util.ChecksumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;

@Service
public class IntegrityService {

    private static final Logger log = LoggerFactory.getLogger(IntegrityService.class);

    private final BackupFileRepository fileRepository;
    private final FileVersionRepository versionRepository;
    private final FileStorageService storageService;

    public IntegrityService(
            BackupFileRepository fileRepository,
            FileVersionRepository versionRepository,
            FileStorageService storageService) {
        this.fileRepository = fileRepository;
        this.versionRepository = versionRepository;
        this.storageService = storageService;
    }

    /**
     * Verifies cryptographic integrity of a file version by recalculating
     * its SHA-256 checksum from cloud storage and comparing against stored metadata.
     */
    @Transactional
    public IntegrityVerificationResponse verifyFileVersionIntegrity(String fileId, String versionId, User user) {
        log.info("[IntegrityService] Verifying integrity for fileId='{}', versionId='{}', user='{}'",
                fileId, versionId, user.getUsername());

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
                throw new ResourceNotFoundException("No current version available for file: " + fileId);
            }
        }

        String expectedChecksum = version.getChecksumSha256();
        String actualChecksum;

        try (InputStream s3Stream = storageService.downloadObjectStream(version.getS3Bucket(), version.getS3ObjectKey())) {
            actualChecksum = ChecksumUtils.calculateSha256(s3Stream);
        } catch (Exception ex) {
            log.error("[IntegrityService] Failed to stream object from S3 for verification: {}", ex.getMessage());
            version.setVerificationStatus(VerificationStatus.CORRUPTED);
            version.setLastVerifiedAt(Instant.now());
            versionRepository.save(version);

            return new IntegrityVerificationResponse(
                    file.getId(),
                    version.getId(),
                    file.getFilename(),
                    version.getVersionNumber(),
                    expectedChecksum,
                    "UNAVAILABLE",
                    false,
                    VerificationStatus.CORRUPTED,
                    "Cloud storage read failed or object missing: " + ex.getMessage()
            );
        }

        boolean verified = expectedChecksum.equalsIgnoreCase(actualChecksum);
        VerificationStatus status = verified ? VerificationStatus.VERIFIED : VerificationStatus.CORRUPTED;

        version.setVerificationStatus(status);
        version.setLastVerifiedAt(Instant.now());
        versionRepository.save(version);

        if (!verified) {
            file.setStatus(FileStatus.CORRUPTED);
            fileRepository.save(file);
            log.error("[IntegrityService] CORRUPTION DETECTED in file '{}' v{}. Expected={}, Actual={}",
                    file.getFilename(), version.getVersionNumber(), expectedChecksum, actualChecksum);
        } else {
            if (file.getStatus() == FileStatus.CORRUPTED) {
                file.setStatus(FileStatus.ACTIVE);
                fileRepository.save(file);
            }
            log.info("[IntegrityService] Integrity verified successfully for file '{}' v{}. SHA-256 match: {}",
                    file.getFilename(), version.getVersionNumber(), actualChecksum);
        }

        String message = verified
                ? "Cryptographic SHA-256 checksum verified: file is intact and tamper-free."
                : "INTEGRITY ALERT: SHA-256 checksum mismatch detected! File may be altered or corrupted.";

        return new IntegrityVerificationResponse(
                file.getId(),
                version.getId(),
                file.getFilename(),
                version.getVersionNumber(),
                expectedChecksum,
                actualChecksum,
                verified,
                status,
                message
        );
    }
}
