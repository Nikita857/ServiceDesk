package com.bm.wschat.feature.ticket.dto.ticket.response;

import com.bm.wschat.feature.ticket.model.TicketPriority;
import com.bm.wschat.feature.ticket.model.TicketStatus;

import java.time.Instant;

public record TicketListResponse(
        Long id,
        String title,
        TicketStatus status,
        TicketPriority priority,
        String createdByUsername,
        String assignedToUsername,
        String supportLineName,
        Instant createdAt,
        Instant slaDeadline) {
}
