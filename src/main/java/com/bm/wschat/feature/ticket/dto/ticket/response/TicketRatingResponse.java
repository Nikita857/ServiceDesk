package com.bm.wschat.feature.ticket.dto.ticket.response;

import java.time.Instant;

/**
 * Ответ с информацией об оценке тикета.
 */
public record TicketRatingResponse(
        Long ticketId,
        String ticketTitle,
        Integer rating,
        String feedback,
        Instant ratedAt,
        String ratedByUsername,
        String ratedByFio,
        String assignedToUsername,
        String assignedToFio) {
}
