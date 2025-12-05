package com.bm.wschat.feature.ticket.dto.ticket.request;

import com.bm.wschat.feature.ticket.model.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull(message = "Status is required") TicketStatus status,

        String comment) {
}
