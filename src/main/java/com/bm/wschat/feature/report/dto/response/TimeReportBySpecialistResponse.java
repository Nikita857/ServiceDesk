package com.bm.wschat.feature.report.dto.response;

/**
 * Отчет по времени для одного специалиста
 */
public record TimeReportBySpecialistResponse(
        Long specialistId,
        String username,
        String fio,
        Long totalSeconds,
        Long ticketCount) {
    public String getFormattedTime() {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return String.format("%dh %02dm", hours, minutes);
    }
}
