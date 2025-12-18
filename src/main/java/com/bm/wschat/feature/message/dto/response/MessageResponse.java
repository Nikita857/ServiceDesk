package com.bm.wschat.feature.message.dto.response;

import com.bm.wschat.feature.attachment.dto.response.AttachmentResponse;
import com.bm.wschat.feature.user.model.SenderType;
import com.bm.wschat.shared.dto.UserShortResponse;

import java.time.Instant;
import java.util.List;

public record MessageResponse(
                Long id,
                Long ticketId,
                String content,
                UserShortResponse sender,
                SenderType senderType,
                boolean internal,
                boolean readByUser,
                boolean readBySpecialist,
                boolean edited,
                List<AttachmentResponse> attachments,
                Instant createdAt,
                Instant updatedAt) {
}
