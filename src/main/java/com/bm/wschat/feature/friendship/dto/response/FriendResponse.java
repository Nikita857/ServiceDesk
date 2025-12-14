package com.bm.wschat.feature.friendship.dto.response;

import com.bm.wschat.shared.dto.UserShortResponse;

/**
 * DTO для отображения друга в списке
 */
public record FriendResponse(
        Long friendshipId,
        UserShortResponse friend) {
}
