package com.cloudbackup.platform.controller;

import com.cloudbackup.platform.dto.request.RestoreRequest;
import com.cloudbackup.platform.dto.response.ApiResponse;
import com.cloudbackup.platform.dto.response.FileResponse;
import com.cloudbackup.platform.dto.response.FileVersionResponse;
import com.cloudbackup.platform.dto.response.IntegrityVerificationResponse;
import com.cloudbackup.platform.model.User;
import com.cloudbackup.platform.service.AuthService;
import com.cloudbackup.platform.service.FileBackupService;
import com.cloudbackup.platform.service.IntegrityService;
import com.cloudbackup.platform.service.RecoveryService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileBackupService backupService;
    private final IntegrityService integrityService;
    private final RecoveryService recoveryService;
    private final AuthService authService;

    public FileController(
            FileBackupService backupService,
            IntegrityService integrityService,
            RecoveryService recoveryService,
            AuthService authService) {
        this.backupService = backupService;
        this.integrityService = integrityService;
        this.recoveryService = recoveryService;
        this.authService = authService;
    }

    /**
     * Upload a new file or back up a new version of an existing file.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "changeSummary", required = false) String changeSummary) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        FileResponse response = backupService.uploadFile(file, changeSummary, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("File uploaded and backed up successfully", response));
    }

    /**
     * Retrieve all files belonging to the authenticated user (supports search filtering).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileResponse>>> listFiles(
            @RequestParam(value = "search", required = false) String search) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        List<FileResponse> files = backupService.getUserFiles(currentUser, search);
        return ResponseEntity.ok(ApiResponse.ok(files));
    }

    /**
     * Retrieve metadata for a specific file.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileResponse>> getFileById(@PathVariable("id") String fileId) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        FileResponse file = backupService.getFileById(fileId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(file));
    }

    /**
     * Retrieve all historical versions for a file.
     */
    @GetMapping("/{id}/versions")
    public ResponseEntity<ApiResponse<List<FileVersionResponse>>> getFileVersions(@PathVariable("id") String fileId) {
        User currentUser = authService.getCurrentAuthenticatedUser();
        List<FileVersionResponse> versions = backupService.getFileVersions(fileId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(versions));
    }

    /**
     * Verify cryptographic SHA-256 integrity of a file against cloud storage.
     */
    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<IntegrityVerificationResponse>> verifyIntegrity(
            @PathVariable("id") String fileId,
            @RequestParam(value = "versionId", required = false) String versionId) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        IntegrityVerificationResponse result = integrityService.verifyFileVersionIntegrity(fileId, versionId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Integrity verification completed", result));
    }

    /**
     * Restore a previous version of a file.
     */
    @PostMapping("/{id}/restore/{versionId}")
    public ResponseEntity<ApiResponse<FileResponse>> restoreVersion(
            @PathVariable("id") String fileId,
            @PathVariable("versionId") String versionId,
            @RequestBody(required = false) RestoreRequest request) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        FileResponse response = recoveryService.restoreVersion(fileId, versionId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok("File restored successfully to version " + response.getCurrentVersionNumber(), response));
    }

    /**
     * Download a file (defaults to current version, or specific version if versionId is provided).
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable("id") String fileId,
            @RequestParam(value = "versionId", required = false) String versionId) {

        User currentUser = authService.getCurrentAuthenticatedUser();
        RecoveryService.DownloadPayload payload = recoveryService.downloadFile(fileId, versionId, currentUser);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(payload.getContentType()))
                .contentLength(payload.getContentLength())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.getFilename() + "\"")
                .header("X-Checksum-SHA256", payload.getChecksumSha256())
                .body(payload.getResource());
    }
}
