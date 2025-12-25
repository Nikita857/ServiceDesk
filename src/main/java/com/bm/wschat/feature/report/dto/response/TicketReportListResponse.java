package com.bm.wschat.feature.report.dto.response;

import java.time.Instant;

/**
 * Краткая информация о тикете для списка отчётов.
 * Включает soft-deleted тикеты.
 */
public record TicketReportListResponse(
        Long id,
        String title,
        String status,
        String priority,
        String createdByFio,
        String assignedToFio,
        String supportLine,
        Instant createdAt,
        Instant closedAt,
        Instant deletedAt,
        boolean isDeleted) {
}
