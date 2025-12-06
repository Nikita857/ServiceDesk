package com.bm.wschat.feature.ticket.dto.assignment.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssignmentRejectRequest(
        @NotBlank(message = "Reason is required") @Size(max = 500, message = "Reason must not exceed 500 characters") String reason) {
}
