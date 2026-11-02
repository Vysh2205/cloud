package com.cloudbackup.platform.repository;

import com.cloudbackup.platform.model.BackupFile;
import com.cloudbackup.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupFileRepository extends JpaRepository<BackupFile, String> {

    List<BackupFile> findByUserAndIsDeletedFalseOrderByUpdatedAtDesc(User user);

    Optional<BackupFile> findByIdAndUser(String id, User user);

    Optional<BackupFile> findByUserAndFilenameAndIsDeletedFalse(User user, String filename);

    long countByUserAndIsDeletedFalse(User user);

    @Query("SELECT COALESCE(SUM(f.totalSizeBytes), 0) FROM BackupFile f WHERE f.user = :user AND f.isDeleted = false")
    Long sumTotalSizeBytesByUser(@Param("user") User user);

    @Query("SELECT f FROM BackupFile f WHERE f.user = :user AND f.isDeleted = false AND LOWER(f.filename) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY f.updatedAt DESC")
    List<BackupFile> searchByUserAndFilename(@Param("user") User user, @Param("keyword") String keyword);
}
