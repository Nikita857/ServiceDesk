package com.bm.wschat.feature.report.dto.response;

import java.time.Instant;

/**
 * Запись истории статуса тикета.
 */
public record TicketStatusHistoryResponse(
        String status,
        Instant enteredAt,
        Instant exitedAt,
        Long durationSeconds,
        String durationFormatted,
        String changedByFio,
        String comment) {
}
