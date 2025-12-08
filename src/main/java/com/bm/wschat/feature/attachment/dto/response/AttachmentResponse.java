package com.bm.wschat.feature.attachment.dto.response;

import com.bm.wschat.feature.attachment.model.AttachmentType;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        String filename,
        String url,
        Long fileSize,
        String mimeType,
        AttachmentType type,
        Long ticketId,
        Long messageId,
        Long uploadedById,
        String uploadedByUsername,
        Instant createdAt) {
}
