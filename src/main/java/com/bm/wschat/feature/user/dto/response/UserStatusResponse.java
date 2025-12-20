package com.bm.wschat.feature.user.dto.response;

import com.bm.wschat.feature.user.model.UserActivityStatus;

import java.time.Instant;

/**
 * DTO для ответа со статусом активности пользователя.
 *
 * @param status                 текущий статус активности
 * @param availableForAssignment доступен ли для назначения тикетов
 * @param updatedAt              время последнего обновления статуса
 */
public record UserStatusResponse(
        UserActivityStatus status,
        boolean availableForAssignment,
        Instant updatedAt) {
}
