package com.bm.wschat.feature.notification.model;

import java.time.Instant;

/**
 * DTO для уведомлений, отправляемых через WebSocket
 */
public record Notification(
        NotificationType type,
        Long ticketId,
        String ticketTitle,
        String title,
        String body,
        Long senderId,
        String senderName,
        Instant createdAt) {
    public static Notification message(Long ticketId, String ticketTitle, Long senderId, String senderName,
            String messagePreview) {
        return new Notification(
                NotificationType.MESSAGE,
                ticketId,
                ticketTitle,
                "Новое сообщение",
                senderName + ": " + truncate(messagePreview, 100),
                senderId,
                senderName,
                Instant.now());
    }

    public static Notification statusChange(Long ticketId, String ticketTitle, String oldStatus, String newStatus) {
        return new Notification(
                NotificationType.STATUS_CHANGE,
                ticketId,
                ticketTitle,
                "Статус изменён",
                "Тикет #" + ticketId + ": " + oldStatus + " → " + newStatus,
                null,
                null,
                Instant.now());
    }

    public static Notification assignment(Long ticketId, String ticketTitle, String assignerName) {
        return new Notification(
                NotificationType.ASSIGNMENT,
                ticketId,
                ticketTitle,
                "Назначен тикет",
                assignerName + " назначил вам тикет #" + ticketId,
                null,
                assignerName,
                Instant.now());
    }

    private static String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
