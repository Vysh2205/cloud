package com.cloudbackup.platform;

import com.cloudbackup.platform.dto.response.FileResponse;
import com.cloudbackup.platform.exception.DuplicateFileException;
import com.cloudbackup.platform.exception.QuotaExceededException;
import com.cloudbackup.platform.exception.ResourceNotFoundException;
import com.cloudbackup.platform.model.*;
import com.cloudbackup.platform.repository.BackupFileRepository;
import com.cloudbackup.platform.repository.BackupHistoryRepository;
import com.cloudbackup.platform.repository.FileVersionRepository;
import com.cloudbackup.platform.repository.UserRepository;
import com.cloudbackup.platform.service.FileBackupService;
import com.cloudbackup.platform.service.FileStorageService;
import com.cloudbackup.platform.util.ChecksumUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileBackupService Upload, Versioning, and Idempotency Tests")
class FileBackupServiceTest {

    @Mock
    private BackupFileRepository fileRepository;

    @Mock
    private FileVersionRepository versionRepository;

    @Mock
    private BackupHistoryRepository historyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService storageService;

    @InjectMocks
    private FileBackupService backupService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("devops_lead", "devops@cloud.internal", "hash", Role.ROLE_USER);
        sampleUser.setId(1L);
        sampleUser.setStorageQuotaBytes(10485760L); // 10 MB
        sampleUser.setUsedStorageBytes(0L);
    }

    @Test
    @DisplayName("Should successfully upload new file, calculate SHA-256, create v1 and S3 object")
    void testUploadNewFile_Success() throws Exception {
        byte[] content = "Production DB Cluster Config v1.0".getBytes(StandardCharsets.UTF_8);
        String expectedChecksum = ChecksumUtils.calculateSha256(content);
        MockMultipartFile file = new MockMultipartFile("file", "db-config.yaml", "text/yaml", content);

        when(fileRepository.findByUserAndFilenameAndIsDeletedFalse(sampleUser, "db-config.yaml"))
                .thenReturn(Optional.empty());
        when(storageService.getDefaultBucket()).thenReturn("distributed-cloud-backups");
        when(storageService.uploadObject(anyString(), anyString(), any(), anyLong(), anyString(), anyMap()))
                .thenReturn("s3-version-id-1001");
        when(historyRepository.save(any(BackupHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileRepository.save(any(BackupFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.save(any(FileVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileResponse response = backupService.uploadFile(file, "Initial backup", sampleUser);

        assertNotNull(response);
        assertEquals("db-config.yaml", response.getFilename());
        assertEquals(1, response.getCurrentVersionNumber());
        assertEquals(expectedChecksum, response.getChecksumSha256());

        verify(storageService, times(1)).uploadObject(
                eq("distributed-cloud-backups"),
                contains("users/1/files/"),
                any(),
                eq((long) content.length),
                eq("text/yaml"),
                anyMap()
        );
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("Should automatically create version 2 when uploading updated file content")
    void testUploadUpdatedFile_IncrementsVersion() {
        byte[] v1Content = "Initial Content".getBytes(StandardCharsets.UTF_8);
        String v1Checksum = ChecksumUtils.calculateSha256(v1Content);

        BackupFile existingFile = new BackupFile(sampleUser, "app.env", "text/plain");
        FileVersion v1 = new FileVersion(existingFile, 1, "distributed-cloud-backups", "key-v1", v1Checksum, (long) v1Content.length, "text/plain", "v1");
        existingFile.addVersion(v1);

        byte[] v2Content = "Updated Content with JWT Secret".getBytes(StandardCharsets.UTF_8);
        String v2Checksum = ChecksumUtils.calculateSha256(v2Content);
        MockMultipartFile v2File = new MockMultipartFile("file", "app.env", "text/plain", v2Content);

        when(fileRepository.findByUserAndFilenameAndIsDeletedFalse(sampleUser, "app.env"))
                .thenReturn(Optional.of(existingFile));
        when(storageService.getDefaultBucket()).thenReturn("distributed-cloud-backups");
        when(storageService.uploadObject(anyString(), anyString(), any(), anyLong(), anyString(), anyMap()))
                .thenReturn("s3-v2-id");
        when(historyRepository.save(any(BackupHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(fileRepository.save(any(BackupFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionRepository.save(any(FileVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FileResponse response = backupService.uploadFile(v2File, "Updated configs", sampleUser);

        assertNotNull(response);
        assertEquals(2, response.getCurrentVersionNumber());
        assertEquals(2, response.getTotalVersions());
        assertEquals(v2Checksum, response.getChecksumSha256());
    }

    @Test
    @DisplayName("Should reject duplicate upload with identical SHA-256 checksum (Idempotency)")
    void testUploadDuplicate_IdempotencyRejected() {
        byte[] content = "Static unchanging dataset".getBytes(StandardCharsets.UTF_8);
        String checksum = ChecksumUtils.calculateSha256(content);

        BackupFile existingFile = new BackupFile(sampleUser, "dataset.csv", "text/csv");
        FileVersion v1 = new FileVersion(existingFile, 1, "distributed-cloud-backups", "key-v1", checksum, (long) content.length, "text/csv", "v1");
        existingFile.addVersion(v1);

        MockMultipartFile duplicateFile = new MockMultipartFile("file", "dataset.csv", "text/csv", content);

        when(fileRepository.findByUserAndFilenameAndIsDeletedFalse(sampleUser, "dataset.csv"))
                .thenReturn(Optional.of(existingFile));

        assertThrows(DuplicateFileException.class, () ->
                backupService.uploadFile(duplicateFile, "Redundant upload", sampleUser)
        );

        verify(storageService, never()).uploadObject(anyString(), anyString(), any(), anyLong(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Should prevent upload if file size exceeds user storage quota")
    void testUpload_QuotaExceeded() {
        sampleUser.setUsedStorageBytes(9 * 1024 * 1024L); // 9 MB used
        byte[] largeContent = new byte[2 * 1024 * 1024]; // 2 MB requested -> total 11MB > 10MB quota

        MockMultipartFile file = new MockMultipartFile("file", "heavy.iso", "application/octet-stream", largeContent);

        assertThrows(QuotaExceededException.class, () ->
                backupService.uploadFile(file, "Too big", sampleUser)
        );
    }

    @Test
    @DisplayName("Multi-tenant Isolation: User cannot access another user's file")
    void testMultiTenantIsolation_AccessDenied() {
        when(fileRepository.findByIdAndUser("secret-file-99", sampleUser))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                backupService.getFileById("secret-file-99", sampleUser)
        );
    }
}
