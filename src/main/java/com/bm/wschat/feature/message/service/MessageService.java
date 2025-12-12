package com.bm.wschat.feature.message.service;

import com.bm.wschat.feature.message.dto.request.EditMessageRequest;
import com.bm.wschat.feature.message.dto.request.SendMessageRequest;
import com.bm.wschat.feature.message.dto.response.MessageResponse;
import com.bm.wschat.feature.message.mapper.MessageMapper;
import com.bm.wschat.feature.message.model.Message;
import com.bm.wschat.feature.message.repository.MessageRepository;
import com.bm.wschat.feature.notification.model.Notification;
import com.bm.wschat.feature.notification.service.NotificationService;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketStatus;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.model.SenderType;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final MessageMapper messageMapper;
    private final NotificationService notificationService;

    @Transactional
    public MessageResponse sendMessage(Long ticketId, SendMessageRequest request, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Тикет не найден: " + ticketId));

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        // Моя реализация блокировки отправки отправки сообщений в тикете

        if (ticket.getStatus().equals(TicketStatus.CLOSED)) {
            throw new AccessDeniedException("Тикет закрыт. Отправка сообщений запрещена");
        }

        // Internal messages only for specialists
        if (request.internal() && !sender.isSpecialist()) {
            throw new AccessDeniedException("Только специалисты могут отправлять внутренние сообщения");
        }

        Message message = messageMapper.toEntity(request);
        message.setTicket(ticket);
        message.setSender(sender);
        message.setSenderType(sender.isSpecialist() ? SenderType.SYSADMIN : SenderType.USER);

        Message saved = messageRepository.save(message);

        // Отправка уведомлений участникам тикета (кроме отправителя)
        sendMessageNotifications(ticket, sender, saved.getContent());

        return messageMapper.toResponse(saved);
    }

    /**
     * Отправить уведомления всем участникам тикета о новом сообщении
     */
    private void sendMessageNotifications(Ticket ticket, User sender, String messageContent) {
        Set<Long> recipientIds = new HashSet<>();

        // Создатель тикета
        if (ticket.getCreatedBy() != null) {
            recipientIds.add(ticket.getCreatedBy().getId());
        }

        // Назначенный специалист
        if (ticket.getAssignedTo() != null) {
            recipientIds.add(ticket.getAssignedTo().getId());
        }

        // Удаляем отправителя
        recipientIds.remove(sender.getId());

        if (!recipientIds.isEmpty()) {
            Notification notification = Notification.message(
                    ticket.getId(),
                    ticket.getTitle(),
                    sender.getId(),
                    sender.getFio() != null ? sender.getFio() : sender.getUsername(),
                    messageContent);
            notificationService.notifyUsers(recipientIds, notification);
        }
    }

    public Page<MessageResponse> getTicketMessages(Long ticketId, Pageable pageable, User user) {
        // Check ticket exists
        if (!ticketRepository.existsById(ticketId)) {
            throw new EntityNotFoundException("Ticket not found with id: " + ticketId);
        }

        Page<Message> messages;
        if (user.isSpecialist()) {
            // Specialists see all messages including internal
            messages = messageRepository.findByTicketIdOrderByCreatedAtDesc(ticketId, pageable);
        } else {
            // Regular users see only public messages
            messages = messageRepository.findByTicketIdAndInternalFalseOrderByCreatedAtDesc(ticketId, pageable);
        }

        return messages.map(messageMapper::toResponse);
    }

    @Transactional
    public int markAsRead(Long ticketId, User user) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new EntityNotFoundException("Ticket not found with id: " + ticketId);
        }

        Instant now = Instant.now();
        if (user.isSpecialist()) {
            return messageRepository.markAsReadBySpecialist(ticketId, user.getId(), now);
        } else {
            return messageRepository.markAsReadByUser(ticketId, user.getId(), now);
        }
    }

    @Transactional
    public MessageResponse editMessage(Long messageId, EditMessageRequest request, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));

        // Only sender can edit their message
        if (message.getSender() == null || !message.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("You can only edit your own messages");
        }

        message.setContent(request.content());
        message.setEditedAt(Instant.now());

        Message updated = messageRepository.save(message);
        return messageMapper.toResponse(updated);
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with id: " + messageId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // Sender or admin can delete
        boolean isSender = message.getSender() != null && message.getSender().getId().equals(userId);
        boolean isAdmin = user.isAdmin();

        if (!isSender && !isAdmin) {
            throw new AccessDeniedException("You can only delete your own messages");
        }

        messageRepository.delete(message); // Soft delete via @SQLDelete
    }

    public Long getUnreadCount(Long ticketId, User user) {
        if (user.isSpecialist()) {
            return messageRepository.countUnreadByTicketForSpecialist(ticketId, user.getId());
        } else {
            return messageRepository.countUnreadByTicketForUser(ticketId, user.getId());
        }
    }
}
