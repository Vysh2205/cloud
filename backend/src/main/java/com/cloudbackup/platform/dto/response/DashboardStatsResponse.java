package com.cloudbackup.platform.dto.response;

import java.util.List;

public class DashboardStatsResponse {

    private long totalFiles;
    private long totalBackups;
    private long totalStorageBytes;
    private long storageQuotaBytes;
    private long successfulRecoveries;
    private long corruptedCount;
    private List<BackupHistoryResponse> recentBackups;
    private List<RecoveryHistoryResponse> recentRecoveries;

    public DashboardStatsResponse() {}

    public long getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(long totalFiles) {
        this.totalFiles = totalFiles;
    }

    public long getTotalBackups() {
        return totalBackups;
    }

    public void setTotalBackups(long totalBackups) {
        this.totalBackups = totalBackups;
    }

    public long getTotalStorageBytes() {
        return totalStorageBytes;
    }

    public void setTotalStorageBytes(long totalStorageBytes) {
        this.totalStorageBytes = totalStorageBytes;
    }

    public long getStorageQuotaBytes() {
        return storageQuotaBytes;
    }

    public void setStorageQuotaBytes(long storageQuotaBytes) {
        this.storageQuotaBytes = storageQuotaBytes;
    }

    public long getSuccessfulRecoveries() {
        return successfulRecoveries;
    }

    public void setSuccessfulRecoveries(long successfulRecoveries) {
        this.successfulRecoveries = successfulRecoveries;
    }

    public long getCorruptedCount() {
        return corruptedCount;
    }

    public void setCorruptedCount(long corruptedCount) {
        this.corruptedCount = corruptedCount;
    }

    public List<BackupHistoryResponse> getRecentBackups() {
        return recentBackups;
    }

    public void setRecentBackups(List<BackupHistoryResponse> recentBackups) {
        this.recentBackups = recentBackups;
    }

    public List<RecoveryHistoryResponse> getRecentRecoveries() {
        return recentRecoveries;
    }

    public void setRecentRecoveries(List<RecoveryHistoryResponse> recentRecoveries) {
        this.recentRecoveries = recentRecoveries;
    }
}
