package com.shiftsync.shiftsync.leave.controller;

import com.shiftsync.shiftsync.common.enums.LeaveStatus;
import com.shiftsync.shiftsync.common.enums.LeaveType;
import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import com.shiftsync.shiftsync.leave.dto.GetLeaveRequestsRequest;
import com.shiftsync.shiftsync.leave.dto.LeaveRequestResponse;
import com.shiftsync.shiftsync.leave.dto.PendingLeaveRequestPageResponse;
import com.shiftsync.shiftsync.leave.dto.PendingLeaveRequestResponse;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        when(leaveRequestService.createLeaveRequest(eq(5L), any()))
                .thenThrow(new BadRequestException("End date must be on or after start date"));

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
                .thenThrow(new DuplicateResourceException("Leave request overlaps with an existing pending or approved leave request"));

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

    @Test
    void getLeaveRequests_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/leave-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void getLeaveRequests_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/leave-requests"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void getLeaveRequests_Success_ReturnsOk() throws Exception {
        PendingLeaveRequestResponse item = new PendingLeaveRequestResponse(
                100L,
                20L,
                "Employee One",
                "Kitchen",
                LocalDate.of(2099, 1, 1),
                LocalDate.of(2099, 1, 3),
                LeaveType.ANNUAL,
                3,
                "Family event",
                LeaveStatus.PENDING,
                LocalDateTime.of(2098, 12, 1, 10, 0)
        );
        PendingLeaveRequestPageResponse pageResponse = new PendingLeaveRequestPageResponse(
                List.of(item),
                1,
                1,
                0
        );

        when(leaveRequestService.getLeaveRequests(any(GetLeaveRequestsRequest.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/leave-requests")
                        .param("employeeId", "20")
                        .param("locationId", "3")
                        .param("startDate", "2099-01-01")
                        .param("endDate", "2099-01-31")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeName").value("Employee One"))
                .andExpect(jsonPath("$.content[0].daysRequested").value(3));

        verify(leaveRequestService).getLeaveRequests(argThat(req ->
                req.employeeId().equals(20L)
                        && req.locationId().equals(3L)
                        && req.status() == null
                        && req.startDate().equals(LocalDate.of(2099, 1, 1))
                        && req.endDate().equals(LocalDate.of(2099, 1, 31))
                        && req.page() == 0
                        && req.size() == 10
        ));
    }

    @Test
    void approveLeaveRequest_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/leave-requests/100/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hrNote\":\"Approved\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void approveLeaveRequest_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/leave-requests/100/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hrNote\":\"Approved\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void approveLeaveRequest_MissingHrNote_ReturnsBadRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/leave-requests/100/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hrNote\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void approveLeaveRequest_NotPending_ReturnsConflict() throws Exception {
        when(leaveRequestService.approveLeaveRequest(eq(1L), eq(100L), any()))
                .thenThrow(new InvalidStateException("Only pending leave requests can be approved"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/leave-requests/100/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hrNote\":\"Approved\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only pending leave requests can be approved"));
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void approveLeaveRequest_Success_ReturnsOk() throws Exception {
        LeaveRequestResponse response = new LeaveRequestResponse(
                100L,
                20L,
                LocalDate.of(2099, 1, 1),
                LocalDate.of(2099, 1, 3),
                LeaveType.ANNUAL,
                "Family event",
                LeaveStatus.APPROVED,
                LocalDateTime.of(2098, 12, 1, 10, 0)
        );
        when(leaveRequestService.approveLeaveRequest(eq(1L), eq(100L), any())).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/leave-requests/100/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hrNote\":\"Approved\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectLeaveRequest_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/leave-requests/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hrNote\":\"Rejected\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void rejectLeaveRequest_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/leave-requests/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hrNote\":\"Rejected\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void rejectLeaveRequest_MissingHrNote_ReturnsBadRequest() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/leave-requests/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hrNote\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void rejectLeaveRequest_NotPending_ReturnsConflict() throws Exception {
        when(leaveRequestService.rejectLeaveRequest(eq(1L), eq(100L), any()))
                .thenThrow(new InvalidStateException("Only pending leave requests can be rejected"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/leave-requests/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hrNote\":\"Rejected\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only pending leave requests can be rejected"));
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void rejectLeaveRequest_Success_ReturnsOk() throws Exception {
        LeaveRequestResponse response = new LeaveRequestResponse(
                100L,
                20L,
                LocalDate.of(2099, 1, 1),
                LocalDate.of(2099, 1, 3),
                LeaveType.ANNUAL,
                "Family event",
                LeaveStatus.REJECTED,
                LocalDateTime.of(2098, 12, 1, 10, 0)
        );
        when(leaveRequestService.rejectLeaveRequest(eq(1L), eq(100L), any())).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/leave-requests/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hrNote\":\"Rejected\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void cancelLeaveRequest_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/leave-requests/100"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void cancelLeaveRequest_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/leave-requests/100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void cancelLeaveRequest_NotPending_ReturnsConflict() throws Exception {
        when(leaveRequestService.cancelLeaveRequest(eq(5L), eq(100L)))
                .thenThrow(new InvalidStateException("Only pending leave requests can be cancelled"));

        mockMvc.perform(delete("/api/v1/leave-requests/100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only pending leave requests can be cancelled"));
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void cancelLeaveRequest_OtherEmployee_ReturnsForbidden() throws Exception {
        when(leaveRequestService.cancelLeaveRequest(eq(5L), eq(100L)))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("You can only cancel your own leave request"));

        mockMvc.perform(delete("/api/v1/leave-requests/100"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You can only cancel your own leave request"));
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void cancelLeaveRequest_Success_ReturnsOk() throws Exception {
        LeaveRequestResponse response = new LeaveRequestResponse(
                100L,
                20L,
                LocalDate.of(2099, 1, 1),
                LocalDate.of(2099, 1, 3),
                LeaveType.ANNUAL,
                "Family event",
                LeaveStatus.CANCELLED,
                LocalDateTime.of(2098, 12, 1, 10, 0)
        );
        when(leaveRequestService.cancelLeaveRequest(eq(5L), eq(100L))).thenReturn(response);

        mockMvc.perform(delete("/api/v1/leave-requests/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}

