package com.bm.wschat.feature.message.mapper;

import com.bm.wschat.feature.message.dto.request.SendMessageRequest;
import com.bm.wschat.feature.message.dto.response.MessageResponse;
import com.bm.wschat.feature.message.model.Message;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.dto.UserShortResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "senderType", ignore = true)
    @Mapping(target = "readByUserAt", ignore = true)
    @Mapping(target = "readBySpecialistAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "editedAt", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", expression = "java(Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(Instant.now())")
    Message toEntity(SendMessageRequest request);

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "readByUser", expression = "java(message.getReadByUserAt() != null)")
    @Mapping(target = "readBySpecialist", expression = "java(message.getReadBySpecialistAt() != null)")
    @Mapping(target = "edited", expression = "java(message.getEditedAt() != null)")
    MessageResponse toResponse(Message message);

    List<MessageResponse> toResponses(List<Message> messages);

    UserShortResponse toUserShortResponse(User user);
}
