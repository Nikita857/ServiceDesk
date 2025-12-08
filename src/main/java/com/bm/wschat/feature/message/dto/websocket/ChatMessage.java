package com.bm.wschat.feature.message.dto.websocket;

import com.bm.wschat.feature.user.model.SenderType;

import java.time.Instant;

/**
 * DTO for real-time chat messages via WebSocket
 */
public record ChatMessage(
        Long id,
        Long ticketId,
        String content,
        Long senderId,
        String senderUsername,
        String senderFio,
        SenderType senderType,
        boolean internal,
        Instant createdAt) {
    public static ChatMessage from(
            Long id, Long ticketId, String content,
            Long senderId, String senderUsername, String senderFio,
            SenderType senderType, boolean internal) {
        return new ChatMessage(
                id, ticketId, content,
                senderId, senderUsername, senderFio,
                senderType, internal, Instant.now());
    }
}
