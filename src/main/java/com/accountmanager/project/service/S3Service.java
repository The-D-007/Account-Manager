package com.accountmanager.project.service;

import com.accountmanager.project.config.AwsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    @Autowired
    public S3Service(AwsConfig aws) {
        AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(aws.getAccessKey(), aws.getSecretKey());
        this.s3Client = S3Client.builder()
                .region(Region.of(aws.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(aws.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
        this.bucketName = aws.getBucketName();
    }

    public String uploadImage(String filePath) {
        String key = generateUniqueKey(filePath);
        File file = new File(filePath);
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        PutObjectResponse response = s3Client.putObject(putObjectRequest, file.toPath());
        return key;
    }
    private String generateUniqueKey(String filePath) {
        String fileExtension = getFileExtension(filePath);
        return UUID.randomUUID() + "-" + System.currentTimeMillis() + fileExtension;
    }

    private String getFileExtension(String filePath) {
        int lastIndexOfDot = filePath.lastIndexOf(".");
        if (lastIndexOfDot == -1) {
            return "";
        }
        return filePath.substring(lastIndexOfDot);
    }

    public URL generatePresignedUrl(String key, Duration duration) {
        GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(b -> b.bucket(bucketName).key(key))
                .build();
        PresignedGetObjectRequest presignResponse = s3Presigner.presignGetObject(getObjectPresignRequest);
        return presignResponse.url();
    }

    public String getImageLink(String key){
        URL presignedUrl = generatePresignedUrl(key, Duration.ofMinutes(10));
        return String.valueOf(presignedUrl);
    }

    public void deleteImage(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }
}
