package com.bm.wschat.feature.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Запрос на обновление профиля (ФИО, email).
 */
public record UpdateProfileRequest(
        @Size(max = 150, message = "ФИО не должно превышать 150 символов") String fio,

        @Email(message = "Некорректный email") @Size(max = 200, message = "Email не должен превышать 200 символов") String email) {
}
