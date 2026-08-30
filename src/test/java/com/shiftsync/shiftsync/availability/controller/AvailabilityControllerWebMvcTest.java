package com.shiftsync.shiftsync.availability.controller;

import com.shiftsync.shiftsync.availability.dto.AvailabilityOverrideResponse;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityResponse;
import com.shiftsync.shiftsync.availability.service.AvailabilityService;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void createOverride_Success_ReturnsCreated() throws Exception {
        when(availabilityService.createOverride(anyLong(), any())).thenReturn(
                new AvailabilityOverrideResponse(10L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 12), "Travel")
        );

        String body = """
                {
                  "startDate": "2026-04-10",
                  "endDate": "2026-04-12",
                  "reason": "Travel"
                }
                """;

        mockMvc.perform(post("/api/v1/employees/me/availability/overrides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/v1/employees/me/availability/overrides/10")))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void createOverride_Overlap_ReturnsConflict() throws Exception {
        when(availabilityService.createOverride(anyLong(), any()))
                .thenThrow(new InvalidStateException("Overlapping override dates are not allowed"));

        String body = """
                {
                  "startDate": "2026-04-10",
                  "endDate": "2026-04-12",
                  "reason": "Travel"
                }
                """;

        mockMvc.perform(post("/api/v1/employees/me/availability/overrides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Overlapping override dates are not allowed"));
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void listOverrides_ReturnsOk() throws Exception {
        when(availabilityService.listActiveOverrides(anyLong())).thenReturn(List.of(
                new AvailabilityOverrideResponse(10L, LocalDate.of(2026, 4, 10), LocalDate.of(2026, 4, 12), "Travel")
        ));

        mockMvc.perform(get("/api/v1/employees/me/availability/overrides"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reason").value("Travel"));
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void deleteOverride_ReturnsNoContent() throws Exception {
        doNothing().when(availabilityService).deleteOverride(5L, 10L);

        mockMvc.perform(delete("/api/v1/employees/me/availability/overrides/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "5", roles = "MANAGER")
    void createOverride_WrongRole_ReturnsForbidden() throws Exception {
        String body = """
                {
                  "startDate": "2026-04-10",
                  "endDate": "2026-04-12"
                }
                """;

        mockMvc.perform(post("/api/v1/employees/me/availability/overrides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}

