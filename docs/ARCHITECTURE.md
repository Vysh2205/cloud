# System Architecture: Distributed Cloud Backup & Recovery Platform

## 1. High-Level Architecture Overview

The **Distributed Cloud Backup & Recovery Platform** is engineered following enterprise cloud-native patterns for resilient distributed object storage, cryptographic integrity verification, multi-tenant isolation, and disaster recovery.

```mermaid
graph TD
    Client["Client Web Browser (React + Vite)"] -->|HTTPS / REST API| Ingress["Kubernetes Ingress / Nginx Reverse Proxy"]
    
    subgraph "Kubernetes Cluster / Docker Network"
        Ingress -->|/api/*| Backend["Spring Boot Backend Service (2+ Replicas)"]
        Ingress -->|/*| Frontend["Frontend Nginx Pod (React SPA)"]
        
        Backend -->|Connection Pool (HikariCP)| DB[(PostgreSQL Database)]
        Backend -->|AWS SDK v2 (S3 API)| CloudStorage["AWS S3 / MinIO Object Storage"]
    end

    subgraph "Security & Resilience Layer"
        Backend -->|Filter Intercept| JWT["JWT Auth & BCrypt RBAC"]
        Backend -->|Stream Hashing| Checksum["SHA-256 Cryptographic Engine"]
        Backend -->|Backoff & Jitter| Retry["Exponential Retry Engine"]
        Backend -->|Compensating Cleanup| Rollback["Distributed Rollback Manager"]
    end
```

---

## 2. Component Breakdown

### A. Frontend Client (React.js + Vite + Tailwind CSS)
- **Role:** Single Page Application (SPA) providing real-time dashboard analytics, drag-and-drop file upload, version history inspection, one-click integrity verification, and recovery rollback triggers.
- **Client-Side Hashing:** Computes client-side SHA-256 pre-upload checksums via the browser's native `crypto.subtle` API for client-to-cloud end-to-end verification.
- **Reverse Proxy:** Served via unprivileged Nginx with automated Gzip compression and `/api` upstream proxying.

### B. Cloud Backend Engine (Java 17 + Spring Boot 3)
- **Role:** Core business logic, distributed coordination, and cloud storage abstraction.
- **REST APIs:** Full RESTful interface managing authentication, file upload, version tree navigation, integrity audits, and file downloads.
- **Stateless Authentication:** Secure JWT filter interceptor extracting claims and establishing `SecurityContextHolder` with role-based access control (`ROLE_USER`, `ROLE_ADMIN`).
- **Storage Abstraction:** Pluggable `FileStorageService` backed by AWS SDK v2 `S3Client`, supporting production AWS S3 as well as MinIO and LocalStack for zero-cost local development.

### C. Persistent Storage Tier
1. **Metadata Store (PostgreSQL):**
   - Tables: `users`, `files`, `file_versions`, `backup_history`, `recovery_history`.
   - Stores user quotas, version numbers, immutable S3 object keys, tamper-evident SHA-256 digests, and audit timestamps.
2. **Object Storage Store (AWS S3 / MinIO):**
   - Partitioned by user and file IDs: `users/{userId}/files/{fileId}/v{versionNumber}/{filename}`.
   - Dual-layer protection: Bucket-level S3 versioning enabled in addition to application-level immutable version indexing.

---

## 3. Resilience and Distributed Consistency Patterns

### A. Distributed 2-Phase Compensating Rollback
When uploading a file:
1. Object stream is sent to AWS S3.
2. Metadata is persisted to PostgreSQL in a transaction.
3. If PostgreSQL fails or rejects the transaction (e.g., constraint violation or DB timeout), the **Compensating Rollback Manager** triggers a compensating `S3:DeleteObject` call to clean up orphaned blobs, preventing unmanaged cloud storage waste.

### B. Exponential Backoff with Jitter
Network hiccups and transient S3 errors (HTTP 500, 503 SlowDown) are shielded through `RetryUtils.executeWithRetry` configured with exponential backoff and randomized jitter:
$$\text{delay} = \text{baseDelay} \times \text{multiplier}^{\text{attempt}} \times \text{random}(0.8, 1.2)$$

### C. Idempotency Guard
Before uploading, the system checks if the exact SHA-256 checksum exists for the latest version of the file. Duplicate uploads of unchanged content are rejected immediately, saving bandwidth and cloud storage costs.
