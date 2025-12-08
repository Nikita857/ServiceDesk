package com.bm.wschat.feature.report.dto.response;

import com.bm.wschat.feature.ticket.model.TicketStatus;

/**
 * Статистика тикетов по статусу
 */
public record TicketStatsByStatusResponse(
        TicketStatus status,
        Long count,
        Double percentage) {
}
