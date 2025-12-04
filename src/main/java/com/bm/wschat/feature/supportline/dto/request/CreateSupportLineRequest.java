package com.bm.wschat.feature.supportline.dto.request;

import com.bm.wschat.feature.ticket.model.AssignmentMode;
import jakarta.validation.constraints.*;

public record CreateSupportLineRequest(
        @NotBlank(message = "Name is required") @Size(max = 100, message = "Name must not exceed 100 characters") String name,

        @Size(max = 500, message = "Description must not exceed 500 characters") String description,

        @Min(value = 1, message = "SLA must be at least 1 minute") @Max(value = 10080, message = "SLA must not exceed 1 week (10080 minutes)") Integer slaMinutes,

        @NotNull(message = "Assignment mode is required") AssignmentMode assignmentMode,

        @Min(value = 0, message = "Display order must be non-negative") Integer displayOrder) {
    public CreateSupportLineRequest {
        if (slaMinutes == null) {
            slaMinutes = 1440; // 24 hours default
        }
        if (assignmentMode == null) {
            assignmentMode = AssignmentMode.FIRST_AVAILABLE;
        }
        if (displayOrder == null) {
            displayOrder = 100;
        }
    }
}
