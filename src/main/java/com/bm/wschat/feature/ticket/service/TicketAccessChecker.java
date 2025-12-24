package com.bm.wschat.feature.ticket.service;

import com.bm.wschat.feature.supportline.model.SupportLine;
import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.ticket.model.Ticket;
import com.bm.wschat.feature.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Компонент для проверки прав доступа к тикетам.
 * 
 * Правила доступа:
 * - ADMIN: полный доступ ко всем тикетам
 * - CREATOR: доступ к своим созданным тикетам
 * - ASSIGNED SPECIALIST: доступ к назначенным тикетам
 * - SPECIALIST в той же линии: доступ к неназначенным тикетам линии
 */
@Component
@RequiredArgsConstructor
public class TicketAccessChecker {

    private final SupportLineRepository supportLineRepository;

    /**
     * Проверить может ли пользователь получить доступ к тикету.
     */
    public boolean canAccess(Ticket ticket, User user) {
        // Admin может всё
        if (user.isAdmin()) {
            return true;
        }

        // Создатель всегда имеет доступ к своему тикету
        if (ticket.getCreatedBy() != null && ticket.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        // Назначенный специалист имеет доступ
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(user.getId())) {
            return true;
        }

        // Специалист из той же линии может видеть неназначенные тикеты
        if (user.isSpecialist() && ticket.getSupportLine() != null) {
            List<SupportLine> userLines = supportLineRepository.findBySpecialist(user);
            boolean inSameLine = userLines.stream()
                    .anyMatch(line -> line.getId().equals(ticket.getSupportLine().getId()));

            // Доступ если в той же линии И тикет не назначен другому
            return inSameLine && ticket.getAssignedTo() == null;
        }

        return false;
    }

    /**
     * Проверить может ли пользователь редактировать тикет.
     * Более строгие правила чем для чтения.
     */
    public boolean canEdit(Ticket ticket, User user) {
        if (user.isAdmin()) {
            return true;
        }

        // Назначенный специалист может редактировать
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(user.getId())) {
            return true;
        }

        return false;
    }

    /**
     * Проверить может ли пользователь менять статус тикета.
     */
    public boolean canChangeStatus(Ticket ticket, User user) {
        if (user.isAdmin()) {
            return true;
        }

        // Назначенный специалист
        if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(user.getId())) {
            return true;
        }

        // Создатель может закрыть/переоткрыть свой тикет
        if (ticket.getCreatedBy() != null && ticket.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }

        return false;
    }
}
