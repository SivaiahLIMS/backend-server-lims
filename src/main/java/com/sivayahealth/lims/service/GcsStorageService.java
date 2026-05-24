package com.sivayahealth.lims.service;

import com.google.cloud.storage.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * Handles all Google Cloud Storage operations for LIMS document files.
 * Storage path convention: {tenantId}/{documentId}/v{versionNo}/{originalFilename}
 */
@Service
public class GcsStorageService {

    private static final Logger log = LogManager.getLogger(GcsStorageService.class);

    private final Storage storage;
    private final String bucket;

    public GcsStorageService(@Value("${gcp.storage.bucket:lims-documents}") String bucket) {
        this.bucket  = bucket;
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    /**
     * Upload bytes to GCS and return a signed URL valid for the given number of days.
     */
    public UploadResult upload(byte[] content, String storagePath, String contentType, int signedUrlDays) {
        BlobId   blobId   = BlobId.of(bucket, storagePath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        storage.create(blobInfo, content);
        log.info("Uploaded {} bytes to gs://{}/{}", content.length, bucket, storagePath);

        URL signedUrl = storage.signUrl(
                blobInfo,
                signedUrlDays,
                TimeUnit.DAYS,
                Storage.SignUrlOption.withV4Signature()
        );

        return new UploadResult(storagePath, signedUrl.toString(), (long) content.length);
    }

    /**
     * Delete a file from GCS (used on retire/cleanup).
     */
    public boolean delete(String storagePath) {
        boolean deleted = storage.delete(BlobId.of(bucket, storagePath));
        log.info("Delete gs://{}/{} -> {}", bucket, storagePath, deleted);
        return deleted;
    }

    /**
     * Generate a fresh signed URL for an existing object (e.g. to refresh expired URLs).
     */
    public String refreshSignedUrl(String storagePath, int days) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, storagePath)).build();
        URL url = storage.signUrl(blobInfo, days, TimeUnit.DAYS,
                Storage.SignUrlOption.withV4Signature());
        return url.toString();
    }

    public record UploadResult(String storagePath, String signedUrl, Long fileSizeBytes) {}
}
