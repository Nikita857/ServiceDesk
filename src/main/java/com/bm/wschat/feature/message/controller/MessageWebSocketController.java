package com.bm.wschat.feature.message.controller;

import com.bm.wschat.feature.message.dto.request.SendMessageRequest;
import com.bm.wschat.feature.message.dto.websocket.ChatMessage;
import com.bm.wschat.feature.message.dto.websocket.TypingIndicator;
import com.bm.wschat.feature.message.model.Message;
import com.bm.wschat.feature.message.repository.MessageRepository;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.model.SenderType;
import com.bm.wschat.feature.user.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "MessageWebSocketController", description = "Websocket controller for live message exchange inside of ticket")
public class MessageWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final TicketRepository ticketRepository;
    private final MessageRepository messageRepository;

    /**
     * Send message to ticket chat
     * Client sends to: /app/ticket/{ticketId}/send
     * Broadcast to: /topic/ticket/{ticketId}
     */
    @MessageMapping("/ticket/{ticketId}/send")
    public void sendMessage(
            @DestinationVariable Long ticketId,
            @Payload SendMessageRequest request,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthorized WebSocket message attempt to ticket {}", ticketId);
            return;
        }

        User user = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal)
                .getPrincipal();

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

        // Only specialists can send internal messages
        if (request.internal() && !user.isSpecialist()) {
            throw new AccessDeniedException("Only specialists can send internal messages");
        }

        // Save message to database
        Message message = Message.builder()
                .ticket(ticket)
                .content(request.content())
                .sender(user)
                .senderType(user.isSpecialist() ? SenderType.SPECIALIST : SenderType.USER)
                .internal(request.internal())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Message saved = messageRepository.save(message);

        // Create broadcast message
        ChatMessage chatMessage = ChatMessage.from(
                saved.getId(),
                ticketId,
                saved.getContent(),
                user.getId(),
                user.getUsername(),
                user.getFio(),
                saved.getSenderType(),
                saved.isInternal());

        // Broadcast to all subscribers of this ticket
        String destination = "/topic/ticket/" + ticketId;

        if (request.internal()) {
            // Internal messages - send only to specialists
            // TODO: implement user-based filtering for internal messages
            log.debug("Sending internal message to ticket {}", ticketId);
        }

        messagingTemplate.convertAndSend(destination, chatMessage);
        log.debug("Message sent to {}: {}", destination, chatMessage.id());
    }

    /**
     * Typing indicator
     * Client sends to: /app/ticket/{ticketId}/typing
     * Broadcast to: /topic/ticket/{ticketId}/typing
     */
    @MessageMapping("/ticket/{ticketId}/typing")
    public void sendTypingIndicator(
            @DestinationVariable Long ticketId,
            @Payload TypingIndicator indicator,
            Principal principal) {

        if (principal == null)
            return;

        User user = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal)
                .getPrincipal();

        TypingIndicator broadcastIndicator = new TypingIndicator(
                ticketId,
                user.getId(),
                user.getUsername(),
                indicator.typing());

        messagingTemplate.convertAndSend(
                "/topic/ticket/" + ticketId + "/typing",
                broadcastIndicator);
    }
}
