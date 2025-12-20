package com.bm.wschat.feature.ticket.mapper;

import com.bm.wschat.feature.supportline.dto.response.SupportLineListResponse;
import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.ticket.dto.ticket.request.CreateTicketRequest;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketListResponse;
import com.bm.wschat.feature.ticket.dto.ticket.response.TicketResponse;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.dto.CategoryResponse;
import com.bm.wschat.shared.dto.UserShortResponse;
import com.bm.wschat.shared.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "assignedTo", ignore = true)
    @Mapping(target = "supportLine", ignore = true)
    @Mapping(target = "status", constant = "NEW")
    @Mapping(target = "categoryUser", ignore = true)
    @Mapping(target = "categorySupport", ignore = true)
    @Mapping(target = "timeSpentSeconds", constant = "0L")
    @Mapping(target = "messages", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "slaDeadline", ignore = true)
    @Mapping(target = "resolvedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "feedback", ignore = true)
    @Mapping(target = "telegramMessageThreadId", ignore = true)
    @Mapping(target = "telegramLastBotMessageId", ignore = true)
    @Mapping(target = "firstResponseAt", ignore = true)
    @Mapping(target = "closureRequestedBy", ignore = true)
    @Mapping(target = "closureRequestedAt", ignore = true)
    @Mapping(target = "escalated", constant = "false")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(Instant.now())")
    Ticket toEntity(CreateTicketRequest request);

    @Mapping(target = "messageCount", expression = "java(ticket.getMessages() != null ? ticket.getMessages().size() : 0)")
    @Mapping(target = "attachmentCount", expression = "java(ticket.getAttachments() != null ? ticket.getAttachments().size() : 0)")
    @Mapping(target = "lastAssignment", ignore = true)
    TicketResponse toResponse(Ticket ticket);

    @Mapping(target = "createdByUsername", source = "createdBy.username")
    @Mapping(target = "assignedToUsername", source = "assignedTo.username")
    @Mapping(target = "supportLineName", source = "supportLine.name")
    TicketListResponse toListResponse(Ticket ticket);

    List<TicketListResponse> toListResponses(List<Ticket> tickets);

    // User mapping
    UserShortResponse toUserShortResponse(User user);

    // Category mapping
    CategoryResponse toCategoryResponse(Category category);

    // SupportLine mapping
    @Mapping(target = "specialistCount", expression = "java(line.getSpecialists() != null ? line.getSpecialists().size() : 0)")
    SupportLineListResponse toSupportLineListResponse(SupportLine line);
}
