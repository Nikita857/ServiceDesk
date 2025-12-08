package com.bm.wschat.feature.attachment.mapper;

import com.bm.wschat.feature.attachment.dto.response.AttachmentResponse;
import com.bm.wschat.feature.attachment.model.Attachment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    @Mapping(target = "ticketId", source = "ticket.id")
    @Mapping(target = "messageId", source = "message.id")
    @Mapping(target = "uploadedById", source = "uploadedBy.id")
    @Mapping(target = "uploadedByUsername", source = "uploadedBy.username")
    AttachmentResponse toResponse(Attachment attachment);

    List<AttachmentResponse> toResponses(List<Attachment> attachments);
}
