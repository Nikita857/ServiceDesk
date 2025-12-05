package com.bm.wschat.feature.ticket.dto.assignment.response;

import com.bm.wschat.feature.ticket.model.AssignmentMode;
import com.bm.wschat.feature.ticket.model.AssignmentStatus;

import java.time.Instant;

// AssignmentResponse.java — полный ответ (для GET)
public record AssignmentResponse(
        Long id,

        // Тикет
        Long ticketId,
        String ticketTitle,

        // Откуда
        Long fromLineId,
        String fromLineName,
        Long fromUserId,
        String fromUsername,
        String fromFio,

        // Куда
        Long toLineId,
        String toLineName,
        Long toUserId,
        String toUsername,
        String toFio,

        // Данные назначения
        String note,
        AssignmentMode mode,
        AssignmentStatus status,

        // Даты
        Instant createdAt,
        Instant acceptedAt,
        Instant rejectedAt,
        String rejectedReason
) {}
