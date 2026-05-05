package com.shiftsync.shiftsync.notification.service;

import com.shiftsync.shiftsync.common.enums.NotificationType;
import com.shiftsync.shiftsync.notification.dto.NotificationResponse;
import com.shiftsync.shiftsync.notification.dto.UnreadCountResponse;
import org.springframework.data.domain.Page;

public interface NotificationService {

    void notifyUser(Long userId, NotificationType type, String message, String entityType, Long entityId);

    Page<NotificationResponse> getInbox(Long actorUserId, boolean unreadOnly, int page, int size);

    void markAsRead(Long actorUserId, Long notificationId);

    void markAllAsRead(Long actorUserId);

    UnreadCountResponse getUnreadCount(Long actorUserId);
}