package com.cloudbackup.platform.service;

import com.cloudbackup.platform.dto.response.BackupHistoryResponse;
import com.cloudbackup.platform.dto.response.DashboardStatsResponse;
import com.cloudbackup.platform.dto.response.RecoveryHistoryResponse;
import com.cloudbackup.platform.model.BackupHistory;
import com.cloudbackup.platform.model.FileStatus;
import com.cloudbackup.platform.model.RecoveryHistory;
import com.cloudbackup.platform.model.User;
import com.cloudbackup.platform.repository.BackupFileRepository;
import com.cloudbackup.platform.repository.BackupHistoryRepository;
import com.cloudbackup.platform.repository.RecoveryHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuditService {

    private final BackupFileRepository fileRepository;
    private final BackupHistoryRepository backupHistoryRepository;
    private final RecoveryHistoryRepository recoveryHistoryRepository;

    public AuditService(
            BackupFileRepository fileRepository,
            BackupHistoryRepository backupHistoryRepository,
            RecoveryHistoryRepository recoveryHistoryRepository) {
        this.fileRepository = fileRepository;
        this.backupHistoryRepository = backupHistoryRepository;
        this.recoveryHistoryRepository = recoveryHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<BackupHistoryResponse> getBackupHistory(User user) {
        List<BackupHistory> list = backupHistoryRepository.findByUserOrderByCreatedAtDesc(user);
        return list.stream().map(BackupHistoryResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RecoveryHistoryResponse> getRecoveryHistory(User user) {
        List<RecoveryHistory> list = recoveryHistoryRepository.findByUserOrderByRestoredAtDesc(user);
        return list.stream().map(RecoveryHistoryResponse::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(User user) {
        long totalFiles = fileRepository.countByUserAndIsDeletedFalse(user);
        long totalBackups = backupHistoryRepository.countByUser(user);
        Long usedBytes = fileRepository.sumTotalSizeBytesByUser(user);
        long totalStorageBytes = usedBytes != null ? usedBytes : 0L;
        long recoveryCount = recoveryHistoryRepository.countByUser(user);

        List<BackupHistory> recentBackups = backupHistoryRepository.findByUserOrderByCreatedAtDesc(user)
                .stream().limit(5).toList();
        List<RecoveryHistory> recentRecoveries = recoveryHistoryRepository.findByUserOrderByRestoredAtDesc(user)
                .stream().limit(5).toList();

        long corruptedCount = fileRepository.findByUserAndIsDeletedFalseOrderByUpdatedAtDesc(user)
                .stream().filter(f -> f.getStatus() == FileStatus.CORRUPTED).count();

        DashboardStatsResponse stats = new DashboardStatsResponse();
        stats.setTotalFiles(totalFiles);
        stats.setTotalBackups(totalBackups);
        stats.setTotalStorageBytes(totalStorageBytes);
        stats.setStorageQuotaBytes(user.getStorageQuotaBytes());
        stats.setSuccessfulRecoveries(recoveryCount);
        stats.setCorruptedCount(corruptedCount);
        stats.setRecentBackups(recentBackups.stream().map(BackupHistoryResponse::fromEntity).collect(Collectors.toList()));
        stats.setRecentRecoveries(recentRecoveries.stream().map(RecoveryHistoryResponse::fromEntity).collect(Collectors.toList()));

        return stats;
    }
}
