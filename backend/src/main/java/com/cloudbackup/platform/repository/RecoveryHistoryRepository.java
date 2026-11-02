package com.cloudbackup.platform.repository;

import com.cloudbackup.platform.model.RecoveryHistory;
import com.cloudbackup.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecoveryHistoryRepository extends JpaRepository<RecoveryHistory, Long> {

    List<RecoveryHistory> findByUserOrderByRestoredAtDesc(User user);

    long countByUser(User user);
}
