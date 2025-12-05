package com.bm.wschat.feature.dm.dto.response;

import com.bm.wschat.shared.dto.UserShortResponse;

import java.time.Instant;

public record DirectMessageResponse(
        Long id,
        UserShortResponse sender,
        UserShortResponse recipient,
        String content,
        boolean read,
        boolean edited,
        Instant createdAt) {
}
