package com.shiftsync.shiftsync.notification.service;

import com.shiftsync.shiftsync.common.enums.NotificationType;

public interface NotificationService {

    void notifyUser(Long userId, NotificationType type, String message, String entityType, Long entityId);
}