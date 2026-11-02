-- =====================================================================
-- Distributed Cloud Backup & Recovery Platform
-- Flyway Database Migration: V1__init_schema.sql
-- PostgreSQL DDL with proper constraints, foreign keys, and indexes
-- =====================================================================

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    storage_quota_bytes BIGINT NOT NULL DEFAULT 5368709120, -- 5 GB default quota
    used_storage_bytes BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 2. Files (Logical File Entity owned by a user)
CREATE TABLE IF NOT EXISTS files (
    id VARCHAR(36) PRIMARY KEY, -- UUID
    user_id BIGINT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL DEFAULT 'application/octet-stream',
    current_version_id VARCHAR(36), -- references file_versions(id)
    current_version_number INT NOT NULL DEFAULT 1,
    total_versions INT NOT NULL DEFAULT 1,
    total_size_bytes BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, DELETED, ARCHIVED, CORRUPTED
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_files_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_filename UNIQUE (user_id, filename)
);

CREATE INDEX IF NOT EXISTS idx_files_user_id ON files(user_id);
CREATE INDEX IF NOT EXISTS idx_files_filename ON files(filename);
CREATE INDEX IF NOT EXISTS idx_files_status ON files(status);

-- 3. File Versions (Append-only immutable file revisions in S3)
CREATE TABLE IF NOT EXISTS file_versions (
    id VARCHAR(36) PRIMARY KEY, -- UUID
    file_id VARCHAR(36) NOT NULL,
    version_number INT NOT NULL,
    s3_bucket VARCHAR(100) NOT NULL,
    s3_object_key VARCHAR(500) NOT NULL,
    s3_version_id VARCHAR(100), -- AWS S3 object versionId if S3 bucket versioning is active
    checksum_sha256 VARCHAR(64) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL DEFAULT 'application/octet-stream',
    change_summary VARCHAR(255),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'VERIFIED', -- VERIFIED, UNVERIFIED, CORRUPTED
    last_verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_file_versions_file FOREIGN KEY (file_id) REFERENCES files(id) ON DELETE CASCADE,
    CONSTRAINT uq_file_version UNIQUE (file_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_file_versions_file_id ON file_versions(file_id);
CREATE INDEX IF NOT EXISTS idx_file_versions_checksum ON file_versions(checksum_sha256);
CREATE INDEX IF NOT EXISTS idx_file_versions_s3_key ON file_versions(s3_object_key);

-- Add foreign key constraint for files.current_version_id after file_versions is created
ALTER TABLE files
    DROP CONSTRAINT IF EXISTS fk_files_current_version;

ALTER TABLE files
    ADD CONSTRAINT fk_files_current_version
    FOREIGN KEY (current_version_id) REFERENCES file_versions(id) ON DELETE SET NULL;

-- 4. Backup History (Audit log of all backup transactions)
CREATE TABLE IF NOT EXISTS backup_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id VARCHAR(36),
    version_id VARCHAR(36),
    filename VARCHAR(255) NOT NULL,
    version_number INT NOT NULL DEFAULT 1,
    file_size_bytes BIGINT NOT NULL,
    checksum_sha256 VARCHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL, -- COMPLETED, FAILED, IN_PROGRESS, ROLLED_BACK
    failure_reason TEXT,
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_backup_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_backup_history_user_id ON backup_history(user_id);
CREATE INDEX IF NOT EXISTS idx_backup_history_created_at ON backup_history(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_backup_history_status ON backup_history(status);

-- 5. Recovery History (Audit log of all restore and recovery operations)
CREATE TABLE IF NOT EXISTS recovery_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_id VARCHAR(36) NOT NULL,
    restored_version_id VARCHAR(36) NOT NULL,
    restored_version_number INT NOT NULL,
    filename VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL, -- SUCCESS, FAILED, VERIFICATION_FAILED
    checksum_verified BOOLEAN NOT NULL DEFAULT FALSE,
    expected_checksum VARCHAR(64),
    actual_checksum VARCHAR(64),
    failure_reason TEXT,
    restored_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recovery_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_recovery_history_user_id ON recovery_history(user_id);
CREATE INDEX IF NOT EXISTS idx_recovery_history_file_id ON recovery_history(file_id);
CREATE INDEX IF NOT EXISTS idx_recovery_history_restored_at ON recovery_history(restored_at DESC);
