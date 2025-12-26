package com.bm.wschat.shared.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Настройки подключения к MinIO.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String host = "localhost";
    private int port = 9000;
    private String username = "admin";
    private String password = "password123";
    private boolean secure = false; // использовать HTTPS

    // Публичный URL для клиентов (если отличается от внутреннего)
    // Например: http://192.168.1.100:9000 или https://minio.example.com
    private String publicUrl;

    // Бакеты для разных типов вложений
    private String chatBucket = "chat-attachments";
    private String wikiBucket = "wiki-attachments";

    /**
     * Внутренний endpoint для подключения MinioClient (для серверной части).
     */
    public String getEndpoint() {
        String protocol = secure ? "https" : "http";
        return protocol + "://" + host + ":" + port;
    }

    /**
     * Публичный endpoint для клиентов (presigned URLs).
     * Если publicUrl не задан, использует обычный endpoint.
     */
    public String getPublicEndpoint() {
        if (publicUrl != null && !publicUrl.isBlank()) {
            return publicUrl;
        }
        return getEndpoint();
    }

    public List<String> getAllBuckets() {
        return List.of(chatBucket, wikiBucket);
    }
}
