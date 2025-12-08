package com.bm.wschat.feature.report.dto.response;

/**
 * Статистика по времени решения тикетов
 */
public record ResolutionTimeResponse(
        Long totalResolved,
        Double avgResolutionSeconds,
        Double minResolutionSeconds,
        Double maxResolutionSeconds,
        Double medianResolutionSeconds) {
    public String getFormattedAvgTime() {
        if (avgResolutionSeconds == null)
            return "N/A";
        long hours = (long) (avgResolutionSeconds / 3600);
        long minutes = (long) ((avgResolutionSeconds % 3600) / 60);
        return String.format("%dh %02dm", hours, minutes);
    }
}
