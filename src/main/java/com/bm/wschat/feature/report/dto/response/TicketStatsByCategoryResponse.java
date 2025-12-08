package com.bm.wschat.feature.report.dto.response;

/**
 * Статистика тикетов по категории
 */
public record TicketStatsByCategoryResponse(
        Long categoryId,
        String categoryName,
        String categoryType, // "USER" or "SUPPORT"
        Long count,
        Double percentage) {
}
