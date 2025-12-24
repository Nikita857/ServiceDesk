package com.bm.wschat.feature.attachment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Запрос на получение presigned URL для загрузки файла.
 */
public record UploadUrlRequest(
        @NotBlank(message = "Имя файла обязательно") String filename,

        @NotBlank(message = "Content-Type обязателен") String contentType,

        @NotNull(message = "Тип цели обязателен") TargetType targetType,

        @NotNull(message = "ID цели обязателен") Long targetId) {
    public enum TargetType {
        TICKET,
        MESSAGE,
        DIRECT_MESSAGE,
        WIKI_ARTICLE
    }
}
