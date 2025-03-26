package com.cloud_storage.integrationTest.config;

import com.cloud_storage.service.MinioService;
import io.minio.MinioClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class MinioServiceTestConfig {

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint("http://localhost:9001")
                .credentials("minioadmin62", "minioadmin62")
                .build();
    }


    @Bean
    public MinioService minioService(MinioClient minioClient) {
        return new MinioService(minioClient);
    }
}
