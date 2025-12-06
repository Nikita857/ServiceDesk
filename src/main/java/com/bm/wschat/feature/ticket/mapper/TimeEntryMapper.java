package com.bm.wschat.feature.ticket.mapper;

import com.bm.wschat.feature.ticket.dto.timeentry.response.TimeEntryResponse;
import com.bm.wschat.feature.ticket.model.TimeEntry;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.dto.UserShortResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TimeEntryMapper {

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "ticketTitle", source = "ticket.title")
    @Mapping(target = "formattedDuration", expression = "java(timeEntry.getFormattedDuration())")
    TimeEntryResponse toResponse(TimeEntry timeEntry);

    List<TimeEntryResponse> toResponses(List<TimeEntry> timeEntries);

    UserShortResponse toUserShortResponse(User user);
}
