package com.cloudbackup.platform.controller;

import com.cloudbackup.platform.dto.response.ApiResponse;
import com.cloudbackup.platform.dto.response.BackupHistoryResponse;
import com.cloudbackup.platform.dto.response.DashboardStatsResponse;
import com.cloudbackup.platform.dto.response.RecoveryHistoryResponse;
import com.cloudbackup.platform.model.User;
import com.cloudbackup.platform.service.AuditService;
import com.cloudbackup.platform.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AuditController {

    private final AuditService auditService;
    private final AuthService authService;

    public AuditController(AuditService auditService, AuthService authService) {
        this.auditService = auditService;
        this.authService = authService;
    }

    /**
     * Retrieve complete backup audit history for the authenticated user.
     */
    @GetMapping("/backup/history")
    public ResponseEntity<ApiResponse<List<BackupHistoryResponse>>> getBackupHistory() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        List<BackupHistoryResponse> history = auditService.getBackupHistory(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    /**
     * Retrieve complete recovery and restore audit history for the authenticated user.
     */
    @GetMapping("/recovery/history")
    public ResponseEntity<ApiResponse<List<RecoveryHistoryResponse>>> getRecoveryHistory() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        List<RecoveryHistoryResponse> history = auditService.getRecoveryHistory(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    /**
     * Retrieve aggregated dashboard statistics (storage usage, file counts, recent logs).
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        User currentUser = authService.getCurrentAuthenticatedUser();
        DashboardStatsResponse stats = auditService.getDashboardStats(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
