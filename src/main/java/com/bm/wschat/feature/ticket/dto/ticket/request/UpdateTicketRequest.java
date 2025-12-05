package com.bm.wschat.feature.ticket.dto.ticket.request;

import com.bm.wschat.feature.ticket.model.TicketPriority;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @Size(max = 250, message = "Title must not exceed 250 characters") String title,

        String description,

        @Size(max = 1000, message = "Link must not exceed 1000 characters") String link1c,

        TicketPriority priority) {
}
