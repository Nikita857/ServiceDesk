package com.bm.wschat.feature.ticket.dto.ticket.response;

import java.util.Map;

/**
 * Статистика тикетов для линии поддержки.
 */
public record LineTicketStatsResponse(
        Long lineId,
        String lineName,
        Long total,
        Long open, // В работе (OPEN, PENDING, ESCALATED)
        Long resolved, // Решённые (RESOLVED, PENDING_CLOSURE)
        Long closed, // Закрытые (CLOSED)
        Long unassigned, // Ожидают назначения (без assignedTo)
        Long newTickets, // Новые (NEW)
        Map<String, Long> byStatus // Детализация по всем статусам
) {
}
