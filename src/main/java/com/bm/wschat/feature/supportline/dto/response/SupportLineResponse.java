package com.bm.wschat.feature.supportline.dto.response;

import com.bm.wschat.feature.ticket.model.AssignmentMode;

import java.time.Instant;
import java.util.List;

public record SupportLineResponse(
        Long id,
        String name,
        String description,
        Integer slaMinutes,
        AssignmentMode assignmentMode,
        Integer displayOrder,
        Integer specialistCount,
        List<SpecialistResponse> specialists,
        Instant createdAt,
        Instant updatedAt) {
}
