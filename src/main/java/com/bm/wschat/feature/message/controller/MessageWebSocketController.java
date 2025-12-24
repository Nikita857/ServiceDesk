package com.bm.wschat.feature.message.controller;

import com.bm.wschat.feature.message.dto.request.SendMessageRequest;
import com.bm.wschat.feature.message.dto.websocket.TypingIndicator;
import com.bm.wschat.feature.message.service.MessageService;
import com.bm.wschat.feature.user.model.User;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "MessageWebSocketController", description = "Вебсокет контроллер для live чата внутри тикета")
public class MessageWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    /**
     * Отправка сообщения в чат тикета через WebSocket.
     * Клиент отправляет на: /app/ticket/{ticketId}/send
     * Рассылка происходит через MessageService -> RabbitMQ -> TicketEventConsumer
     * -> /topic/ticket/{ticketId}/messages
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

        User user = (User) ((UsernamePasswordAuthenticationToken) principal)
                .getPrincipal();

        // Делегируем логику создания сообщения и публикации событий в сервис
        messageService.sendMessage(ticketId, request, user.getId());
    }

    /**
     * Индикатор набора текста.
     * Клиент отправляет на: /app/ticket/{ticketId}/typing
     * Рассылка на: /topic/ticket/{ticketId}/typing
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
