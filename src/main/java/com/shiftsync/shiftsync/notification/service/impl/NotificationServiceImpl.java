package com.shiftsync.shiftsync.notification.service.impl;

import com.shiftsync.shiftsync.common.enums.NotificationType;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.notification.dto.NotificationResponse;
import com.shiftsync.shiftsync.notification.dto.UnreadCountResponse;
import com.shiftsync.shiftsync.notification.entity.Notification;
import com.shiftsync.shiftsync.notification.repository.NotificationRepository;
import com.shiftsync.shiftsync.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Async("notificationExecutor")
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

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getInbox(Long actorUserId, boolean unreadOnly, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Notification> results = unreadOnly
                ? notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(actorUserId, pageable)
                : notificationRepository.findByUserIdOrderByCreatedAtDesc(actorUserId, pageable);
        return results.map(this::toResponse);
    }

    @Override
    @Transactional
    public void markAsRead(Long actorUserId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(actorUserId)) {
            throw new BadRequestException("Notification does not belong to the current user");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(Long actorUserId) {
        notificationRepository.markAllAsRead(actorUserId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long actorUserId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadFalse(actorUserId));
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getMessage(),
                n.getEntityType(),
                n.getEntityId(),
                n.isRead(),
                n.getReadAt(),
                n.getCreatedAt()
        );
    }
}