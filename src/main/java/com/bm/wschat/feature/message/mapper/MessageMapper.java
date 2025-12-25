package com.bm.wschat.feature.message.mapper;

import com.bm.wschat.feature.attachment.mapper.AttachmentMapper;
import com.bm.wschat.feature.message.dto.request.SendMessageRequest;
import com.bm.wschat.feature.message.dto.response.MessageResponse;
import com.bm.wschat.feature.message.model.Message;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.dto.UserShortResponse;
import com.bm.wschat.shared.storage.MinioStorageService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

@Mapper(componentModel = "spring", uses = { AttachmentMapper.class })
public abstract class MessageMapper {

    @Autowired
    protected MinioStorageService minioStorageService;

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
    public abstract Message toEntity(SendMessageRequest request);

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "readByUser", expression = "java(message.getReadByUserAt() != null)")
    @Mapping(target = "readBySpecialist", expression = "java(message.getReadBySpecialistAt() != null)")
    @Mapping(target = "edited", expression = "java(message.getEditedAt() != null)")
    @Mapping(target = "sender", expression = "java(toUserShortResponse(message.getSender()))")
    public abstract MessageResponse toResponse(Message message);

    public abstract List<MessageResponse> toResponses(List<Message> messages);

    public UserShortResponse toUserShortResponse(User user) {
        if (user == null) {
            return null;
        }
        String avatarUrl = null;
        if (user.getAvatarUrl() != null) {
            avatarUrl = minioStorageService.generateDownloadUrl(
                    user.getAvatarUrl(),
                    minioStorageService.getBucket(MinioStorageService.BucketType.CHAT),
                    user.getAvatarUrl());
        }
        return new UserShortResponse(user.getId(), user.getUsername(), user.getFio(), avatarUrl);
    }
}
