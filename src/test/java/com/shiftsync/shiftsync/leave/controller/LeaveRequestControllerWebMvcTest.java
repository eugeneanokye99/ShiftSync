package com.shiftsync.shiftsync.leave.controller;

import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.LeaveType;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import com.shiftsync.shiftsync.leave.dto.LeaveRequestResponse;
import com.shiftsync.shiftsync.leave.service.LeaveRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveRequestController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthenticationHelper.class})
class LeaveRequestControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaveRequestService leaveRequestService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void createLeaveRequest_WithoutToken_ReturnsUnauthorized() throws Exception {
        String body = """
                {
                  "startDate": "2099-01-01",
                  "endDate": "2099-01-03",
                  "leaveType": "ANNUAL",
                  "reason": "Family event"
                }
                """;

        mockMvc.perform(post("/api/v1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void createLeaveRequest_WrongRole_ReturnsForbidden() throws Exception {
        String body = """
                {
                  "startDate": "2099-01-01",
                  "endDate": "2099-01-03",
                  "leaveType": "ANNUAL",
                  "reason": "Family event"
                }
                """;

        mockMvc.perform(post("/api/v1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void createLeaveRequest_Success_ReturnsCreated() throws Exception {
        LeaveRequestResponse response = new LeaveRequestResponse(
                100L,
                20L,
                LocalDate.of(2099, 1, 1),
                LocalDate.of(2099, 1, 3),
                LeaveType.ANNUAL,
                "Family event",
                LeaveStatus.PENDING,
                LocalDateTime.of(2098, 12, 1, 10, 0)
        );
        when(leaveRequestService.createLeaveRequest(eq(5L), any())).thenReturn(response);

        String body = """
                {
                  "startDate": "2099-01-01",
                  "endDate": "2099-01-03",
                  "leaveType": "ANNUAL",
                  "reason": "Family event"
                }
                """;

        mockMvc.perform(post("/api/v1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void createLeaveRequest_MissingRequiredFields_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void createLeaveRequest_StartDateInPast_ReturnsBadRequest() throws Exception {
        String body = """
                {
                  "startDate": "2020-01-01",
                  "endDate": "2099-01-03",
                  "leaveType": "ANNUAL"
                }
                """;

        mockMvc.perform(post("/api/v1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void createLeaveRequest_EndDateBeforeStartDate_ReturnsBadRequest() throws Exception {
        String body = """
                {
                  "startDate": "2099-01-10",
                  "endDate": "2099-01-03",
                  "leaveType": "ANNUAL"
                }
                """;

        mockMvc.perform(post("/api/v1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void createLeaveRequest_OverlappingDates_ReturnsConflict() throws Exception {
        when(leaveRequestService.createLeaveRequest(eq(5L), any()))
                .thenThrow(new InvalidStateException("Leave request overlaps with an existing pending or approved leave request"));

        String body = """
                {
                  "startDate": "2099-01-01",
                  "endDate": "2099-01-03",
                  "leaveType": "ANNUAL"
                }
                """;

        mockMvc.perform(post("/api/v1/leave-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Leave request overlaps with an existing pending or approved leave request"));
    }
}

