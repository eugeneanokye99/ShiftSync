package com.shiftsync.shiftsync.availability.controller;

import com.shiftsync.shiftsync.availability.dto.ManagerWeeklyAvailabilityResponse;
import com.shiftsync.shiftsync.availability.service.AvailabilityService;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerAvailabilityController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthenticationHelper.class})
class ManagerAvailabilityControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AvailabilityService availabilityService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void getWeeklyAvailability_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/locations/1/availability")
                        .param("week", "2026-04-08"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "5", roles = "MANAGER")
    void getWeeklyAvailability_Success_ReturnsOk() throws Exception {
        ManagerWeeklyAvailabilityResponse.TimeWindow window =
                new ManagerWeeklyAvailabilityResponse.TimeWindow(LocalTime.of(9, 0), LocalTime.of(13, 0));

        ManagerWeeklyAvailabilityResponse.DailyAvailability day =
                new ManagerWeeklyAvailabilityResponse.DailyAvailability(
                        LocalDate.of(2026, 4, 7),
                        DayOfWeek.TUESDAY,
                        false,
                        List.of(window)
                );

        ManagerWeeklyAvailabilityResponse.EmployeeWeeklyAvailability employee =
                new ManagerWeeklyAvailabilityResponse.EmployeeWeeklyAvailability(20L, "Employee Two", List.of(day));

        ManagerWeeklyAvailabilityResponse response = new ManagerWeeklyAvailabilityResponse(
                LocalDate.of(2026, 4, 6),
                LocalDate.of(2026, 4, 12),
                List.of(employee)
        );

        when(availabilityService.getLocationWeeklyAvailability(anyLong(), eq(1L), eq(LocalDate.of(2026, 4, 8))))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/locations/1/availability")
                        .param("week", "2026-04-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value("2026-04-06"))
                .andExpect(jsonPath("$.employees[0].employeeId").value(20))
                .andExpect(jsonPath("$.employees[0].days[0].windows[0].startTime").value("09:00:00"));
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void getWeeklyAvailability_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/locations/1/availability")
                        .param("week", "2026-04-08"))
                .andExpect(status().isForbidden());
    }
}

