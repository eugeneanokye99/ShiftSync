package com.shiftsync.shiftsync.notification.entity;

import com.shiftsync.shiftsync.common.enums.NotificationType;

public record NotificationEvent(
        Long userId,
        NotificationType type,
        String message,
        String entityType,
        Long entityId
) {}
