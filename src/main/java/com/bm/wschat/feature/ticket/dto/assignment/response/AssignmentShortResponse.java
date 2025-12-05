package com.bm.wschat.feature.ticket.dto.assignment.response;

import com.bm.wschat.feature.ticket.model.AssignmentStatus;

import java.time.Instant;

// AssignmentShortResponse.java — для списков (например, в тикете)
public record AssignmentShortResponse(
        Long id,
        Long toUserId,
        String toUsername,
        String toFio,
        AssignmentStatus status,
        Instant createdAt,
        Instant acceptedAt,
        String mode
) {}
