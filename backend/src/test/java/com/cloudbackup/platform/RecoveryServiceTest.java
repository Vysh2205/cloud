package com.cloudbackup.platform;

import com.cloudbackup.platform.dto.request.RestoreRequest;
import com.cloudbackup.platform.dto.response.FileResponse;
import com.cloudbackup.platform.exception.ChecksumMismatchException;
import com.cloudbackup.platform.model.*;
import com.cloudbackup.platform.repository.BackupFileRepository;
import com.cloudbackup.platform.repository.FileVersionRepository;
import com.cloudbackup.platform.repository.RecoveryHistoryRepository;
import com.cloudbackup.platform.repository.UserRepository;
import com.cloudbackup.platform.service.FileStorageService;
import com.cloudbackup.platform.service.RecoveryService;
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
@DisplayName("RecoveryService Restore & Fault-Tolerance Tests")
class RecoveryServiceTest {

    @Mock
    private BackupFileRepository fileRepository;

    @Mock
    private FileVersionRepository versionRepository;

    @Mock
    private RecoveryHistoryRepository recoveryHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService storageService;

    @InjectMocks
    private RecoveryService recoveryService;

    private User sampleUser;
    private BackupFile sampleFile;
    private FileVersion version1;
    private FileVersion version2;
    private byte[] v1Bytes;
    private String v1Checksum;

    @BeforeEach
    void setUp() {
        sampleUser = new User("disaster_recovery_admin", "dra@cloud.internal", "hash", Role.ROLE_USER);
        sampleUser.setId(10L);

        sampleFile = new BackupFile(sampleUser, "database.sql", "application/sql");

        v1Bytes = "CREATE TABLE users; INSERT INTO users VALUES (1);".getBytes(StandardCharsets.UTF_8);
        v1Checksum = ChecksumUtils.calculateSha256(v1Bytes);

        version1 = new FileVersion(sampleFile, 1, "distributed-cloud-backups", "key-v1", v1Checksum, (long) v1Bytes.length, "application/sql", "Initial schema");

        byte[] v2Bytes = "DROP TABLE users; -- Accidental drop".getBytes(StandardCharsets.UTF_8);
        String v2Checksum = ChecksumUtils.calculateSha256(v2Bytes);
        version2 = new FileVersion(sampleFile, 2, "distributed-cloud-backups", "key-v2", v2Checksum, (long) v2Bytes.length, "application/sql", "Accidental change");

        sampleFile.addVersion(version1);
        sampleFile.addVersion(version2); // currentVersion is version 2
    }

    @Test
    @DisplayName("Should restore previous version 1 and promote it as version 3 with intact integrity")
    void testRestoreVersion_Success() {
        RestoreRequest request = new RestoreRequest(true, "Rollback accidental table drop");

        when(fileRepository.findByIdAndUser(sampleFile.getId(), sampleUser))
                .thenReturn(Optional.of(sampleFile));
        when(versionRepository.findById(version1.getId()))
                .thenReturn(Optional.of(version1));
        when(storageService.downloadObjectStream(version1.getS3Bucket(), version1.getS3ObjectKey()))
                .thenReturn(new ByteArrayInputStream(v1Bytes));
        when(versionRepository.save(any(FileVersion.class))).thenAnswer(i -> i.getArgument(0));
        when(fileRepository.save(any(BackupFile.class))).thenAnswer(i -> i.getArgument(0));

        FileResponse response = recoveryService.restoreVersion(sampleFile.getId(), version1.getId(), request, sampleUser);

        assertNotNull(response);
        assertEquals(3, response.getCurrentVersionNumber());
        assertEquals(3, response.getTotalVersions());
        assertEquals(v1Checksum, response.getChecksumSha256());

        verify(recoveryHistoryRepository, times(1)).save(argThat(h ->
                h.getStatus() == RecoveryStatus.SUCCESS &&
                h.getChecksumVerified() &&
                h.getRestoredVersionNumber() == 1
        ));
    }

    @Test
    @DisplayName("Should prevent recovery if target version fails pre-recovery checksum check")
    void testRestoreVersion_AbortsOnCorruptedVersion() {
        RestoreRequest request = new RestoreRequest(true, "Emergency rollback");
        byte[] tamperedBytes = "Corrupted bits in storage".getBytes(StandardCharsets.UTF_8);

        when(fileRepository.findByIdAndUser(sampleFile.getId(), sampleUser))
                .thenReturn(Optional.of(sampleFile));
        when(versionRepository.findById(version1.getId()))
                .thenReturn(Optional.of(version1));
        when(storageService.downloadObjectStream(version1.getS3Bucket(), version1.getS3ObjectKey()))
                .thenReturn(new ByteArrayInputStream(tamperedBytes));

        assertThrows(ChecksumMismatchException.class, () ->
                recoveryService.restoreVersion(sampleFile.getId(), version1.getId(), request, sampleUser)
        );

        verify(recoveryHistoryRepository, times(1)).save(argThat(h ->
                h.getStatus() == RecoveryStatus.VERIFICATION_FAILED &&
                !h.getChecksumVerified()
        ));
    }
}
