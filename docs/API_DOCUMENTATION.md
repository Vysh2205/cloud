# REST API Documentation

**Base URL:** `http://localhost:8080` (or `http://localhost:3000/api` through reverse proxy)  
**Authentication Scheme:** `Authorization: Bearer <JWT_TOKEN>`

---

## 1. Authentication Endpoints

### Register User
- **Endpoint:** `POST /api/auth/register`
- **Access:** Public
- **Request Body:**
```json
{
  "username": "cloud_engineer",
  "email": "engineer@company.com",
  "password": "SecurePassword123!"
}
```
- **Response (201 Created):**
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "type": "Bearer",
    "id": 1,
    "username": "cloud_engineer",
    "email": "engineer@company.com",
    "role": "ROLE_USER",
    "storageQuotaBytes": 5368709120,
    "usedStorageBytes": 0
  },
  "timestamp": "2026-11-02T07:30:00Z"
}
```

### Login User
- **Endpoint:** `POST /api/auth/login`
- **Access:** Public
- **Request Body:**
```json
{
  "usernameOrEmail": "cloud_engineer",
  "password": "SecurePassword123!"
}
```
- **Response (200 OK):** Returns JWT token and user profile.

### Get Current Profile
- **Endpoint:** `GET /api/auth/me`
- **Access:** Authenticated (Bearer Token)
- **Response (200 OK):** Returns current user details and storage quota usage.

---

## 2. File Upload & Backup Endpoints

### Upload & Back Up File
- **Endpoint:** `POST /api/files/upload`
- **Access:** Authenticated
- **Content-Type:** `multipart/form-data`
- **Form Parameters:**
  - `file` (File, required): The binary payload.
  - `changeSummary` (String, optional): Description or release notes for this version.
- **Response (201 Created):**
```json
{
  "success": true,
  "message": "File uploaded and backed up successfully",
  "data": {
    "id": "e83713f0-2521-4d1a-8531-bc663b65f7c3",
    "filename": "application-prod.yml",
    "contentType": "text/yaml",
    "currentVersionId": "7df41ad2-881c-4b53-8519-74d6c4e09f51",
    "currentVersionNumber": 1,
    "totalVersions": 1,
    "totalSizeBytes": 4096,
    "status": "ACTIVE",
    "checksumSha256": "4b227777d4dd1fc61c6f884f48641d02b4d121d3fd328cb08b5531fcacdabf8a",
    "createdAt": "2026-11-02T07:32:00Z",
    "updatedAt": "2026-11-02T07:32:00Z"
  }
}
```

### List Files
- **Endpoint:** `GET /api/files`
- **Query Parameters:** `search` (optional string)
- **Response (200 OK):** Returns array of all active files owned by the authenticated user.

### Get File By ID
- **Endpoint:** `GET /api/files/{id}`
- **Response (200 OK):** Returns file metadata.

---

## 3. Versioning & Integrity Endpoints

### Get Version Lineage
- **Endpoint:** `GET /api/files/{id}/versions`
- **Response (200 OK):**
```json
{
  "success": true,
  "data": [
    {
      "id": "7df41ad2-881c-4b53-8519-74d6c4e09f51",
      "versionNumber": 2,
      "s3Bucket": "distributed-cloud-backups",
      "s3ObjectKey": "users/1/files/e83713f0-2521-4d1a-8531-bc663b65f7c3/v2/application-prod.yml",
      "checksumSha256": "a35d918...",
      "fileSizeBytes": 5120,
      "contentType": "text/yaml",
      "changeSummary": "Added TLS mutual auth",
      "verificationStatus": "VERIFIED",
      "lastVerifiedAt": "2026-11-02T07:35:00Z",
      "createdAt": "2026-11-02T07:35:00Z"
    },
    {
      "id": "1192e2fb-a790-4e1b-930e-5a0dc5c63ef5",
      "versionNumber": 1,
      "s3Bucket": "distributed-cloud-backups",
      "s3ObjectKey": "users/1/files/e83713f0-2521-4d1a-8531-bc663b65f7c3/v1/application-prod.yml",
      "checksumSha256": "4b22777...",
      "fileSizeBytes": 4096,
      "contentType": "text/yaml",
      "changeSummary": "Initial production backup",
      "verificationStatus": "VERIFIED",
      "lastVerifiedAt": "2026-11-02T07:32:00Z",
      "createdAt": "2026-11-02T07:32:00Z"
    }
  ]
}
```

### Verify Integrity (Cryptographic SHA-256)
- **Endpoint:** `POST /api/files/{id}/verify`
- **Query Parameter:** `versionId` (optional; defaults to current version)
- **Response (200 OK):**
```json
{
  "success": true,
  "message": "Integrity verification completed",
  "data": {
    "fileId": "e83713f0-2521-4d1a-8531-bc663b65f7c3",
    "versionId": "7df41ad2-881c-4b53-8519-74d6c4e09f51",
    "filename": "application-prod.yml",
    "versionNumber": 2,
    "expectedChecksum": "a35d918...",
    "actualChecksum": "a35d918...",
    "verified": true,
    "status": "VERIFIED",
    "verifiedAt": "2026-11-02T07:36:10Z",
    "message": "Cryptographic SHA-256 checksum verified: file is intact and tamper-free."
  }
}
```

---

## 4. Recovery & Download Endpoints

### Restore Previous Version
- **Endpoint:** `POST /api/files/{id}/restore/{versionId}`
- **Request Body:**
```json
{
  "promoteToCurrentVersion": true,
  "restoreReason": "Rollback corrupted deployment configuration"
}
```
- **Response (200 OK):** Returns updated file metadata with newly promoted active version.

### Download File
- **Endpoint:** `GET /api/files/{id}/download`
- **Query Parameter:** `versionId` (optional; defaults to latest active version)
- **Response (200 OK):** Binary octet-stream with headers:
  - `Content-Disposition: attachment; filename="..."`
  - `X-Checksum-SHA256: <SHA-256>`

---

## 5. Audit & Activity Trails

### Get Backup Audit History
- **Endpoint:** `GET /api/backup/history`
- **Response (200 OK):** Array of all backup events (filename, version, status, retries, timestamps).

### Get Recovery Audit History
- **Endpoint:** `GET /api/recovery/history`
- **Response (200 OK):** Array of all restore actions with pre-recovery checksum verification logs.

### Get Dashboard Statistics
- **Endpoint:** `GET /api/dashboard/stats`
- **Response (200 OK):** Aggregated metrics including total files, total storage consumed, quota, recovery count, and recent events.
