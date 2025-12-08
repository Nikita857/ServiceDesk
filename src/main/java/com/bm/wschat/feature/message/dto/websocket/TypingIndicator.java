package com.bm.wschat.feature.message.dto.websocket;

/**
 * DTO for typing indicator events
 */
public record TypingIndicator(
        Long ticketId,
        Long userId,
        String username,
        boolean typing) {
}
