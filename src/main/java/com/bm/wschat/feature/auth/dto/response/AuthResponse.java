package com.bm.wschat.feature.auth.dto.response;

import com.bm.wschat.feature.user.dto.response.UserAuthResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserAuthResponse userAuthResponse
) {
    public AuthResponse(String accessToken,
                        String refreshToken,
                        Long expiresIn,
                        UserAuthResponse userAuthResponse) {
        this(accessToken, refreshToken, "Bearer", expiresIn, userAuthResponse);
    }
}
