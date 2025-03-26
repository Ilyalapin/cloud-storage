package com.cloud_storage.minio.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MinioClientConfig {

    private final MinioClientProperties minioClientProperties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioClientProperties.getUrl())
                .credentials(minioClientProperties.getUser(), minioClientProperties.getPassword())
                .build();
    }
}

