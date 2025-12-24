package com.bm.wschat.feature.attachment.dto.response;

/**
 * Ответ с presigned URL для загрузки файла.
 */
public record UploadUrlResponse(
        String uploadUrl, // Presigned PUT URL для загрузки напрямую в MinIO
        String fileKey, // Ключ файла в MinIO (нужен для подтверждения)
        String filename, // Оригинальное имя файла
        String bucket // Имя бакета MinIO
) {
}
