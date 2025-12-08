package com.bm.wschat.feature.ticket.dto.ticket.filter;

import com.bm.wschat.feature.ticket.model.TicketPriority;
import com.bm.wschat.feature.ticket.model.TicketStatus;

import java.time.Instant;
import java.util.List;

public record TicketFilter(
        List<TicketStatus> statuses,
        List<TicketPriority> priorities,
        Long supportLineId,
        Long assignedToId,
        Long createdById,
        Long categoryId,
        Instant createdFrom,
        Instant createdTo,
        Boolean overdueSla) {
}
