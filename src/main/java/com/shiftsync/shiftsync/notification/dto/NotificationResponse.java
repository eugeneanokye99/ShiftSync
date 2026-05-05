package com.shiftsync.shiftsync.notification.dto;

import com.shiftsync.shiftsync.common.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String message,
        String entityType,
        Long entityId,
        boolean read,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {}
