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
import com.bm.wschat.feature.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "MessageWebSocketController", description = "Вебсокет контроллер для live чата внутри тикета")
public class MessageWebSocketController {

        private final SimpMessagingTemplate messagingTemplate;
        private final TicketRepository ticketRepository;
        private final MessageRepository messageRepository;
        private final UserService userService;

        /**
         * Send message to ticket chat
         * Client sends to: /app/ticket/{ticketId}/send
         * Broadcast to: /topic/ticket/{ticketId}/messages
         */
        @MessageMapping("/ticket/{ticketId}/send")
        public void sendMessage(
                        @DestinationVariable Long ticketId,
                        @Payload SendMessageRequest request,
                        Principal principal) {

                if (principal == null) {
                        log.warn("Неавторизованная попытка открыть чат тикета {}", ticketId);
                        return;
                }

                User user = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal)
                                .getPrincipal();

                Ticket ticket = ticketRepository.findById(ticketId)
                                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

                // Only specialists can send internal messages
                if (request.internal() && !user.isSpecialist()) {
                        throw new AccessDeniedException("Внутренние сообщения могут отправлять только специалисты");
                }

                // Save message to database
                Message message = Message.builder()
                                .ticket(ticket)
                                .content(request.content())
                                .sender(user)
                                .senderType(SenderType.findMainRole(user.getRoles()))
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
                String destination = "/topic/ticket/" + ticketId + "/messages";

                if (request.internal()) {
                        // Internal messages - send only to specialists of the ticket's support line
                        if (ticket.getSupportLine() != null) {
                                for (User specialist : ticket.getSupportLine().getSpecialists()) {
                                        messagingTemplate.convertAndSendToUser(
                                                        specialist.getUsername(),
                                                        "/queue/ticket/" + ticketId,
                                                        chatMessage);
                                }
                                log.debug("Internal message sent to {} specialists of ticket {}",
                                                ticket.getSupportLine().getSpecialists().size(), ticketId);
                        }
                } else {
                        // Public messages - send to all subscribers
                        messagingTemplate.convertAndSend(destination, chatMessage);
                        log.debug("Message sent to {}: {}", destination, chatMessage.id());
                }
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

                User user = (User) ((UsernamePasswordAuthenticationToken) principal)
                                .getPrincipal();

                TypingIndicator broadcastIndicator = new TypingIndicator(
                                ticketId,
                                user.getId(),
                                user.getUsername(),
                                user.getFio(),
                                indicator.typing());

                messagingTemplate.convertAndSend(
                                "/topic/ticket/" + ticketId + "/typing",
                                broadcastIndicator);
        }
}
