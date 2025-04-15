package com.cloud_storage.integrationTest.config;

import com.cloud_storage.minio.service.MinioService;
import io.minio.MinioClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class MinioServiceTestConfig {
    private MinIOContainer minio = new MinIOContainer(DockerImageName.parse("minio/minio:latest"))
            .withEnv("MINIO_ROOT_USER", "minioadmin62")
                .withEnv("MINIO_ROOT_PASSWORD", "minioadmin62")
                .withCommand("server /data")
                .withExposedPorts(9000);

    @Bean
    public MinioClient minioClient() {
        minio.start();
        return MinioClient.builder()
                .endpoint(minio.getS3URL())
                .credentials("minioadmin62", "minioadmin62")
                .build();
    }


    @Bean
    public MinioService minioService(MinioClient minioClient) {
        return new MinioService(minioClient);
    }
}
