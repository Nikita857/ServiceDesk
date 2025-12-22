package com.bm.wschat.feature.user.service;

import com.bm.wschat.feature.supportline.repository.SupportLineRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityStatus;
import com.bm.wschat.feature.user.model.UserActivityStatusEntity;
import com.bm.wschat.feature.user.repository.UserActivityStatusRepository;
import com.bm.wschat.shared.messaging.TicketEvent;
import com.bm.wschat.shared.messaging.TicketEventPublisher;
import com.bm.wschat.shared.messaging.TicketEventType;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис управления статусом активности специалистов.
 * <p>
 * Статусы влияют на возможность назначения тикетов:
 * <ul>
 * <li>AVAILABLE, BUSY - можно назначать тикеты</li>
 * <li>UNAVAILABLE, TECHNICAL_ISSUE, OFFLINE - нельзя назначать</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserActivityStatusService {

    private final UserActivityStatusRepository statusRepository;
    private final UserActivityLogService activityLogService;
    private final TicketEventPublisher ticketEventPublisher;
    private final SupportLineRepository supportLineRepository;

    /**
     * Получить текущий статус пользователя.
     * Если запись не найдена, возвращает OFFLINE.
     */
    public UserActivityStatus getStatus(Long userId) {
        return statusRepository.findByUserId(userId)
                .map(UserActivityStatusEntity::getStatus)
                .orElse(UserActivityStatus.OFFLINE);
    }

    /**
     * Получить сущность статуса пользователя (с временем обновления).
     */
    public UserActivityStatusEntity getStatusEntity(Long userId) {
        return statusRepository.findByUserId(userId)
                .orElse(null);
    }

    /**
     * Проверить, может ли специалист получать тикеты.
     * 
     * @param userId ID пользователя
     * @return true если AVAILABLE или BUSY
     */
    public boolean isAvailableForAssignment(Long userId) {
        UserActivityStatus status = getStatus(userId);
        return status.isAvailableForAssignment();
    }

    /**
     * Обработка входа пользователя.
     * Устанавливает статус AVAILABLE.
     */
    @Transactional
    public void onLogin(User user) {
        updateStatus(user, UserActivityStatus.AVAILABLE, false);
        log.debug("Статус при входе: userId={}, status=AVAILABLE", user.getId());
    }

    /**
     * Обработка выхода пользователя.
     * Устанавливает статус OFFLINE.
     */
    @Transactional
    public void onLogout(User user) {
        updateStatus(user, UserActivityStatus.OFFLINE, false);
        log.debug("Статус при выходе: userId={}, status=OFFLINE", user.getId());
    }

    /**
     * Установить статус вручную (только для специалистов).
     * 
     * @param user      текущий пользователь
     * @param newStatus новый статус
     * @return установленный статус
     * @throws AccessDeniedException если пользователь не специалист
     * @throws IllegalStateException если статус уже установлен
     */
    @Transactional
    public UserActivityStatus setStatus(User user, UserActivityStatus newStatus) {
        // Валидация: только специалисты могут менять статус
        if (!user.isSpecialist()) {
            throw new AccessDeniedException("Только специалисты могут управлять статусом активности");
        }

        // Получаем текущий статус
        UserActivityStatusEntity entity = statusRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Запись статуса не найдена для пользователя: " + user.getId()));

        // Проверка на повторную установку того же статуса
        if (entity.getStatus() == newStatus) {
            throw new IllegalStateException("Статус уже установлен в: " + newStatus);
        }

        UserActivityStatus oldStatus = entity.getStatus();
        updateStatus(user, newStatus, true);

        log.info("Статус изменён: userId={}, username={}, {} -> {}",
                user.getId(), user.getUsername(), oldStatus, newStatus);

        // Публикуем изменение статуса для каждой линии поддержки, к которой принадлежит
        // специалист
        var payload = java.util.Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "fio", user.getFio() != null ? user.getFio() : user.getUsername(),
                "status", newStatus.name(),
                "oldStatus", oldStatus.name());

        for (var line : supportLineRepository.findBySpecialist(user)) {
            ticketEventPublisher.publish(TicketEvent.of(
                    TicketEventType.USER_STATUS_CHANGED,
                    line.getId(), user.getId(), payload));
        }

        return newStatus;
    }

    /**
     * Внутренний метод обновления статуса.
     * 
     * @param user      пользователь
     * @param newStatus новый статус
     * @param logChange записывать ли в аудит
     */
    private void updateStatus(User user, UserActivityStatus newStatus, boolean logChange) {
        UserActivityStatusEntity entity = statusRepository.findById(user.getId())
                .orElseGet(() -> UserActivityStatusEntity.builder()
                        .user(user)
                        .status(newStatus)
                        .build());

        entity.setStatus(newStatus);
        statusRepository.save(entity);

        if (logChange) {
            activityLogService.logStatusChange(user);
        }
    }
}
