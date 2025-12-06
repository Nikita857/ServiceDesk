package com.bm.wschat.shared.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app.upload")
@Getter
@Setter
public class FileStorageConfig {

    /**
     * Directory to store uploaded files
     */
    private String dir = "./uploads";

    /**
     * Maximum file size in bytes (default 10MB)
     */
    private long maxFileSize = 10 * 1024 * 1024;

    /**
     * Allowed MIME types
     */
    private List<String> allowedTypes = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
}
