package com.bm.wschat.shared.storage.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.bm.wschat.shared.storage.MinioProperties;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;

/**
 * Конфигурация MinIO клиента.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    /**
     * Основной MinioClient для операций с файлами (upload, delete, check).
     * Использует внутренний endpoint (Docker hostname).
     */
    @Bean
    @Primary
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getUsername(), minioProperties.getPassword())
                .build();

        // Создаём все бакеты если не существуют
        for (String bucket : minioProperties.getAllBuckets()) {
            createBucketIfNotExists(client, bucket);
        }

        log.info("MinIO клиент инициализирован с endpoint: {}", minioProperties.getEndpoint());
        return client;
    }

    /**
     * MinioClient для генерации публичных presigned URLs.
     * Использует публичный endpoint (IP или домен, доступный из браузера).
     * 
     * ВАЖНО: Публичный endpoint должен быть доступен из контейнера.
     * Для этого в docker-compose добавлен extra_hosts с host.docker.internal.
     */
    @Bean
    public MinioClient publicMinioClient() {
        String publicEndpoint = minioProperties.getPublicEndpoint();
        log.info("PublicMinioClient с endpoint: {}", publicEndpoint);

        return MinioClient.builder()
                .endpoint(publicEndpoint)
                .credentials(minioProperties.getUsername(), minioProperties.getPassword())
                .build();
    }

    private void createBucketIfNotExists(MinioClient client, String bucketName) {
        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build());

            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build());
                log.info("Создан MinIO bucket: {}", bucketName);
            } else {
                log.debug("MinIO bucket уже существует: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Ошибка при создании MinIO bucket {}: {}", bucketName, e.getMessage());
        }
    }
}
