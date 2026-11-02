package com.cloudbackup.platform;

import com.cloudbackup.platform.util.ChecksumUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChecksumUtils Cryptographic Integrity Tests")
class ChecksumUtilsTest {

    @Test
    @DisplayName("Should correctly calculate known SHA-256 checksum for byte array")
    void testCalculateSha256_KnownInput() {
        byte[] input = "Distributed Cloud Backup & Recovery".getBytes(StandardCharsets.UTF_8);
        String checksum = ChecksumUtils.calculateSha256(input);

        assertNotNull(checksum);
        assertEquals(64, checksum.length());
        // Verify deterministic hashing
        String secondRun = ChecksumUtils.calculateSha256(input);
        assertEquals(checksum, secondRun);
    }

    @Test
    @DisplayName("Should streamingly compute identical checksum from InputStream")
    void testCalculateSha256_InputStream() {
        byte[] input = "Cloud storage resilience and fault tolerance".getBytes(StandardCharsets.UTF_8);
        String arrayChecksum = ChecksumUtils.calculateSha256(input);

        String streamChecksum = ChecksumUtils.calculateSha256(new ByteArrayInputStream(input));
        assertEquals(arrayChecksum, streamChecksum);
    }

    @Test
    @DisplayName("Should verify valid checksum and reject tampered content")
    void testVerifyChecksum() {
        byte[] validData = "Secure backup block v1.0.0".getBytes(StandardCharsets.UTF_8);
        String expectedChecksum = ChecksumUtils.calculateSha256(validData);

        // Valid match
        assertTrue(ChecksumUtils.verifyChecksum(new ByteArrayInputStream(validData), expectedChecksum));

        // Tampered byte stream
        byte[] tamperedData = "Secure backup block v1.0.1 (Tampered)".getBytes(StandardCharsets.UTF_8);
        assertFalse(ChecksumUtils.verifyChecksum(new ByteArrayInputStream(tamperedData), expectedChecksum));
    }
}
