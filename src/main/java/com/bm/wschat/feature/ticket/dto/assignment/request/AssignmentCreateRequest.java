package com.bm.wschat.feature.ticket.dto.assignment.request;

import com.bm.wschat.feature.ticket.model.AssignmentMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// AssignmentCreateRequest.java — для создания назначения (POST)
public record AssignmentCreateRequest(
        @NotNull(message = "Ticket ID cannot be empty")
        Long ticketId,

        // Куда назначаем
        @NotNull(message = "Target line ID cannot be empty")
        Long toLineId,

        Long toUserId,  // опционально — если DIRECT

        // Откуда (для истории эскалации)
        Long fromLineId,
        Long fromUserId,

        @NotBlank(message = "Note cannot be empty")
        @Size(max = 1000, message = "Note too long")
        String note,

        AssignmentMode mode // по умолчанию FIRST_AVAILABLE в сервисе
) {}
