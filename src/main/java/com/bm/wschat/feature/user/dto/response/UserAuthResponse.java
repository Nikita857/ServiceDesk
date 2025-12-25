package com.bm.wschat.feature.user.dto.response;

import java.util.Set;

public record UserAuthResponse(
                Long id,
                String fio,
                String username,
                String avatarUrl,
                Long telegramId,
                boolean specialist,
                Set<String> roles,
                boolean active) {
}
