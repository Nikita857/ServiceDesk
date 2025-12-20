package com.bm.wschat.feature.user.service;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.model.UserActivityStatus;
import com.bm.wschat.feature.user.model.UserActivityStatusEntity;
import com.bm.wschat.feature.user.repository.UserActivityStatusRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Transactional
public class UserActivityStatusService {

    private final UserActivityStatusRepository repository;

    public void onLogin(User user) {
        updateStatus(user, UserActivityStatus.AVAILABLE);
    }

    public void onLogout(User user) {
        updateStatus(user, UserActivityStatus.OFFLINE);
    }

    public void setStatus(User user, UserActivityStatus status) throws HttpMessageNotReadableException {
        UserActivityStatusEntity userActivityStatus = repository.findByUser(user).orElseThrow(
                () -> new EntityNotFoundException("Запись статуса не найдена")
        );
        if(userActivityStatus.getStatus() == status) {
            throw new IllegalArgumentException("Нельзя менять стаутус на тот же самый");
        }
        updateStatus(user, status);
    }

    private void updateStatus(User user, UserActivityStatus newStatus) {
        UserActivityStatusEntity status = repository.findById(user.getId())
                .orElseGet(() -> UserActivityStatusEntity.builder()
                        .user(user)  // только user — userId скопируется через @MapsId
                        .status(newStatus)
                        .build());

        status.setStatus(newStatus);
        repository.save(status);  // INSERT или UPDATE
    }
}
