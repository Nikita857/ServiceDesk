package com.bm.wschat.shared.messaging.template;

import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.ticket.model.TicketPriority;
import com.bm.wschat.feature.user.model.User;
import org.springframework.stereotype.Component;

/**
 * Шаблоны сообщений для Telegram уведомлений.
 * Использует Markdown для форматирования.
 */
@Component
public class TelegramMessageTemplate {

    private static final String PRIORITY_HIGH = "🔴";
    private static final String PRIORITY_MEDIUM = "🟡";
    private static final String PRIORITY_LOW = "🟢";

    /**
     * Новая заявка создана — для канала линии поддержки
     */
    public String buildCreatedMessage(Ticket ticket) {
        return String.format("""
                🆕 *Новая заявка #%d*

                📋 *Тема:* %s
                📍 *Линия:* %s
                👤 *Автор:* %s
                %s *Приоритет:* %s

                📝 _%s_
                """,
                ticket.getId(),
                escapeMarkdown(ticket.getTitle()),
                ticket.getSupportLine() != null ? ticket.getSupportLine().getName() : "Не назначена",
                getUserName(ticket.getCreatedBy()),
                getPriorityEmoji(ticket.getPriority()),
                ticket.getPriority(),
                truncate(escapeMarkdown(ticket.getDescription()), 200));
    }

    /**
     * Заявка закрыта/решена — для автора
     */
    public String buildClosedMessage(Ticket ticket) {
        return String.format("""
                ✅ *Заявка #%d закрыта*

                📋 *Тема:* %s
                👨‍💻 *Специалист:* %s

                _Спасибо за обращение! Пожалуйста, оцените работу специалиста._
                """,
                ticket.getId(),
                escapeMarkdown(ticket.getTitle()),
                ticket.getAssignedTo() != null ? getUserName(ticket.getAssignedTo()) : "Не назначен");
    }

    /**
     * Оценка получена — для специалиста
     */
    public String buildRatedMessage(Ticket ticket) {
        int rating = ticket.getRating() != null ? ticket.getRating() : 0;
        String stars = "⭐".repeat(rating) + "☆".repeat(5 - rating);

        return String.format("""
                ⭐ *Получена оценка по заявке #%d*

                %s (%d/5)
                💬 *Отзыв:* %s
                """,
                ticket.getId(),
                stars,
                rating,
                ticket.getFeedback() != null && !ticket.getFeedback().isBlank()
                        ? escapeMarkdown(ticket.getFeedback())
                        : "_Без комментария_");
    }

    /**
     * Назначена заявка — для специалиста
     */
    public String buildAssignedMessage(Ticket ticket) {
        return String.format("""
                🎫 *Вам назначена заявка #%d*

                📋 *Тема:* %s
                👤 *Автор:* %s
                %s *Приоритет:* %s
                """,
                ticket.getId(),
                escapeMarkdown(ticket.getTitle()),
                getUserName(ticket.getCreatedBy()),
                getPriorityEmoji(ticket.getPriority()),
                ticket.getPriority());
    }

    /**
     * Назначение на специалиста в чат поддержки
     */
    public String buildAssignmentMessageInSupportLineChat(Ticket ticket) {
        return String.format("""
                🎫 *Заявка #%d взята в работу*

                📋 *Тема:* %s
                👤 *Автор:* %s
                👤 *Исполнитель:* %s
                %s *Приоритет:* %s
                """,
                ticket.getId(),
                escapeMarkdown(ticket.getTitle()),
                getUserName(ticket.getCreatedBy()),
                getUserName(ticket.getAssignedTo()),
                getPriorityEmoji(ticket.getPriority()),
                ticket.getPriority());
    }

    /**
     * Заявка взята в работу — для автора
     */
    public String buildTakenInWorkMessage(Ticket ticket) {
        return String.format("""
                👷 *Заявка #%d взята в работу*

                👨‍💻 *Специалист:* %s

                _Ожидайте ответа, специалист уже работает над вашей заявкой._
                """,
                ticket.getId(),
                getUserName(ticket.getAssignedTo()));
    }

    /**
     * Статус изменился — для автора
     */
    public String buildStatusChangedMessage(Ticket ticket) {
        String statusEmoji = switch (ticket.getStatus()) {
            case NEW -> "🆕";
            case OPEN, REOPENED -> "📂";
            case PENDING, PENDING_CLOSURE -> "⏳";
            case ESCALATED -> "⬆️";
            case RESOLVED -> "✅";
            case CLOSED -> "🔒";
            case REJECTED, CANCELLED -> "❌";
        };

        return String.format("""
                %s *Статус заявки #%d изменён*

                Новый статус: *%s*
                """,
                statusEmoji,
                ticket.getId(),
                ticket.getStatus());
    }

    /**
     * Новое сообщение от поддержки — для автора
     */
    public String buildNewMessageFromSupport(Ticket ticket) {
        return String.format("""
                💬 *Новое сообщение от поддержки*

                📋 Заявка #%d: %s
                👨‍💻 Специалист: %s
                """,
                ticket.getId(),
                escapeMarkdown(ticket.getTitle()),
                getUserName(ticket.getAssignedTo()));
    }

    /**
     * Новое сообщение от пользователя — для специалиста
     */
    public String buildNewMessageFromUser(Ticket ticket) {
        return String.format("""
                💬 *Новое сообщение от пользователя*

                📋 Заявка #%d: %s
                👤 Автор: %s
                """,
                ticket.getId(),
                escapeMarkdown(ticket.getTitle()),
                getUserName(ticket.getCreatedBy()));
    }

    /**
     * Новое сообщение (общее) — когда пишет третье лицо
     */
    public String buildNewMessageGeneric(Ticket ticket) {
        return String.format("""
                💬 *Новое сообщение в заявке*

                📋 Заявка #%d: %s
                """,
                ticket.getId(),
                escapeMarkdown(ticket.getTitle()));
    }

    // === Helper methods ===

    private String getUserName(User user) {
        if (user == null)
            return "Неизвестно";
        return user.getFio() != null && !user.getFio().isBlank()
                ? user.getFio()
                : user.getUsername();
    }

    private String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        if (text.length() <= maxLength)
            return text;
        return text.substring(0, maxLength) + "...";
    }

    private String getPriorityEmoji(TicketPriority priority) {
        if (priority == null)
            return PRIORITY_MEDIUM;
        return switch (priority) {
            case HIGH, URGENT -> PRIORITY_HIGH;
            case MEDIUM -> PRIORITY_MEDIUM;
            case LOW -> PRIORITY_LOW;
        };
    }

    /**
     * Экранирует специальные символы Markdown чтобы избежать ошибок форматирования.
     */
    private String escapeMarkdown(String text) {
        if (text == null)
            return "";
        return text
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("`", "\\`");
    }
}
