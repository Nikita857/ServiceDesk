package com.bm.wschat.feature.attachment.repository;

import com.bm.wschat.feature.attachment.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    List<Attachment> findByMessageIdOrderByCreatedAtDesc(Long messageId);

    List<Attachment> findByUploadedByIdOrderByCreatedAtDesc(Long userId);

    Long countByTicketId(Long ticketId);

    Long countByMessageId(Long messageId);
}
