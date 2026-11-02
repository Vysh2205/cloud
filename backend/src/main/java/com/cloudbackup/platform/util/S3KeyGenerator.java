package com.cloudbackup.platform.util;

import java.text.Normalizer;

public class S3KeyGenerator {

    private S3KeyGenerator() {}

    /**
     * Generates a deterministic, partition-friendly S3 object key.
     * Pattern: users/{userId}/files/{fileId}/v{versionNumber}/{sanitizedFilename}
     */
    public static String generateKey(Long userId, String fileId, Integer versionNumber, String filename) {
        String sanitized = sanitizeFilename(filename);
        return String.format("users/%d/files/%s/v%d/%s", userId, fileId, versionNumber, sanitized);
    }

    /**
     * Sanitizes filename to prevent path traversal or invalid S3 characters.
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed_file";
        }
        // Normalize unicode
        String normalized = Normalizer.normalize(filename, Normalizer.Form.NFC);
        // Replace backslashes and slashes
        normalized = normalized.replace('\\', '_').replace('/', '_');
        // Strip forbidden characters
        normalized = normalized.replaceAll("[^a-zA-Z0-9._-]", "_");
        // Prevent leading dots (hidden files/parent dir)
        if (normalized.startsWith(".")) {
            normalized = "_" + normalized.substring(1);
        }
        return normalized;
    }
}
