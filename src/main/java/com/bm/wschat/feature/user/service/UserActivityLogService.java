package com.bm.wschat.feature.user.service;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityEventType;
import com.bm.wschat.feature.user.model.UserActivityLog;
import com.bm.wschat.feature.user.repository.UserActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActivityLogService {

    private final UserActivityLogRepository userActivityLogRepository;

    public void onLogin(User user) {
        if (user.getId() == null) {
            throw new IllegalStateException("User must have id before logging activity");
        }

        userActivityLogRepository.save(
                UserActivityLog.builder()
                        .user(user)
                        .eventType(UserActivityEventType.LOGIN)
                        .build()
        );
    }


    public void onLogout(User user) {
        userActivityLogRepository.save(UserActivityLog.builder()
                .eventType(UserActivityEventType.LOGOUT)
                .build()
        );
    }
}
