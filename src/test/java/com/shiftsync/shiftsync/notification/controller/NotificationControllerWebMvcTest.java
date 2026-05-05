package com.shiftsync.shiftsync.notification.controller;

import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import com.shiftsync.shiftsync.notification.dto.NotificationResponse;
import com.shiftsync.shiftsync.notification.dto.UnreadCountResponse;
import com.shiftsync.shiftsync.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthenticationHelper.class})
class NotificationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getInbox_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1")
    void getInbox_Authenticated_ReturnsOk() throws Exception {
        when(notificationService.getInbox(anyLong(), anyBoolean(), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "1")
    void getInbox_UnreadOnly_ReturnsOk() throws Exception {
        NotificationResponse response = new NotificationResponse(
                1L, com.shiftsync.shiftsync.common.enums.NotificationType.SHIFT_ASSIGNED,
                "You have been assigned a shift", "SHIFT", 10L,
                false, null, LocalDateTime.now()
        );
        when(notificationService.getInbox(anyLong(), eq(true), anyInt(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/notifications").param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void getUnreadCount_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1")
    void getUnreadCount_Authenticated_ReturnsCount() throws Exception {
        when(notificationService.getUnreadCount(anyLong()))
                .thenReturn(new UnreadCountResponse(5L));

        mockMvc.perform(get("/api/v1/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void markAsRead_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/5/read"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1")
    void markAsRead_ValidRequest_ReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/5/read"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "1")
    void markAsRead_NotFound_ReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Notification not found"))
                .when(notificationService).markAsRead(anyLong(), eq(99L));

        mockMvc.perform(patch("/api/v1/notifications/99/read"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "1")
    void markAsRead_NotOwner_ReturnsBadRequest() throws Exception {
        doThrow(new BadRequestException("Notification does not belong to the current user"))
                .when(notificationService).markAsRead(anyLong(), eq(5L));

        mockMvc.perform(patch("/api/v1/notifications/5/read"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Notification does not belong to the current user"));
    }

    @Test
    void markAllAsRead_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1")
    void markAllAsRead_Authenticated_ReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/notifications/read-all"))
                .andExpect(status().isNoContent());
    }
}
