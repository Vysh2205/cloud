package com.cloudbackup.platform.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

public class RetryUtils {

    private static final Logger log = LoggerFactory.getLogger(RetryUtils.class);

    private RetryUtils() {}

    /**
     * Executes a callable action with exponential backoff and jitter.
     *
     * @param action The operation to execute.
     * @param maxAttempts Maximum retry attempts.
     * @param initialDelayMs Initial delay in milliseconds.
     * @param multiplier Delay multiplier.
     * @param operationName Descriptive name for logs.
     * @param <T> Return type.
     * @return Result of the action.
     * @throws Exception If all attempts fail.
     */
    public static <T> T executeWithRetry(
            Callable<T> action,
            int maxAttempts,
            long initialDelayMs,
            double multiplier,
            String operationName) throws Exception {

        int attempt = 0;
        long currentDelay = initialDelayMs;

        while (true) {
            attempt++;
            try {
                return action.call();
            } catch (Exception ex) {
                if (attempt >= maxAttempts) {
                    log.error("[RetryEngine] All {} attempts failed for operation: '{}'. Error: {}",
                            maxAttempts, operationName, ex.getMessage());
                    throw ex;
                }

                // Calculate jitter between 0.8 and 1.2
                double jitter = 0.8 + (ThreadLocalRandom.current().nextDouble() * 0.4);
                long sleepTime = (long) (currentDelay * jitter);

                log.warn("[RetryEngine] Attempt {} failed for '{}': {}. Retrying in {} ms...",
                        attempt, operationName, ex.getMessage(), sleepTime);

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted for operation: " + operationName, ie);
                }

                currentDelay = (long) (currentDelay * multiplier);
            }
        }
    }
}
