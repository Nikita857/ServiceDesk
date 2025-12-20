package com.bm.wschat.feature.user.service;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityEventType;
import com.bm.wschat.feature.user.model.UserActivityLog;
import com.bm.wschat.feature.user.repository.UserActivityLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityLogService {

    private final UserActivityLogRepository userActivityLogRepository;

    public void onLogin(User user) {
        logEvent(user, UserActivityEventType.LOGIN);
    }

    public void onLogout(User user) {
        logEvent(user, UserActivityEventType.LOGOUT);
    }

    private void logEvent(User user, UserActivityEventType eventType) {
        UserActivityLog log = UserActivityLog.builder()
                .user(user)                  // передаём объект — Hibernate подставит user_id сам
                .eventType(eventType)
                .build();

        userActivityLogRepository.save(log);
    }
}
