package com.bm.wschat.feature.ticket.dto.ticket.response;

import java.time.Instant;

/**
 * Ответ с историей статуса тикета.
 */
public record TicketStatusHistoryResponse(
        Long id,
        String status,
        Instant enteredAt,
        Instant exitedAt,
        Long durationSeconds,
        String durationFormatted,
        String changedByUsername,
        String changedByFio,
        String comment) {
}
