package com.bm.wschat.feature.attachment.service;

import com.bm.wschat.feature.attachment.dto.response.AttachmentResponse;
import com.bm.wschat.feature.attachment.mapper.AttachmentMapper;
import com.bm.wschat.feature.attachment.model.Attachment;
import com.bm.wschat.feature.attachment.model.AttachmentType;
import com.bm.wschat.feature.attachment.repository.AttachmentRepository;
import com.bm.wschat.feature.dm.model.DirectMessage;
import com.bm.wschat.feature.dm.repository.DirectMessageRepository;
import com.bm.wschat.feature.message.model.Message;
import com.bm.wschat.feature.message.repository.MessageRepository;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.messaging.TicketEventPublisher;
import com.bm.wschat.shared.messaging.event.TicketEvent;
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
    private final DirectMessageRepository directMessageRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AttachmentMapper attachmentMapper;
    private final TicketEventPublisher ticketEventPublisher;

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
            throw new IllegalArgumentException("Файл пустой");
        }

        // Проверка размера
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Максимальный размер файла 10 МБ");
        }

        // Проверка расширения
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lowerName = filename.toLowerCase();
            for (String ext : BLOCKED_EXTENSIONS) {
                if (lowerName.endsWith(ext)) {
                    throw new IllegalArgumentException("Неразрешенный тип файла: " + ext);
                }
            }
        }
    }

    @Transactional
    public AttachmentResponse uploadToTicket(Long ticketId, MultipartFile file, Long userId) {
        validateFile(file);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + ticketId));

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

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
        log.info("Вложение прикреплено к тикету {}: {}", ticketId, saved.getId());

        AttachmentResponse response = attachmentMapper.toResponse(saved);

        // Публикуем событие добавления вложения через RabbitMQ
        ticketEventPublisher.publish(TicketEvent.of(
                com.bm.wschat.shared.messaging.TicketEventType.ATTACHMENT_ADDED,
                ticketId, userId, response));

        return response;
    }

    @Transactional
    public AttachmentResponse uploadToMessage(Long messageId, MultipartFile file, Long userId) {
        validateFile(file);

        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Сообщение не найдено: " + messageId));

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

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
        log.info("Вложение прикреплено к сообщению {}: {}", messageId, saved.getId());

        AttachmentResponse response = attachmentMapper.toResponse(saved);

        // Публикуем событие добавления вложения через RabbitMQ
        ticketEventPublisher.publish(TicketEvent.of(
                com.bm.wschat.shared.messaging.TicketEventType.ATTACHMENT_ADDED,
                message.getTicket().getId(), userId, response));

        return response;
    }

    @Transactional
    public AttachmentResponse uploadToDirectMessage(Long dmId, MultipartFile file, Long userId) {
        validateFile(file);

        DirectMessage dm = findDirectMessageById(dmId);

        User uploader = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        // Проверяем, что пользователь — участник переписки
        if (!dm.getSender().getId().equals(userId) && !dm.getRecipient().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "Вы можете прикреплять вложения только в своей переписке");
        }

        String storedFilename = fileStorageService.store(file);

        Attachment attachment = Attachment.builder()
                .directMessage(dm)
                .filename(file.getOriginalFilename())
                .url(fileStorageService.getUrl(storedFilename))
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .type(detectType(file.getContentType()))
                .uploadedBy(uploader)
                .createdAt(Instant.now())
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        log.info("Вложение к прикреплено к личному сообщению {}: {}", dmId, saved.getId());
        return attachmentMapper.toResponse(saved);
    }

    private DirectMessage findDirectMessageById(Long dmId) {
        return directMessageRepository.findById(dmId)
                .orElseThrow(() -> new EntityNotFoundException("Личное сообщение не найдено: " + dmId));
    }

    public List<AttachmentResponse> getByTicketId(Long ticketId) {
        List<Attachment> attachments = attachmentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
        return attachmentMapper.toResponses(attachments);
    }

    public List<AttachmentResponse> getByMessageId(Long messageId) {
        List<Attachment> attachments = attachmentRepository.findByMessageIdOrderByCreatedAtDesc(messageId);
        return attachmentMapper.toResponses(attachments);
    }

    public List<AttachmentResponse> getByDirectMessageId(Long dmId) {
        List<Attachment> attachments = attachmentRepository.findByDirectMessageIdOrderByCreatedAtDesc(dmId);
        return attachmentMapper.toResponses(attachments);
    }

    public AttachmentResponse getById(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Вложение не найдено: " + attachmentId));
        return attachmentMapper.toResponse(attachment);
    }

    public Resource download(String filename) {
        return fileStorageService.load(filename);
    }

    /**
     * Скачать файл по ID вложения.
     * Возвращает ресурс и оригинальное имя файла.
     */
    public DownloadResult downloadById(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Вложение не найдено: " + attachmentId));

        // Извлекаем имя файла из URL (последняя часть пути)
        String storedFilename = extractFilenameFromUrl(attachment.getUrl());
        Resource resource = fileStorageService.load(storedFilename);

        return new DownloadResult(
                resource,
                attachment.getFilename(), // оригинальное имя
                attachment.getMimeType());
    }

    private String extractFilenameFromUrl(String url) {
        if (url == null)
            return null;
        // URL может быть "/uploads/uuid.ext" или "http://...../uuid.ext"
        int lastSlash = url.lastIndexOf('/');
        return lastSlash >= 0 ? url.substring(lastSlash + 1) : url;
    }

    /**
     * Результат скачивания: ресурс + метаданные для headers.
     */
    public record DownloadResult(Resource resource, String originalFilename, String mimeType) {
    }

    @Transactional
    public void delete(Long attachmentId, Long userId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Вложение не найдено: " + attachmentId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        boolean isUploader = attachment.getUploadedBy() != null &&
                attachment.getUploadedBy().getId().equals(userId);
        boolean isAdmin = user.isAdmin();

        if (!isUploader && !isAdmin) {
            throw new AccessDeniedException("Вы можете удалять только свои вложения");
        }

        // Extract filename from URL
        String url = attachment.getUrl();
        String filename = url.substring(url.lastIndexOf('/') + 1);
        fileStorageService.delete(filename);

        attachmentRepository.delete(attachment); // Soft delete
        log.info("Вложение удалено: {} Пользователь {}", attachmentId, userId);
    }

    private AttachmentType detectType(String mimeType) {
        if (mimeType == null) {
            return AttachmentType.DOCUMENT;
        }

        return switch (mimeType.split("/")[0]) {
            case "image" -> AttachmentType.PHOTO;
            case "video" -> AttachmentType.VIDEO;
            default -> AttachmentType.DOCUMENT;
        };
    }
}
