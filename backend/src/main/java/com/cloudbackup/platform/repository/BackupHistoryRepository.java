package com.cloudbackup.platform.repository;

import com.cloudbackup.platform.model.BackupHistory;
import com.cloudbackup.platform.model.BackupStatus;
import com.cloudbackup.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BackupHistoryRepository extends JpaRepository<BackupHistory, Long> {

    List<BackupHistory> findByUserOrderByCreatedAtDesc(User user);

    long countByUser(User user);

    long countByUserAndStatus(User user, BackupStatus status);
}
