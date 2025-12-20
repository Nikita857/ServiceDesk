package com.bm.wschat.feature.ticket.dto.ticket.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Запрос на оценку качества обслуживания.
 * Отправляется пользователем после закрытия тикета.
 */
public record RateTicketRequest(
        @NotNull(message = "Оценка обязательна") @Min(value = 1, message = "Минимальная оценка — 1") @Max(value = 5, message = "Максимальная оценка — 5") Integer rating,

        @Size(max = 1000, message = "Отзыв не должен превышать 1000 символов") String feedback) {
}
