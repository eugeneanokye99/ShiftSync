package com.shiftsync.shiftsync.notification.service;

import com.shiftsync.shiftsync.common.enums.NotificationType;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.notification.dto.NotificationResponse;
import com.shiftsync.shiftsync.notification.dto.UnreadCountResponse;
import com.shiftsync.shiftsync.notification.entity.Notification;
import com.shiftsync.shiftsync.notification.repository.NotificationRepository;
import com.shiftsync.shiftsync.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Test
    void notifyUser_SavesNotificationWithCorrectFields() {
        notificationService.notifyUser(1L, NotificationType.SHIFT_ASSIGNED, "You have been assigned", "SHIFT", 10L);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getType()).isEqualTo(NotificationType.SHIFT_ASSIGNED);
        assertThat(saved.getMessage()).isEqualTo("You have been assigned");
        assertThat(saved.getEntityType()).isEqualTo("SHIFT");
        assertThat(saved.getEntityId()).isEqualTo(10L);
    }

    @Test
    void getInbox_UnreadOnlyFalse_ReturnsAllNotifications() {
        Notification n = buildNotification(1L, 100L, false);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(100L), any()))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationResponse> result = notificationService.getInbox(100L, false, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(1L);
        assertThat(result.getContent().getFirst().read()).isFalse();
    }

    @Test
    void getInbox_UnreadOnlyTrue_QueriesUnreadRepository() {
        Notification n = buildNotification(2L, 100L, false);
        when(notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(eq(100L), any()))
                .thenReturn(new PageImpl<>(List.of(n)));

        Page<NotificationResponse> result = notificationService.getInbox(100L, true, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().id()).isEqualTo(2L);
    }

    @Test
    void markAsRead_UnreadNotification_SetsReadAndReadAt() {
        Notification n = buildNotification(5L, 100L, false);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        notificationService.markAsRead(100L, 5L);

        assertThat(n.isRead()).isTrue();
        assertThat(n.getReadAt()).isNotNull();
    }

    @Test
    void markAsRead_AlreadyRead_DoesNotUpdateReadAt() {
        LocalDateTime existingReadAt = LocalDateTime.of(2026, 1, 1, 12, 0);
        Notification n = buildNotification(5L, 100L, true);
        n.setReadAt(existingReadAt);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        notificationService.markAsRead(100L, 5L);

        assertThat(n.getReadAt()).isEqualTo(existingReadAt);
    }

    @Test
    void markAsRead_NotFound_ThrowsResourceNotFound() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(100L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Notification not found");
    }

    @Test
    void markAsRead_NotOwner_ThrowsBadRequest() {
        Notification n = buildNotification(5L, 200L, false);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.markAsRead(100L, 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Notification does not belong to the current user");
    }

    @Test
    void markAllAsRead_CallsRepositoryWithCorrectUserId() {
        notificationService.markAllAsRead(100L);

        verify(notificationRepository).markAllAsRead(eq(100L), any(LocalDateTime.class));
    }

    @Test
    void getUnreadCount_ReturnsCountFromRepository() {
        when(notificationRepository.countByUserIdAndReadFalse(100L)).thenReturn(3L);

        UnreadCountResponse result = notificationService.getUnreadCount(100L);

        assertThat(result.count()).isEqualTo(3L);
    }

    private Notification buildNotification(Long id, Long userId, boolean read) {
        Notification n = Notification.builder()
                .id(id)
                .userId(userId)
                .type(NotificationType.SHIFT_ASSIGNED)
                .message("Test notification")
                .entityType("SHIFT")
                .entityId(10L)
                .build();
        n.setRead(read);
        n.setReadAt(read ? LocalDateTime.now() : null);
        n.setCreatedAt(LocalDateTime.now());
        return n;
    }
}
