package com.bm.wschat.feature.report.dto.response;

/**
 * Отчет по времени для линии поддержки
 */
public record TimeReportByLineResponse(
        Long lineId,
        String lineName,
        Integer lineLevel,
        Long totalSeconds,
        Long ticketCount,
        Long specialistCount) {
    public String getFormattedTime() {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return String.format("%dh %02dm", hours, minutes);
    }
}
