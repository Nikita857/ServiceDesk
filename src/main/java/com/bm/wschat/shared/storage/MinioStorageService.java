package com.bm.wschat.shared.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.http.Method;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Сервис хранения файлов в MinIO с поддержкой presigned URLs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    // Время жизни presigned URL для загрузки (минуты)
    private static final int UPLOAD_URL_EXPIRY_MINUTES = 15;

    // Время жизни presigned URL для скачивания (минуты)
    private static final int DOWNLOAD_URL_EXPIRY_MINUTES = 60;

    /**
     * Тип бакета для разных типов вложений.
     */
    public enum BucketType {
        CHAT,
        WIKI
    }

    /**
     * Генерирует presigned URL для загрузки файла напрямую в MinIO.
     */
    public PresignedUploadUrl generateUploadUrl(String originalFilename, String contentType, BucketType bucketType) {
        String fileKey = generateFileKey(originalFilename);
        String bucket = getBucket(bucketType);

        try {
            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(fileKey)
                            .expiry(UPLOAD_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
                            .build());

            log.info("Сгенерирован presigned URL для загрузки в {}: {}", bucket, fileKey);
            return new PresignedUploadUrl(uploadUrl, fileKey, originalFilename, bucket);

        } catch (Exception e) {
            log.error("Ошибка генерации presigned URL: {}", e.getMessage());
            throw new StorageException("Не удалось сгенерировать URL для загрузки", e);
        }
    }

    /**
     * Генерирует presigned URL для скачивания файла.
     */
    /**
     * Генерирует presigned URL для скачивания файла.
     */
    public String generateDownloadUrl(String fileKey, String bucket, String originalFilename) {
        try {
            // Формируем заголовок Content-Disposition
            String encodedFilename = URLEncoder.encode(originalFilename, StandardCharsets.UTF_8).replace("+", "%20");
            Map<String, String> extraParams = Map.of(
                    "response-content-disposition",
                    "attachment; filename=\"" + originalFilename + "\"; filename*=UTF-8''" + encodedFilename);

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(fileKey)
                            .expiry(DOWNLOAD_URL_EXPIRY_MINUTES, TimeUnit.MINUTES)
                            .extraQueryParams(extraParams)
                            .build());
        } catch (Exception e) {
            log.error("Ошибка генерации download URL: {}", e.getMessage());
            throw new StorageException("Не удалось сгенерировать URL для скачивания", e);
        }
    }

    /**
     * Проверяет, существует ли файл в MinIO.
     */
    public boolean fileExists(String fileKey, String bucket) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Загружает файл напрямую через бэкенд (fallback для legacy).
     */
    public String uploadFile(MultipartFile file, BucketType bucketType) {
        String fileKey = generateFileKey(file.getOriginalFilename());
        String bucket = getBucket(bucketType);

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());

            log.info("Файл загружен в MinIO bucket {}: {}", bucket, fileKey);
            return fileKey;

        } catch (Exception e) {
            log.error("Ошибка загрузки файла в MinIO: {}", e.getMessage());
            throw new StorageException("Не удалось загрузить файл", e);
        }
    }

    /**
     * Удаляет файл из MinIO.
     */
    public void deleteFile(String fileKey, String bucket) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .build());
            log.info("Файл удалён из MinIO bucket {}: {}", bucket, fileKey);
        } catch (Exception e) {
            log.error("Ошибка удаления файла из MinIO: {}", e.getMessage());
        }
    }

    /**
     * Возвращает имя бакета по типу.
     */
    public String getBucket(BucketType bucketType) {
        return switch (bucketType) {
            case CHAT -> minioProperties.getChatBucket();
            case WIKI -> minioProperties.getWikiBucket();
        };
    }

    /**
     * Генерирует уникальный ключ файла с сохранением расширения.
     */
    private String generateFileKey(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    // === DTOs ===

    public record PresignedUploadUrl(String uploadUrl, String fileKey, String originalFilename, String bucket) {
    }

    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
