package com.shiftsync.shiftsync.availability.controller;

import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityResponse;
import com.shiftsync.shiftsync.availability.service.AvailabilityService;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AvailabilityController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthenticationHelper.class})
class AvailabilityControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvailabilityService availabilityService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void replaceRecurringAvailability_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/employees/me/availability/recurring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void replaceRecurringAvailability_Success_ReturnsOk() throws Exception {
        when(availabilityService.replaceRecurringAvailability(anyLong(), any())).thenReturn(List.of(
                new RecurringAvailabilityResponse(1L, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0))
        ));

        String body = """
                [
                  {
                    "dayOfWeek": "MONDAY",
                    "startTime": "09:00:00",
                    "endTime": "13:00:00"
                  }
                ]
                """;

        mockMvc.perform(put("/api/v1/employees/me/availability/recurring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"));
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void replaceRecurringAvailability_Overlap_ReturnsBadRequest() throws Exception {
        when(availabilityService.replaceRecurringAvailability(anyLong(), any()))
                .thenThrow(new BadRequestException("Overlapping recurring availability windows are not allowed for MONDAY"));

        String body = """
                [
                  {
                    "dayOfWeek": "MONDAY",
                    "startTime": "09:00:00",
                    "endTime": "13:00:00"
                  },
                  {
                    "dayOfWeek": "MONDAY",
                    "startTime": "12:00:00",
                    "endTime": "16:00:00"
                  }
                ]
                """;

        mockMvc.perform(put("/api/v1/employees/me/availability/recurring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Overlapping recurring availability windows are not allowed for MONDAY"));
    }

    @Test
    @WithMockUser(username = "5", roles = "MANAGER")
    void replaceRecurringAvailability_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(put("/api/v1/employees/me/availability/recurring")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden());
    }
}

