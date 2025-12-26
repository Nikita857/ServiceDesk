package com.bm.wschat.shared.messaging.consumer;

import com.bm.wschat.shared.messaging.event.TicketEvent;
import com.bm.wschat.shared.messaging.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Консьюмер событий тикетов из RabbitMQ.
 * Обрабатывает события и отправляет WebSocket push на фронт.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.TICKET_QUEUE)
    public void handleTicketEvent(TicketEvent event) {
        log.debug("Received ticket event: type={}, ticketId={}", event.type(), event.ticketId());

        switch (event.type()) {
            case CREATED -> handleCreated(event);
            case UPDATED, STATUS_CHANGED, ASSIGNED, RATED -> handleUpdate(event);
            case MESSAGE_SENT, MESSAGE_UPDATED -> handleMessage(event);
            case DELETED -> handleDeleted(event);
            case ATTACHMENT_ADDED -> handleAttachment(event);
            case INTERNAL_COMMENT -> handleInternalComment(event);
            case SLA_BREACH -> handleSlaBreach(event);
            case USER_STATUS_CHANGED -> handleUserStatusChanged(event);
            case ASSIGNMENT_CREATED -> handleAssignmentCreated(event);
            case ASSIGNMENT_REJECTED -> handleAssignmentRejected(event);
        }
    }

    private void handleCreated(TicketEvent event) {
        // Broadcast new ticket to all subscribers
        sendToTopic("/topic/ticket/new", event.payload());
        log.info("Broadcasted new ticket: id={}", event.ticketId());
    }

    private void handleUpdate(TicketEvent event) {
        // Broadcast update to ticket subscribers
        String destination = "/topic/ticket/" + event.ticketId();
        sendToTopic(destination, event.payload());
        log.info("Broadcasted ticket update: id={}, type={}", event.ticketId(), event.type());
    }

    private void handleMessage(TicketEvent event) {
        // Broadcast message to ticket chat subscribers
        String destination = "/topic/ticket/" + event.ticketId() + "/messages";
        sendToTopic(destination, event.payload());
        log.info("Broadcasted message to ticket: id={}", event.ticketId());
    }

    private void handleDeleted(TicketEvent event) {
        // Notify about ticket deletion
        String destination = "/topic/ticket/" + event.ticketId() + "/deleted";
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", event.ticketId());
        payload.put("deleted", true);
        sendToTopic(destination, payload);
        log.info("Broadcasted ticket deletion: id={}", event.ticketId());
    }

    private void handleAttachment(TicketEvent event) {
        // Broadcast attachment added to ticket subscribers
        String destination = "/topic/ticket/" + event.ticketId() + "/attachments";
        sendToTopic(destination, event.payload());
        log.info("Broadcasted attachment added: ticketId={}", event.ticketId());
    }

    private void handleInternalComment(TicketEvent event) {
        // Broadcast internal comment (only to specialists)
        String destination = "/topic/ticket/" + event.ticketId() + "/internal";
        sendToTopic(destination, event.payload());
        log.info("Broadcasted internal comment: ticketId={}", event.ticketId());
    }

    private void handleSlaBreach(TicketEvent event) {
        // Broadcast SLA breach notification
        String destination = "/topic/sla/breach";
        sendToTopic(destination, event.payload());
        log.warn("Broadcasted SLA breach: ticketId={}", event.ticketId());
    }

    private void handleUserStatusChanged(TicketEvent event) {
        // Broadcast user status change to support line team
        // ticketId is used as lineId in this case, userId is the user who changed
        // status
        String destination = "/topic/line/" + event.ticketId() + "/status";
        sendToTopic(destination, event.payload());
        log.info("Broadcasted user status change: userId={}, lineId={}", event.userId(), event.ticketId());
    }

    private void handleAssignmentCreated(TicketEvent event) {
        // Broadcast assignment created to user
        String destination = "/queue/assignments";
        messagingTemplate.convertAndSendToUser(
                event.userId().toString(),
                destination,
                event.payload());
        log.info("Sent assignment notification to user: userId={}, ticketId={}",
                event.userId(), event.ticketId());
    }

    private void handleAssignmentRejected(TicketEvent event) {
        // Уведомление отправителю назначения об отклонении
        String destination = "/queue/assignments/rejected";
        messagingTemplate.convertAndSendToUser(
                event.userId().toString(),
                destination,
                event.payload());
        log.info("Sent assignment rejection to user: userId={}, ticketId={}",
                event.userId(), event.ticketId());
    }

    /**
     * Helper to avoid ambiguous method call in SimpMessagingTemplate
     */
    private void sendToTopic(String destination, Object payload) {
        messagingTemplate.convertAndSend(destination, payload, Map.of());
    }
}
