package com.bm.wschat.feature.ticket.dto.timeentry.request;

import com.bm.wschat.feature.ticket.model.TimeEntryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateTimeEntryRequest(
        @NotNull(message = "Duration is required") @Positive(message = "Duration must be positive") Long durationSeconds,

        @Size(max = 1000, message = "Note must not exceed 1000 characters") String note,

        LocalDate workDate,

        TimeEntryType activityType) {

    public CreateTimeEntryRequest {
        if (activityType == null) {
            activityType = TimeEntryType.WORK;
        }
    }
}
