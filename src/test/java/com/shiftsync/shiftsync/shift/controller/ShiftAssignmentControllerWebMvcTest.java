package com.shiftsync.shiftsync.shift.controller;

import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import com.shiftsync.shiftsync.shift.dto.AssignEmployeeResponse;
import com.shiftsync.shiftsync.shift.service.ShiftAssignmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShiftAssignmentController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthenticationHelper.class})
class ShiftAssignmentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShiftAssignmentService shiftAssignmentService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void assignEmployee_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/shifts/100/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":20}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "5", roles = "MANAGER")
    void assignEmployee_AvailabilityWarning_ReturnsOk() throws Exception {
        when(shiftAssignmentService.assignEmployee(anyLong(), eq(100L), any(), eq(false)))
                .thenReturn(new AssignEmployeeResponse(
                        "WARNING",
                        List.of("AVAILABILITY_MISMATCH"),
                        "Employee availability does not match this shift. Re-submit with override=true to proceed.",
                        null
                ));

        mockMvc.perform(post("/api/v1/shifts/100/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WARNING"));
    }

    @Test
    @WithMockUser(username = "5", roles = "MANAGER")
    void assignEmployee_WithOverride_CreatesAssignment() throws Exception {
        when(shiftAssignmentService.assignEmployee(anyLong(), eq(100L), any(), eq(true)))
                .thenReturn(new AssignEmployeeResponse(
                        "ASSIGNED",
                        List.of(),
                        "Employee assigned successfully",
                        200L
                ));

        mockMvc.perform(post("/api/v1/shifts/100/assignments")
                        .queryParam("override", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignmentId").value(200));
    }

    @Test
    @WithMockUser(username = "5", roles = "EMPLOYEE")
    void assignEmployee_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/shifts/100/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"employeeId\":20}"))
                .andExpect(status().isForbidden());
    }
}


