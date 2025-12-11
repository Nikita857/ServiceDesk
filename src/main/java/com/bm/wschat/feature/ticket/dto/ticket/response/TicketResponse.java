package com.bm.wschat.feature.ticket.dto.ticket.response;

import com.bm.wschat.feature.ticket.dto.assignment.response.AssignmentResponse;
import com.bm.wschat.feature.ticket.model.TicketPriority;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.shared.dto.CategoryResponse;
import com.bm.wschat.shared.dto.UserShortResponse;
import com.bm.wschat.feature.supportline.dto.response.SupportLineListResponse;

import java.time.Instant;

public record TicketResponse(
                Long id,
                String title,
                String description,
                String link1c,
                TicketStatus status,
                TicketPriority priority,

                UserShortResponse createdBy,
                UserShortResponse assignedTo,
                SupportLineListResponse supportLine,
                CategoryResponse categoryUser,
                CategoryResponse categorySupport,

                Long timeSpentSeconds,
                Integer messageCount,
                Integer attachmentCount,

                Instant slaDeadline,
                Instant resolvedAt,
                Instant closedAt,
                Instant createdAt,
                Instant updatedAt,

                // Последнее назначение (включает причину отклонения если было)
                AssignmentResponse lastAssignment) {
}
