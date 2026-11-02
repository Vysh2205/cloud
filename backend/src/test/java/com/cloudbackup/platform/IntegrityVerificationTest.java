package com.cloudbackup.platform;

import com.cloudbackup.platform.dto.response.IntegrityVerificationResponse;
import com.cloudbackup.platform.exception.ResourceNotFoundException;
import com.cloudbackup.platform.exception.StorageException;
import com.cloudbackup.platform.model.*;
import com.cloudbackup.platform.repository.BackupFileRepository;
import com.cloudbackup.platform.repository.FileVersionRepository;
import com.cloudbackup.platform.service.FileStorageService;
import com.cloudbackup.platform.service.IntegrityService;
import com.cloudbackup.platform.util.ChecksumUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntegrityService Cryptographic Verification Tests")
class IntegrityVerificationTest {

    @Mock
    private BackupFileRepository fileRepository;

    @Mock
    private FileVersionRepository versionRepository;

    @Mock
    private FileStorageService storageService;

    @InjectMocks
    private IntegrityService integrityService;

    private User sampleUser;
    private BackupFile sampleFile;
    private FileVersion sampleVersion;
    private byte[] originalBytes;
    private String originalChecksum;

    @BeforeEach
    void setUp() {
        sampleUser = new User("secops", "secops@cloud.internal", "hash", Role.ROLE_USER);
        sampleUser.setId(2L);

        originalBytes = "Authentic Cloud Data Packet".getBytes(StandardCharsets.UTF_8);
        originalChecksum = ChecksumUtils.calculateSha256(originalBytes);

        sampleFile = new BackupFile(sampleUser, "system.log", "text/plain");
        sampleVersion = new FileVersion(
                sampleFile,
                1,
                "distributed-cloud-backups",
                "users/2/files/" + sampleFile.getId() + "/v1/system.log",
                originalChecksum,
                (long) originalBytes.length,
                "text/plain",
                "Initial log"
        );
        sampleFile.addVersion(sampleVersion);
    }

    @Test
    @DisplayName("Should confirm integrity when S3 stream matches stored SHA-256")
    void testVerifyIntegrity_Match() {
        when(fileRepository.findByIdAndUser(sampleFile.getId(), sampleUser))
                .thenReturn(Optional.of(sampleFile));
        when(storageService.downloadObjectStream(sampleVersion.getS3Bucket(), sampleVersion.getS3ObjectKey()))
                .thenReturn(new ByteArrayInputStream(originalBytes));
        when(versionRepository.save(any(FileVersion.class))).thenAnswer(i -> i.getArgument(0));

        IntegrityVerificationResponse response = integrityService.verifyFileVersionIntegrity(
                sampleFile.getId(), sampleVersion.getId(), sampleUser
        );

        assertNotNull(response);
        assertTrue(response.isVerified());
        assertEquals(VerificationStatus.VERIFIED, response.getStatus());
        assertEquals(originalChecksum, response.getExpectedChecksum());
        assertEquals(originalChecksum, response.getActualChecksum());
        verify(versionRepository, times(1)).save(sampleVersion);
    }

    @Test
    @DisplayName("Should detect file corruption when S3 stream has modified/tampered bytes")
    void testVerifyIntegrity_DetectCorruption() {
        byte[] corruptedBytes = "Corrupted Cloud Data Packet (Bit-rot)".getBytes(StandardCharsets.UTF_8);
        String corruptedChecksum = ChecksumUtils.calculateSha256(corruptedBytes);

        when(fileRepository.findByIdAndUser(sampleFile.getId(), sampleUser))
                .thenReturn(Optional.of(sampleFile));
        when(storageService.downloadObjectStream(sampleVersion.getS3Bucket(), sampleVersion.getS3ObjectKey()))
                .thenReturn(new ByteArrayInputStream(corruptedBytes));
        when(versionRepository.save(any(FileVersion.class))).thenAnswer(i -> i.getArgument(0));

        IntegrityVerificationResponse response = integrityService.verifyFileVersionIntegrity(
                sampleFile.getId(), sampleVersion.getId(), sampleUser
        );

        assertNotNull(response);
        assertFalse(response.isVerified());
        assertEquals(VerificationStatus.CORRUPTED, response.getStatus());
        assertEquals(originalChecksum, response.getExpectedChecksum());
        assertEquals(corruptedChecksum, response.getActualChecksum());
        assertEquals(FileStatus.CORRUPTED, sampleFile.getStatus());
        verify(fileRepository, times(1)).save(sampleFile);
    }

    @Test
    @DisplayName("Should handle missing S3 object gracefully without crashing")
    void testVerifyIntegrity_MissingObject() {
        when(fileRepository.findByIdAndUser(sampleFile.getId(), sampleUser))
                .thenReturn(Optional.of(sampleFile));
        when(storageService.downloadObjectStream(sampleVersion.getS3Bucket(), sampleVersion.getS3ObjectKey()))
                .thenThrow(new StorageException("S3 404 NoSuchKey"));
        when(versionRepository.save(any(FileVersion.class))).thenAnswer(i -> i.getArgument(0));

        IntegrityVerificationResponse response = integrityService.verifyFileVersionIntegrity(
                sampleFile.getId(), sampleVersion.getId(), sampleUser
        );

        assertNotNull(response);
        assertFalse(response.isVerified());
        assertEquals(VerificationStatus.CORRUPTED, response.getStatus());
        assertTrue(response.getMessage().contains("NoSuchKey"));
    }
}
