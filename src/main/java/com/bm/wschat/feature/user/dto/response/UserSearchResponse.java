package com.bm.wschat.feature.user.dto.response;

/**
 * DTO для результата поиска пользователей
 */
public record UserSearchResponse(
        Long id,
        String username,
        String fio) {
}
