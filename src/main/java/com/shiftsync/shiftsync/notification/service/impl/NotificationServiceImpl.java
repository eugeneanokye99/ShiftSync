package com.shiftsync.shiftsync.notification.service.impl;

import com.shiftsync.shiftsync.common.enums.NotificationType;
import com.shiftsync.shiftsync.notification.entity.Notification;
import com.shiftsync.shiftsync.notification.repository.NotificationRepository;
import com.shiftsync.shiftsync.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void notifyUser(Long userId, NotificationType type, String message, String entityType, Long entityId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .type(type)
                .message(message)
                .entityType(entityType)
                .entityId(entityId)
                .build();
        notificationRepository.save(notification);
    }
}