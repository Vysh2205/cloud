# Postman Testing & Verification Guide

This project includes a comprehensive Postman collection located at:  
`postman/Distributed_Cloud_Backup_Platform.postman_collection.json`

---

## 1. Importing the Collection

1. Open **Postman**.
2. Click **Import** in the upper-left corner.
3. Drag & drop `postman/Distributed_Cloud_Backup_Platform.postman_collection.json` (or click **Files** and select it).
4. The collection **Distributed Cloud Backup & Recovery Platform API** will appear in your left sidebar.

---

## 2. Environment Variables & Automated JWT Chaining

The collection comes with collection variables pre-configured:
- `baseUrl`: `http://localhost:8080` (or `http://localhost:3000/api`)
- `token`: Automatically populated upon calling **Register** or **Login** via embedded test scripts.
- `fileId`: Automatically captured when calling **Upload File**.
- `versionId`: Automatically captured from version responses.

---

## 3. Recommended Execution Order

Execute requests in the following sequence for full end-to-end verification:

1. **Authentication:**
   - Execute `Register User`: Verifies password hashing, creates user, stores JWT token.
   - Execute `Login User`: Re-authenticates and verifies JWT issuance.
   - Execute `Get Current Profile`: Confirms `ROLE_USER` permissions and initial storage quota.

2. **File Upload & Backup:**
   - Execute `Upload File (v1)`: Select any file from your computer. Verify response returns status `201`, generated `id`, `currentVersionNumber: 1`, and 64-character SHA-256 digest.
   - Execute `Upload File (v1)` again with the SAME file: Verify that the idempotency check rejects duplicate unchanged uploads with `409 Conflict`.
   - Modify the file slightly and upload again: Verify version counter increments to `2`.
   - Execute `List All User Files`: Confirms inventory listing.
   - Execute `Search Files By Keyword`: Confirms search filtering.

3. **Versioning & Integrity Verification:**
   - Execute `Get File Version Lineage`: Inspects the immutable revision ledger (`v1`, `v2`).
   - Execute `Verify Cryptographic Integrity`: Streams object directly from S3, recalculates SHA-256, and returns `verified: true`.

4. **Disaster Recovery & Rollback:**
   - Execute `Restore Previous Version`: Restores `v1`, triggers pre-recovery integrity verification, and promotes `v1` to `v3`.
   - Execute `Download Active File`: Downloads the active version and verifies `X-Checksum-SHA256` HTTP header.

5. **Audit Trails:**
   - Execute `Get Backup History Audit Log`: Validates complete audit history including timestamps and retry counts.
   - Execute `Get Recovery History Audit Log`: Validates recovery history with checksum verification timestamps.
   - Execute `Get Dashboard Statistics`: Validates aggregated system statistics.
