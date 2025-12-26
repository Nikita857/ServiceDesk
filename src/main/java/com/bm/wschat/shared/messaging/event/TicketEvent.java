package com.bm.wschat.shared.messaging.event;

import com.bm.wschat.shared.messaging.TicketEventType;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO события тикета для RabbitMQ.
 * Содержит тип события, ID тикета и payload (JSON).
 */
public record TicketEvent(
        TicketEventType type,
        Long ticketId,
        Long userId,
        Object payload,
        Instant timestamp) implements Serializable {

    public static TicketEvent of(TicketEventType type, Long ticketId, Long userId, Object payload) {
        return new TicketEvent(type, ticketId, userId, payload, Instant.now());
    }

    public static TicketEvent created(Long ticketId, Long userId, Object payload) {
        return of(TicketEventType.CREATED, ticketId, userId, payload);
    }

    public static TicketEvent updated(Long ticketId, Long userId, Object payload) {
        return of(TicketEventType.UPDATED, ticketId, userId, payload);
    }

    public static TicketEvent statusChanged(Long ticketId, Long userId, Object payload) {
        return of(TicketEventType.STATUS_CHANGED, ticketId, userId, payload);
    }

    public static TicketEvent assigned(Long ticketId, Long userId, Object payload) {
        return of(TicketEventType.ASSIGNED, ticketId, userId, payload);
    }

    public static TicketEvent assignmentCreated(Long ticketId, Long userId, Object payload) {
        return of(TicketEventType.ASSIGNMENT_CREATED, ticketId, userId, payload);
    }

    public static TicketEvent assignmentRejected(Long ticketId, Long userId, Object payload) {
        return of(TicketEventType.ASSIGNMENT_REJECTED, ticketId, userId, payload);
    }

    public static TicketEvent messageSent(Long ticketId, Long userId, Object payload) {
        return of(TicketEventType.MESSAGE_SENT, ticketId, userId, payload);
    }

    public static TicketEvent rated(Long ticketId, Long userId, Object payload) {
        return of(TicketEventType.RATED, ticketId, userId, payload);
    }

    public static TicketEvent deleted(Long ticketId, Long userId) {
        return of(TicketEventType.DELETED, ticketId, userId, null);
    }

    public static TicketEvent internalComment(Long ticketId, Long userId, Object payload) {
        return of(TicketEventType.INTERNAL_COMMENT, ticketId, userId, payload);
    }

    public static TicketEvent messageUpdated(Long ticketId, Long userId, Object payload) {
        return of(TicketEventType.MESSAGE_UPDATED, ticketId, userId, payload);
    }
}
