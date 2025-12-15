package com.bm.wschat.feature.dm.controller;

import com.bm.wschat.feature.dm.dto.request.SendDirectMessageRequest;
import com.bm.wschat.feature.dm.dto.response.DirectMessageResponse;
import com.bm.wschat.feature.dm.dto.websocket.DmTypingIndicator;
import com.bm.wschat.feature.dm.service.DirectMessageService;
import com.bm.wschat.feature.user.model.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
@Tag(name = "DirectMessageWebSocketController", description = "Websocket controller for real time message exchange")
public class DirectMessageWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final DirectMessageService dmService;

    /**
     * Отправить личное сообщение через WebSocket
     * Client sends to: /app/dm/send
     * Delivers to: /user/{recipientId}/queue/private
     */
    @MessageMapping("/dm/send")
    public void sendDirectMessage(
            @Payload SendDirectMessageRequest request,
            Principal principal) {

        if (principal == null) {
            log.warn("Попытка подключиться неавторизованного пользователя");
            return;
        }

        User sender = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal)
                .getPrincipal();

        try {
            DirectMessageResponse response = dmService.sendMessage(request, sender.getId());

            // Отправить получателю
            messagingTemplate.convertAndSendToUser(
                    request.recipientId().toString(),
                    "/queue/private",
                    response);

            // Отправить отправителю (подтверждение)
            messagingTemplate.convertAndSendToUser(
                    sender.getId().toString(),
                    "/queue/private",
                    response);

            log.debug("Сообщение отправлено {} -> {}", sender.getId(), request.recipientId());

        } catch (Exception e) {
            log.error("Не удалось отправиь личное сообщение: {}", e.getMessage());
        }
    }

    /**
     * Отправить индикатор печати в личном чате
     * Client sends to: /app/dm/typing
     * Delivers to: /user/{recipientId}/queue/dm-typing
     */
    @MessageMapping("/dm/typing")
    public void sendTypingIndicator(
            @Payload DmTypingIndicator indicator,
            Principal principal) {

        if (principal == null) {
            log.warn("Unauthorized typing indicator attempt");
            return;
        }

        User sender = (User) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal)
                .getPrincipal();

        // Отправить получателю индикатор печати
        DmTypingIndicator outgoing = new DmTypingIndicator(
                sender.getId(),
                sender.getFio(),
                indicator.recipientId(),
                indicator.typing());

        messagingTemplate.convertAndSendToUser(
                indicator.recipientId().toString(),
                "/queue/dm-typing",
                outgoing);

        log.debug("Индикатор печати отправлен {} -> {}: {}",
                sender.getId(), indicator.recipientId(), indicator.typing());
    }
}
