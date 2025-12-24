package com.bm.wschat.feature.attachment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Запрос на подтверждение загрузки файла после успешного upload в MinIO.
 */
public record ConfirmUploadRequest(
        @NotBlank(message = "Ключ файла обязателен") String fileKey,

        @NotBlank(message = "Имя файла обязательно") String filename,

        @NotBlank(message = "Content-Type обязателен") String contentType,

        Long fileSize,

        @NotBlank(message = "Bucket обязателен") String bucket,

        @NotNull(message = "Тип цели обязателен") UploadUrlRequest.TargetType targetType,

        @NotNull(message = "ID цели обязателен") Long targetId) {
}
