package com.cloudbackup.platform.repository;

import com.cloudbackup.platform.model.BackupFile;
import com.cloudbackup.platform.model.FileVersion;
import com.cloudbackup.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, String> {

    List<FileVersion> findByFileOrderByVersionNumberDesc(BackupFile file);

    List<FileVersion> findByFile_IdOrderByVersionNumberDesc(String fileId);

    Optional<FileVersion> findByIdAndFile_User(String id, User user);

    Optional<FileVersion> findByFileAndVersionNumber(BackupFile file, Integer versionNumber);

    Optional<FileVersion> findTopByFileOrderByVersionNumberDesc(BackupFile file);

    @Query("SELECT v FROM FileVersion v WHERE v.file.id = :fileId AND v.file.user = :user ORDER BY v.versionNumber DESC")
    List<FileVersion> findByFileIdAndUserOrderByVersionNumberDesc(@Param("fileId") String fileId, @Param("user") User user);
}
