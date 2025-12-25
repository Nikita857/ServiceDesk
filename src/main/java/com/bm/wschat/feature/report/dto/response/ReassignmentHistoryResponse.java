package com.bm.wschat.feature.report.dto.response;

import java.time.Instant;

/**
 * Запись истории переназначений тикета.
 */
public record ReassignmentHistoryResponse(
        Long assignmentId,
        String fromUserFio,
        String toUserFio,
        String fromLine,
        String toLine,
        String mode,
        String status,
        String note,
        Instant createdAt,
        Instant acceptedAt,
        Instant rejectedAt,
        String rejectedReason) {
}
