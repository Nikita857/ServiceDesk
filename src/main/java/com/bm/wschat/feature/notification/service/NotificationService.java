package com.bm.wschat.feature.notification.service;

import com.bm.wschat.feature.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * Сервис уведомлений через WebSocket
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Отправить уведомление пользователю через WebSocket
     */
    public void notifyUser(Long userId, Notification notification) {
        if (userId == null) {
            log.warn("Cannot send notification: userId is null");
            return;
        }

        String destination = "/topic/user/" + userId + "/notifications";
        messagingTemplate.convertAndSend(destination, notification);

        log.debug("Notification sent to user {}: type={}, ticketId={}",
                userId, notification.type(), notification.ticketId());
    }

    /**
     * Отправить уведомление нескольким пользователям
     */
    public void notifyUsers(Collection<Long> userIds, Notification notification) {
        for (Long userId : userIds) {
            notifyUser(userId, notification);
        }
    }

    /**
     * Отправить уведомление всем, кроме указанного пользователя
     */
    public void notifyUsersExcept(Collection<Long> userIds, Long excludeUserId, Notification notification) {
        for (Long userId : userIds) {
            if (!userId.equals(excludeUserId)) {
                notifyUser(userId, notification);
            }
        }
    }
}
