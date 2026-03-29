package com.pdfreader.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public void uploadFile(MultipartFile file, String s3Key, Map<String, String> metadata) throws IOException {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType("application/pdf")
                        .contentLength(file.getSize())
                        .metadata(metadata)
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        log.info("Uploaded to S3: {}", s3Key);
    }

    public InputStream downloadFile(String s3Key) {
        return s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(s3Key).build()
        );
    }

    public HeadObjectResponse getMetadata(String s3Key) {
        return s3Client.headObject(
                HeadObjectRequest.builder().bucket(bucketName).key(s3Key).build()
        );
    }

    public List<S3Object> listFiles(String prefix) {
        ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix).build()
        );
        return response.contents();
    }

    public String generatePresignedUrl(String s3Key) {
        return s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(1))
                        .getObjectRequest(r -> r.bucket(bucketName).key(s3Key))
                        .build()
        ).url().toString();
    }

    public void deleteFile(String s3Key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder().bucket(bucketName).key(s3Key).build()
        );
        log.info("Deleted from S3: {}", s3Key);
    }

    public void uploadJson(String s3Key, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType("application/json")
                        .contentLength((long) bytes.length)
                        .build(),
                RequestBody.fromBytes(bytes)
        );
    }

    public String downloadJson(String s3Key) {
        try {
            return s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucketName).key(s3Key).build()
            ).asUtf8String();
        } catch (NoSuchKeyException e) {
            return null;
        } catch (Exception e) {
            log.warn("Could not read JSON from S3 key {}: {}", s3Key, e.getMessage());
            return null;
        }
    }
}
