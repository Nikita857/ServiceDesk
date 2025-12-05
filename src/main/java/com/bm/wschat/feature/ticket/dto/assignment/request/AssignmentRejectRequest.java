package com.bm.wschat.feature.ticket.dto.assignment.request;

import jakarta.validation.constraints.NotBlank;

public class AssignmentRejectRequest {
    @NotBlank String reason;
}
