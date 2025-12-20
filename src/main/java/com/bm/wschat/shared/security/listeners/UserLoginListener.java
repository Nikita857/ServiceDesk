package com.bm.wschat.shared.security.listeners;

import com.bm.wschat.feature.user.service.UserActivityLogService;
import com.bm.wschat.feature.user.service.UserActivityStatusService;
import com.bm.wschat.shared.security.events.UserLoginEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserLoginListener {

    private final UserActivityStatusService userActivityStatusService;
    private final UserActivityLogService userActivityLogService;

    @EventListener
    public void handleUserLoginEvent(UserLoginEvent event) {
        userActivityStatusService.onLogin(event.user());
        userActivityLogService.onLogin(event.user());
    }
}
