package com.cloudbackup.platform.exception;

public class ChecksumMismatchException extends RuntimeException {
    private final String expectedChecksum;
    private final String actualChecksum;

    public ChecksumMismatchException(String message, String expectedChecksum, String actualChecksum) {
        super(message);
        this.expectedChecksum = expectedChecksum;
        this.actualChecksum = actualChecksum;
    }

    public String getExpectedChecksum() {
        return expectedChecksum;
    }

    public String getActualChecksum() {
        return actualChecksum;
    }
}
