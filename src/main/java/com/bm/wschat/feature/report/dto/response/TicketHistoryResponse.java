package com.bm.wschat.feature.report.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Полная история тикета с временной статистикой.
 */
public record TicketHistoryResponse(
        Long ticketId,
        String title,
        String status,
        String priority,
        String createdByFio,
        String assignedToFio,
        String supportLine,
        Instant createdAt,
        Instant resolvedAt,
        Instant closedAt,
        Instant deletedAt,
        Long firstResponseTimeSeconds,
        Long totalUnassignedSeconds,
        Long totalActiveSeconds,
        List<TicketStatusHistoryResponse> statusHistory) {
}
