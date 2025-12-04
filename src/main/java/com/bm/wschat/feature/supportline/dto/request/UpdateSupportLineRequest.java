package com.bm.wschat.feature.supportline.dto.request;

import com.bm.wschat.feature.ticket.model.AssignmentMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateSupportLineRequest(
        @Size(max = 500, message = "Description must not exceed 500 characters") String description,

        @Min(value = 1, message = "SLA must be at least 1 minute") @Max(value = 10080, message = "SLA must not exceed 1 week") Integer slaMinutes,

        AssignmentMode assignmentMode,

        @Min(value = 0, message = "Display order must be non-negative") Integer displayOrder) {
}
