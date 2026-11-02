package com.cloudbackup.platform.service;

import java.io.InputStream;
import java.util.Map;

public interface FileStorageService {

    /**
     * Uploads object stream to cloud storage with retry and SHA-256 metadata.
     *
     * @return S3 Version ID or generated object identifier.
     */
    String uploadObject(String bucket, String key, InputStream inputStream, long contentLength,
                        String contentType, Map<String, String> metadata);

    /**
     * Streams object bytes from cloud storage.
     */
    InputStream downloadObjectStream(String bucket, String key);

    /**
     * Checks if the object exists in cloud storage.
     */
    boolean objectExists(String bucket, String key);

    /**
     * Compensating deletion of object from cloud storage.
     */
    void deleteObject(String bucket, String key);

    /**
     * Gets storage bucket name.
     */
    String getDefaultBucket();
}
