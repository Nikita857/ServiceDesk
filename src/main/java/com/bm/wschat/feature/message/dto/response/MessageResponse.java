package com.bm.wschat.feature.message.dto.response;

import com.bm.wschat.feature.user.model.SenderType;
import com.bm.wschat.shared.dto.UserShortResponse;

import java.time.Instant;

public record MessageResponse(
        Long id,
        Long ticketId,
        String content,
        UserShortResponse sender,
        SenderType senderType,
        boolean internal,
        boolean readByUser,
        boolean readBySpecialist,
        boolean edited,
        Instant createdAt,
        Instant updatedAt) {
}
