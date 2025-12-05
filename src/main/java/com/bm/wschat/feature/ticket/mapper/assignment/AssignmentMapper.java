package com.bm.wschat.feature.ticket.mapper.assignment;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.ticket.dto.assignment.request.AssignmentCreateRequest;
import com.bm.wschat.feature.ticket.dto.assignment.response.AssignmentResponse;
import com.bm.wschat.feature.ticket.dto.assignment.response.AssignmentShortResponse;
import com.bm.wschat.feature.ticket.model.Assignment;
import com.bm.wschat.feature.user.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AssignmentMapper {

    AssignmentMapper INSTANCE = Mappers.getMapper(AssignmentMapper.class);

    // === CREATE REQUEST → ENTITY ===
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "fromLine", ignore = true)
    @Mapping(target = "fromUser", ignore = true)
    @Mapping(target = "toLine", source = "toLineId", qualifiedByName = "lineById")
    @Mapping(target = "toUser", source = "toUserId", qualifiedByName = "userById")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "acceptedAt", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "rejectedReason", ignore = true)
    Assignment toEntity(AssignmentCreateRequest request);


    // === ENTITY → FULL RESPONSE ===
    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "ticketTitle", source = "ticket.title")
    @Mapping(target = "fromLineId", source = "fromLine.id")
    @Mapping(target = "fromLineName", source = "fromLine.name")
    @Mapping(target = "fromUserId", source = "fromUser.id")
    @Mapping(target = "fromUsername", source = "fromUser.username")
    @Mapping(target = "fromFio", source = "fromUser.fio")
    @Mapping(target = "toLineId", source = "toLine.id")
    @Mapping(target = "toLineName", source = "toLine.name")
    @Mapping(target = "toUserId", source = "toUser.id")
    @Mapping(target = "toUsername", source = "toUser.username")
    @Mapping(target = "toFio", source = "toUser.fio")
    AssignmentResponse toResponse(Assignment assignment);


    // === ENTITY → SHORT RESPONSE (для списков) ===
    @Mapping(target = "toUsername", source = "toUser.username")
    @Mapping(target = "toFio", source = "toUser.fio")
    @Mapping(target = "mode", source = "mode")
    AssignmentShortResponse toShortResponse(Assignment assignment);


    // === Вспомогательные методы для @Named ===
    @Named("lineById")
    default SupportLine lineById(Long id) {
        return id == null ? null : SupportLine.builder().id(id).build();
    }

    @Named("userById")
    default User userById(Long id) {
        return id == null ? null : User.builder().id(id).build();
    }
}
