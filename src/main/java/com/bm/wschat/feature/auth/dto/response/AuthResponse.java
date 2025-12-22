package com.bm.wschat.feature.auth.dto.response;

import com.bm.wschat.feature.user.dto.response.UserAuthResponse;

import java.time.Instant;

/**
 * Ответ на успешную аутентификацию.
 * 
 * @param accessToken      JWT access token
 * @param refreshToken     Refresh token для обновления
 * @param tokenType        Тип токена (Bearer)
 * @param expiresIn        Время жизни access token в миллисекундах
 * @param expiresAt        Абсолютное время истечения access token (ISO-8601)
 * @param userAuthResponse Информация о пользователе
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Instant expiresAt,
        UserAuthResponse userAuthResponse) {
    public AuthResponse(String accessToken,
            String refreshToken,
            Long expiresIn,
            UserAuthResponse userAuthResponse) {
        this(accessToken, refreshToken, "Bearer", expiresIn,
                Instant.now().plusMillis(expiresIn), userAuthResponse);
    }
}
