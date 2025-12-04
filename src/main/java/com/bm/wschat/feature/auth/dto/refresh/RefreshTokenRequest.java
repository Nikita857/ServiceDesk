package com.bm.wschat.feature.auth.dto.refresh;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
       @NotBlank
       String refreshToken
) {
}
