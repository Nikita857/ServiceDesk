package com.bm.wschat.feature.ticket.dto.timeentry.response;

import com.bm.wschat.feature.ticket.model.TimeEntryType;
import com.bm.wschat.shared.dto.UserShortResponse;

import java.time.Instant;
import java.time.LocalDate;

public record TimeEntryResponse(
        Long id,
        Long ticketId,
        String ticketTitle,
        UserShortResponse specialist,
        Long durationSeconds,
        String formattedDuration,
        String note,
        Instant entryDate,
        LocalDate workDate,
        TimeEntryType activityType,
        Instant createdAt,
        Instant updatedAt) {
}
