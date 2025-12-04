package com.bm.wschat.feature.user.dto.response;

import java.util.Set;

public record UserAuthResponse(
        String FIO,
        String username,
        Set<String> roles
) {
}
