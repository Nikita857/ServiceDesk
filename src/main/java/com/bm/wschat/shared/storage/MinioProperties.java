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

    // Бакеты для разных типов вложений
    private String chatBucket = "chat-attachments";
    private String wikiBucket = "wiki-attachments";

    public String getEndpoint() {
        String protocol = secure ? "https" : "http";
        return protocol + "://" + host + ":" + port;
    }

    public List<String> getAllBuckets() {
        return List.of(chatBucket, wikiBucket);
    }
}
