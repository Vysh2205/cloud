# Platform Workflows & Sequence Diagrams

## 1. File Upload & Backup Workflow

```mermaid
sequenceDiagram
    autonumber
    actor User as User / Browser
    participant API as Spring Boot Backend
    participant DB as PostgreSQL
    participant S3 as AWS S3 / MinIO Storage

    User->>API: POST /api/files/upload (MultipartFile, JWT)
    API->>API: Authenticate JWT & Extract User Context
    API->>API: Compute SHA-256 Checksum on Input Stream
    API->>DB: Check User Quota & Existing File Record
    
    alt Idempotency Match (Checksum == Current Version)
        API-->>User: 409 Conflict ("File content identical to current version")
    else New File or Updated Version
        API->>DB: Insert BackupHistory (status: IN_PROGRESS)
        API->>S3: PutObject(key: users/{uid}/files/{fid}/v{ver}/{name}, metadata: SHA-256)
        
        alt S3 Upload Fails
            API->>DB: Update BackupHistory (status: FAILED)
            API-->>User: 503 Service Unavailable
        else S3 Upload Succeeds
            API->>DB: Begin DB Transaction: Save FileVersion & Update File & Quota
            alt DB Commit Fails
                API->>S3: Compensating DeleteObject (cleanup orphaned blob)
                API->>DB: Update BackupHistory (status: ROLLED_BACK)
                API-->>User: 500 Internal Error ("Rolled back")
            else DB Commit Succeeds
                API->>DB: Update BackupHistory (status: COMPLETED)
                API-->>User: 201 Created (FileResponse + SHA-256)
            end
        end
    end
```

---

## 2. File Versioning Workflow

```mermaid
sequenceDiagram
    autonumber
    actor User as Client
    participant API as Backup Service
    participant DB as PostgreSQL Database
    participant S3 as AWS S3 Storage

    User->>API: POST /api/files/upload (filename: "config.yaml", content: v2)
    API->>DB: Query file by user and filename
    DB-->>API: Found existing file (currentVersionNumber: 1, id: "uuid-1")
    API->>API: Increment version: nextVersion = 2
    API->>API: Generate new key: "users/1/files/uuid-1/v2/config.yaml"
    API->>S3: Store version 2 blob into S3
    API->>DB: Insert into file_versions (version_number: 2, file_id: "uuid-1")
    API->>DB: Update files SET current_version_id = "v2", total_versions = 2
    Note over DB: Version 1 is immutable and remains untouched in S3 & DB
    API-->>User: Return updated file metadata (Version 2 active)
```

---

## 3. Cryptographic Integrity Verification Workflow

```mermaid
sequenceDiagram
    autonumber
    actor User as Auditor / User
    participant API as Integrity Service
    participant DB as PostgreSQL
    participant S3 as AWS S3 Storage

    User->>API: POST /api/files/{id}/verify (optional versionId)
    API->>DB: Fetch file version metadata (stored checksum: "hash_A")
    API->>S3: GetObject stream for target object key
    API->>API: Recalculate SHA-256 streamingly from S3 bytes ("hash_B")
    
    alt hash_A == hash_B
        API->>DB: Update verification_status = "VERIFIED", last_verified_at = NOW
        API-->>User: 200 OK (verified: true, status: "VERIFIED")
    else hash_A != hash_B (Bit Rot or Tampering)
        API->>DB: Update verification_status = "CORRUPTED", file.status = "CORRUPTED"
        API-->>User: 200 OK (verified: false, status: "CORRUPTED", alert message)
    end
```

---

## 4. Disaster Recovery & Restore Workflow

```mermaid
sequenceDiagram
    autonumber
    actor User as User / Admin
    participant API as Recovery Service
    participant DB as PostgreSQL
    participant S3 as AWS S3 Storage

    User->>API: POST /api/files/{id}/restore/{versionId}
    API->>DB: Query target historical version (e.g. v1)
    API->>S3: Pre-recovery stream read from S3
    API->>API: Recalculate SHA-256 of S3 object
    
    alt Pre-recovery Checksum Mismatch
        API->>DB: Log RecoveryHistory (status: VERIFICATION_FAILED)
        API-->>User: 400 Bad Request ("Pre-recovery verification failed: object corrupted")
    else Pre-recovery Checksum Matches
        API->>DB: Create new promoted FileVersion (v3 pointing to verified v1 S3 key)
        API->>DB: Update files SET current_version = v3, current_version_number = 3
        API->>DB: Log RecoveryHistory (status: SUCCESS, checksum_verified: true)
        API-->>User: 200 OK (Restored version promoted as current)
    end
```
