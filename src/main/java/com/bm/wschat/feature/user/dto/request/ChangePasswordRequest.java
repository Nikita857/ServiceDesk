package com.bm.wschat.feature.user.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Запрос на смену пароля.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Текущий пароль обязателен") String oldPassword,

        @NotBlank(message = "Новый пароль обязателен") String newPassword) {
}
