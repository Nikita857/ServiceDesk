package com.bm.wschat.feature.ticket.dto.ticket.response;

import java.util.Map;

/**
 * Статистика тикетов пользователя.
 */
public record UserTicketStatsResponse(
        Long userId,
        String username,
        Long total,
        Long open, // В работе (OPEN, PENDING, ESCALATED)
        Long resolved, // Решённые (RESOLVED, PENDING_CLOSURE)
        Long closed, // Закрытые (CLOSED)
        Long waiting, // Новые, ждут обработки (NEW)
        Map<String, Long> byStatus // Детализация по всем статусам
) {
}
