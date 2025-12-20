package com.bm.wschat.feature.user.service;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityEventType;
import com.bm.wschat.feature.user.model.UserActivityLog;
import com.bm.wschat.feature.user.repository.UserActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис для аудита событий активности пользователей.
 * Записывает логин, логаут и смену статуса.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityLogService {

    private final UserActivityLogRepository userActivityLogRepository;

    /**
     * Записать событие входа в систему
     */
    @Transactional
    public void onLogin(User user) {
        logEvent(user, UserActivityEventType.LOGIN);
        log.debug("Записан вход: userId={}, username={}", user.getId(), user.getUsername());
    }

    /**
     * Записать событие выхода из системы
     */
    @Transactional
    public void onLogout(User user) {
        logEvent(user, UserActivityEventType.LOGOUT);
        log.debug("Записан выход: userId={}, username={}", user.getId(), user.getUsername());
    }

    /**
     * Записать событие смены статуса активности
     */
    @Transactional
    public void logStatusChange(User user) {
        logEvent(user, UserActivityEventType.STATUS_CHANGED);
        log.debug("Записана смена статуса: userId={}, username={}", user.getId(), user.getUsername());
    }

    private void logEvent(User user, UserActivityEventType eventType) {
        UserActivityLog activityLog = UserActivityLog.builder()
                .user(user)
                .eventType(eventType)
                .build();

        userActivityLogRepository.save(activityLog);
    }
}
