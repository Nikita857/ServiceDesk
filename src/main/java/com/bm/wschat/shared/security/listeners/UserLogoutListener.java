package com.bm.wschat.shared.security.listeners;

import com.bm.wschat.feature.user.service.UserActivityLogService;
import com.bm.wschat.feature.user.service.UserActivityStatusService;
import com.bm.wschat.shared.security.events.UserLogoutEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserLogoutListener {

    private final UserActivityStatusService userActivityStatusService;
    private final UserActivityLogService userActivityLogService;

    @EventListener
    public void onLogout(UserLogoutEvent event) {
        userActivityStatusService.onLogout(event.user());
        userActivityLogService.onLogout(event.user());
    }
}
