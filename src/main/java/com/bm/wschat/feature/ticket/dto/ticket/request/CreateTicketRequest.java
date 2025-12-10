package com.bm.wschat.feature.ticket.dto.ticket.request;

import com.bm.wschat.feature.ticket.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
        @NotBlank(message = "Title is required") @Size(max = 250, message = "Title must not exceed 250 characters") String title,

        @NotBlank(message = "Description is required") String description,

        @Size(max = 1000, message = "Link must not exceed 1000 characters") String link1c,

        Long categoryUserId,

        TicketPriority priority,

        Long supportLineId,

        // Опционально: назначить конкретному специалисту
        Long assignToUserId) {
    public CreateTicketRequest {
        if (priority == null) {
            priority = TicketPriority.MEDIUM;
        }
    }
}
