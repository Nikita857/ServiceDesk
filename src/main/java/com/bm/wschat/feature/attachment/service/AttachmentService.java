package com.bm.wschat.feature.attachment.service;

import com.bm.wschat.feature.attachment.dto.response.AttachmentResponse;
import com.bm.wschat.feature.attachment.mapper.AttachmentMapper;
import com.bm.wschat.feature.attachment.model.Attachment;
import com.bm.wschat.feature.attachment.model.AttachmentType;
import com.bm.wschat.feature.attachment.repository.AttachmentRepository;
import com.bm.wschat.feature.message.model.Message;
import com.bm.wschat.feature.message.repository.MessageRepository;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.service.FileStorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AttachmentMapper attachmentMapper;

    // Опасные расширения файлов, которые блокируем
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".exe", ".bat", ".cmd", ".sh", ".ps1", ".msi", ".dll",
            ".scr", ".vbs", ".js", ".jar", ".com", ".pif", ".hta");

    // Максимальный размер файла: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Валидация файла перед загрузкой
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Проверка размера
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed (10MB)");
        }

        // Проверка расширения
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lowerName = filename.toLowerCase();
            for (String ext : BLOCKED_EXTENSIONS) {
                if (lowerName.endsWith(ext)) {
                    throw new IllegalArgumentException("File type not allowed: " + ext);
                }
            }
        }
    }

    @Transactional
    public AttachmentResponse uploadToTicket(Long ticketId, MultipartFile file, Long userId) {
        validateFile(file);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        String storedFilename = fileStorageService.store(file);

        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .filename(file.getOriginalFilename())
                .url(fileStorageService.getUrl(storedFilename))
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .type(detectType(file.getContentType()))
                .uploadedBy(uploader)
                .createdAt(Instant.now())
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        log.info("Attachment uploaded to ticket {}: {}", ticketId, saved.getId());
        return attachmentMapper.toResponse(saved);
    }

    @Transactional
    public AttachmentResponse uploadToMessage(Long messageId, MultipartFile file, Long userId) {
        validateFile(file);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        String storedFilename = fileStorageService.store(file);

        Attachment attachment = Attachment.builder()
                .message(message)
                .filename(file.getOriginalFilename())
                .url(fileStorageService.getUrl(storedFilename))
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .type(detectType(file.getContentType()))
                .uploadedBy(uploader)
                .createdAt(Instant.now())
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        log.info("Attachment uploaded to message {}: {}", messageId, saved.getId());
        return attachmentMapper.toResponse(saved);
    }

    public List<AttachmentResponse> getByTicketId(Long ticketId) {
        List<Attachment> attachments = attachmentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
        return attachmentMapper.toResponses(attachments);
    }

    public List<AttachmentResponse> getByMessageId(Long messageId) {
        List<Attachment> attachments = attachmentRepository.findByMessageIdOrderByCreatedAtDesc(messageId);
        return attachmentMapper.toResponses(attachments);
    }

    public AttachmentResponse getById(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found: " + attachmentId));
        return attachmentMapper.toResponse(attachment);
    }

    public Resource download(String filename) {
        return fileStorageService.load(filename);
    }

    @Transactional
    public void delete(Long attachmentId, Long userId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found: " + attachmentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        boolean isUploader = attachment.getUploadedBy() != null &&
                attachment.getUploadedBy().getId().equals(userId);
        boolean isAdmin = user.isAdmin();

        if (!isUploader && !isAdmin) {
            throw new AccessDeniedException("You can only delete your own attachments");
        }

        // Extract filename from URL
        String url = attachment.getUrl();
        String filename = url.substring(url.lastIndexOf('/') + 1);
        fileStorageService.delete(filename);

        attachmentRepository.delete(attachment); // Soft delete
        log.info("Attachment deleted: {} by user {}", attachmentId, userId);
    }

    private AttachmentType detectType(String mimeType) {
        if (mimeType == null)
            return AttachmentType.DOCUMENT;

        if (mimeType.startsWith("image/")) {
            return AttachmentType.PHOTO;
        } else if (mimeType.startsWith("video/")) {
            return AttachmentType.VIDEO;
        } else {
            return AttachmentType.DOCUMENT;
        }
    }
}
