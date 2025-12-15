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
            log.info("Директория загрузки создана: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Не удается создать директорию загрузки: " + uploadPath, e);
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
            log.debug("Файл сохранен: {} -> {}", originalFilename, storedFilename);
            return storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Не удалось сохранить файл: " + originalFilename, e);
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
                throw new RuntimeException("Файл не найден: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Файл не найден: " + filename, e);
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
            log.error("Не удалось удалиь файл: {}", filename, e);
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
            throw new IllegalArgumentException("Размер файла превышает ограничение 10МБ: " +
                    (config.getMaxFileSize() / 1024 / 1024) + "MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedType(contentType)) {
            throw new IllegalArgumentException("Тип файла запрещен: " + contentType);
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
