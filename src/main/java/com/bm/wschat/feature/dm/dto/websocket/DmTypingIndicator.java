package com.bm.wschat.feature.dm.dto.websocket;

/**
 * DTO для индикатора печати в личных сообщениях
 */
public record DmTypingIndicator(
        Long senderId,
        String senderFio,
        Long recipientId,
        boolean typing) {
}
