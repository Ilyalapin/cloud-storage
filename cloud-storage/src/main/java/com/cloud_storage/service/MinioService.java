package com.cloud_storage.service;

import com.cloud_storage.common.exception.MinioException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {
    private final MinioClient minioClient;

    @Value("${minio.bucket.name}")
    private String bucketName;


    @PostConstruct
    public void init() throws Exception {
        createBucketIfNotExists();
    }


    public void createBucketIfNotExists() throws Exception {
        try {
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!isExist) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Bucket:{} created successfully", bucketName);
            } else {
                log.info("Bucket:{} already exist", bucketName);
            }
        } catch (RuntimeException e) {
            throw new MinioException("Error to create a bucket: " + e.getMessage());
        }
    }


    public void createUserFolder(String userId) throws MinioException {
        String folderName = "user-" + userId + "-files";
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName).object(folderName + "/")
                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                    .build());
            log.info("Folder:{} created successfully", folderName);
        } catch (Exception e) {
            throw new MinioException(e.getMessage());
        }
    }
}
