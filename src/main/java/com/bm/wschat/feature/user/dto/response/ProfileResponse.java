package com.bm.wschat.feature.user.dto.response;

import java.time.Instant;
import java.util.Set;

/**
 * Полный профиль пользователя для личного кабинета.
 */
public record ProfileResponse(
        Long id,
        String username,
        String fio,
        String email,
        Long telegramId,
        String avatarUrl,
        Set<String> roles,
        boolean isSpecialist,
        Double averageRating,
        Long ratedTicketsCount,
        Instant createdAt) {
}
