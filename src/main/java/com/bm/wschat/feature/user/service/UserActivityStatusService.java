package com.bm.wschat.feature.user.service;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityStatus;
import com.bm.wschat.feature.user.model.UserActivityStatusEntity;
import com.bm.wschat.feature.user.repository.UserActivityStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserActivityStatusService {
    private final UserActivityStatusRepository userActivityStatusRepository;

    public void onLogin(User user) {
        upsert(user, UserActivityStatus.AVAILABLE);
    }

    public void onLogout(User user) {
        upsert(user, UserActivityStatus.OFFLINE);
    }

    private void upsert(User user, UserActivityStatus status) {
        // Найти или создать
        UserActivityStatusEntity entity = userActivityStatusRepository.findById(user.getId())
                .orElseGet(() -> UserActivityStatusEntity.builder()
                        .user(user)
                        .build());

        entity.setStatus(status);
        userActivityStatusRepository.save(entity);
    }
}
