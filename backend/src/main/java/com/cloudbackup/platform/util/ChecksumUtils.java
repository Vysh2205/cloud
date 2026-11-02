package com.cloudbackup.platform.util;

import com.cloudbackup.platform.exception.StorageException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumUtils {

    private static final String SHA_256 = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    private ChecksumUtils() {}

    /**
     * Calculates the SHA-256 checksum of an InputStream by streaming through a buffer.
     * Memory-efficient: does NOT load the full stream into memory.
     */
    public static String calculateSha256(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new StorageException("Failed to calculate SHA-256 checksum: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the SHA-256 checksum of raw byte array.
     */
    public static String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(data);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new StorageException("SHA-256 algorithm not available: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies that the computed SHA-256 checksum of the stream matches the expected checksum.
     */
    public static boolean verifyChecksum(InputStream inputStream, String expectedChecksum) {
        String actual = calculateSha256(inputStream);
        return actual.equalsIgnoreCase(expectedChecksum);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
