package com.bm.wschat.feature.dm.dto.response;

import com.bm.wschat.shared.dto.UserShortResponse;

import java.time.Instant;

/**
 * Элемент списка диалогов
 */
public record ConversationResponse(
        UserShortResponse partner,
        String lastMessage,
        Instant lastMessageAt,
        Long unreadCount) {
}
