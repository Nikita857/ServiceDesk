package com.bm.wschat.shared.security.listeners;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.feature.user.service.UserActivityLogService;
import com.bm.wschat.feature.user.service.UserActivityStatusService;
import com.bm.wschat.shared.security.events.UserLogoutEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserLogoutListener {

    private final UserActivityStatusService userActivityStatusService;
    private final UserActivityLogService userActivityLogService;
    private final UserRepository userRepository;

    @EventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onLogout(UserLogoutEvent event) {

        User user = userRepository.findById(event.userId()).orElseThrow(
                () -> new EntityNotFoundException("Пользователь не найден: " + event.userId())
        );

        userActivityStatusService.onLogout(user);
        userActivityLogService.onLogout(user);
    }
}
