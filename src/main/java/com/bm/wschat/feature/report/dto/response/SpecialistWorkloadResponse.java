package com.bm.wschat.feature.report.dto.response;

/**
 * Загрузка специалиста
 */
public record SpecialistWorkloadResponse(
        Long specialistId,
        String username,
        String fio,
        Long activeTickets, // Тикеты в статусах NEW, IN_PROGRESS, WAITING
        Long resolvedToday, // Решено сегодня
        Long totalTimeToday, // Время за сегодня (секунды)
        Double avgResolutionTime // Среднее время решения (секунды)
) {
    public String getFormattedTimeToday() {
        long hours = totalTimeToday / 3600;
        long minutes = (totalTimeToday % 3600) / 60;
        return String.format("%dh %02dm", hours, minutes);
    }
}
