package com.bm.wschat.shared.service;

import com.bm.wschat.shared.config.FileStorageConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageConfig config;
    private Path uploadPath;

    @PostConstruct
    public void init() {
        uploadPath = Paths.get(config.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadPath);
            log.info("Upload directory initialized: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadPath, e);
        }
    }

    /**
     * Store file and return generated filename
     */
    public String store(MultipartFile file) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID() + extension;

        try {
            Path targetPath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File stored: {} -> {}", originalFilename, storedFilename);
            return storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }
    }

    /**
     * Load file as Resource
     */
    public Resource load(String filename) {
        try {
            Path filePath = uploadPath.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("File not found: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("File not found: " + filename, e);
        }
    }

    /**
     * Delete file
     */
    public boolean delete(String filename) {
        try {
            Path filePath = uploadPath.resolve(filename).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filename, e);
            return false;
        }
    }

    /**
     * Get relative URL for file
     */
    public String getUrl(String filename) {
        return "/api/v1/attachments/file/" + filename;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > config.getMaxFileSize()) {
            throw new IllegalArgumentException("File size exceeds maximum allowed: " +
                    (config.getMaxFileSize() / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedType(contentType)) {
            throw new IllegalArgumentException("File type not allowed: " + contentType);
        }
    }

    private boolean isAllowedType(String contentType) {
        return config.getAllowedTypes().stream()
                .anyMatch(allowed -> {
                    if (allowed.endsWith("/*")) {
                        String prefix = allowed.substring(0, allowed.length() - 1);
                        return contentType.startsWith(prefix);
                    }
                    return allowed.equals(contentType);
                });
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex > 0 ? filename.substring(dotIndex) : "";
    }
}
