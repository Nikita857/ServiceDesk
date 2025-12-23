package com.bm.wschat.shared.messaging.template;

import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.user.model.User;
import org.springframework.stereotype.Component;

@Component
public class TelegramMessageTemplate {

    public String buildCreatedMessage(Ticket ticket) {
        return String.format("""
                🆕 *Новая заявка #%d*
                
                *Тема:* %s
                *Линия:* %s
                *Автор:* %s
                *Приоритет:* %s
                
                📝 %s
                """,
                ticket.getId(),
                ticket.getTitle(),
                ticket.getSupportLine() != null ? ticket.getSupportLine().getName() : "Не назначена",
                getUserName(ticket.getCreatedBy()),
                ticket.getPriority(),
                truncate(ticket.getDescription(), 200)
        );
    }

    public String buildClosedMessage(Ticket ticket) {
        return String.format("""
                ✅ *Заявка #%d закрыта*
                
                *Тема:* %s
                *Специалист:* %s
                """,
                ticket.getId(),
                ticket.getTitle(),
                ticket.getAssignedTo() != null ? getUserName(ticket.getAssignedTo()) : "Не назначен"
        );
    }

    public String buildRatedMessage(Ticket ticket) {
        String starRating = "⭐".repeat(ticket.getRating() != null ? ticket.getRating() : 0);
        
        return String.format("""
                ⭐️ *Оценка заявки #%d*
                
                *Оценка:* %s (%d/5)
                *Отзыв:* %s
                *Специалист:* %s
                """,
                ticket.getId(),
                starRating,
                ticket.getRating() != null ? ticket.getRating() : 0,
                ticket.getFeedback() != null && !ticket.getFeedback().isBlank() ? ticket.getFeedback() : "Без отзыва",
                ticket.getAssignedTo() != null ? getUserName(ticket.getAssignedTo()) : "Не назначен"
        );
    }

    public String buildAssignedMessage(Ticket ticket) {
        return String.format("""
                🎫 *Вам назначена заявка #%d*
                
                *Тема:* %s
                *Приоритет:* %s
                """,
                ticket.getId(),
                ticket.getTitle(),
                ticket.getPriority()
        );
    }
    
    public String buildTakenInWorkMessage(Ticket ticket) {
        return String.format("""
                👷 *Заявка #%d взята в работу*
                
                *Специалист:* %s
                """,
                ticket.getId(),
                getUserName(ticket.getAssignedTo())
        );
    }
    
    public String buildStatusChangedMessage(Ticket ticket) {
        return String.format("🔄 *Статус заявки #%d изменен на %s*", ticket.getId(), ticket.getStatus());
    }

    private String getUserName(User user) {
        if (user == null) return "Неизвестно";
        return user.getFio() != null ? user.getFio() : user.getUsername();
    }

    private String truncate(String text, int length) {
        if (text == null) return "";
        if (text.length() <= length) return text;
        return text.substring(0, length) + "...";
    }
}
