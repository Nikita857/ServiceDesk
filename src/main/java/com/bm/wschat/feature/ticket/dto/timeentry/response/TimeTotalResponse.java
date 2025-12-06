package com.bm.wschat.feature.ticket.dto.timeentry.response;

/**
 * Итого времени по тикету
 */
public record TimeTotalResponse(
        Long ticketId,
        Long totalSeconds,
        String formattedTotal,
        Integer entryCount) {

    public static TimeTotalResponse of(Long ticketId, Long totalSeconds, Integer entryCount) {
        return new TimeTotalResponse(
                ticketId,
                totalSeconds,
                formatDuration(totalSeconds),
                entryCount);
    }

    private static String formatDuration(Long seconds) {
        if (seconds == null || seconds == 0)
            return "0h 00m";
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return String.format("%dh %02dm", hours, minutes);
    }
}
