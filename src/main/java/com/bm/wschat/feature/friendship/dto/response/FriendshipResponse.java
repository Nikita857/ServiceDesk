package com.bm.wschat.feature.friendship.dto.response;

import com.bm.wschat.feature.friendship.model.FriendshipStatus;
import com.bm.wschat.shared.dto.UserShortResponse;

import java.time.Instant;

/**
 * DTO ответа с информацией о дружбе/запросе
 */
public record FriendshipResponse(
        Long id,
        UserShortResponse requester,
        UserShortResponse addressee,
        FriendshipStatus status,
        Instant requestedAt,
        Instant respondedAt) {
}
