package com.shiftsync.shiftsync.notification.service.impl;

import com.shiftsync.shiftsync.common.enums.NotificationType;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.notification.dto.NotificationResponse;
import com.shiftsync.shiftsync.notification.dto.UnreadCountResponse;
import com.shiftsync.shiftsync.notification.entity.Notification;
import com.shiftsync.shiftsync.notification.entity.NotificationEvent;
import com.shiftsync.shiftsync.notification.repository.NotificationRepository;
import com.shiftsync.shiftsync.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void notifyUser(Long userId, NotificationType type, String message, String entityType, Long entityId) {
        eventPublisher.publishEvent(new NotificationEvent(userId, type, message, entityType, entityId));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("notificationExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onNotificationEvent(NotificationEvent event) {
        Notification notification = Notification.builder()
                .userId(event.userId())
                .type(event.type())
                .message(event.message())
                .entityType(event.entityType())
                .entityId(event.entityId())
                .build();
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getInbox(Long actorUserId, Boolean read, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Notification> results = Boolean.FALSE.equals(read)
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
            throw new AccessDeniedException("Notification does not belong to the current user");
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
