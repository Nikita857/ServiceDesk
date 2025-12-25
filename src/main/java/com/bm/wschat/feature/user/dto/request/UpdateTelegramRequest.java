package com.bm.wschat.feature.user.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Запрос на обновление Telegram ID.
 */
public record UpdateTelegramRequest(
        @NotNull(message = "Telegram ID обязателен") Long telegramId) {
}
