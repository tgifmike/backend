package com.backend.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public S3Service(
            S3Client s3Client,
            S3Presigner s3Presigner
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    // =========================================================
    // UPLOAD
    // =========================================================

    public String uploadImage(
            byte[] imageBytes,
            String contentType,
            String originalFileName,
            UUID lineCheckItemId,
            String photoType
    ) {

        String extension = getExtension(originalFileName);

        String key =
                "line-check-items/"
                        + lineCheckItemId
                        + "/"
                        + photoType.toLowerCase()
                        + "/"
                        + UUID.randomUUID()
                        + extension;

        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(contentType)
                        .build();

        s3Client.putObject(
                request,
                RequestBody.fromBytes(imageBytes)
        );

        System.out.println("SUCCESS: Uploaded image to S3");
        System.out.println("Bucket: " + bucketName);
        System.out.println("Key: " + key);

        return key;
    }

    // =========================================================
    // DELETE
    // =========================================================

    public void deleteFile(String key) {

        DeleteObjectRequest request =
                DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

        s3Client.deleteObject(request);

        System.out.println("SUCCESS: Deleted S3 object");
        System.out.println("Key: " + key);
    }

    // =========================================================
    // PRESIGNED URL
    // =========================================================

    public String generatePresignedUrl(String key) {

        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(15))
                        .getObjectRequest(getObjectRequest)
                        .build();

        PresignedGetObjectRequest presignedRequest =
                s3Presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }

    // =========================================================
    // TEST CONNECTION
    // =========================================================

    public void testConnection() {

        s3Client.headBucket(
                HeadBucketRequest.builder()
                        .bucket(bucketName)
                        .build()
        );

        System.out.println(
                "SUCCESS: Connected to S3 bucket: " + bucketName
        );
    }

    // =========================================================
    // FILE EXTENSION
    // =========================================================

    private String getExtension(String fileName) {

        if (fileName == null || !fileName.contains(".")) {
            return "";
        }

        return fileName.substring(
                fileName.lastIndexOf(".")
        ).toLowerCase();
    }
}