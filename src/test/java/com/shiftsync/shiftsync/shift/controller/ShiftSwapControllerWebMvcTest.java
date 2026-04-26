package com.shiftsync.shiftsync.shift.controller;

import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import com.shiftsync.shiftsync.shift.dto.ShiftSwapResponse;
import com.shiftsync.shiftsync.shift.service.ShiftSwapService;
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
import java.time.LocalTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShiftSwapController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthenticationHelper.class})
class ShiftSwapControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShiftSwapService shiftSwapService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    private static final String SWAP_REQUEST_BODY = """
            {
              "myShiftAssignmentId": 1,
              "targetEmployeeId": 200,
              "targetShiftAssignmentId": 2,
              "reason": "Personal conflict"
            }
            """;

    @Test
    void requestSwap_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/shift-swaps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SWAP_REQUEST_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "MANAGER")
    void requestSwap_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/shift-swaps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SWAP_REQUEST_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void requestSwap_ValidRequest_ReturnsCreated() throws Exception {
        ShiftSwapResponse response = new ShiftSwapResponse(
                10L, 100L, "Alice",
                LocalDate.of(2026, 6, 1), LocalTime.of(9, 0), LocalTime.of(17, 0),
                200L, "Bob",
                LocalDate.of(2026, 6, 2), LocalTime.of(9, 0), LocalTime.of(17, 0),
                "PENDING_MANAGER_APPROVAL", "Personal conflict", null,
                LocalDateTime.now()
        );
        when(shiftSwapService.requestSwap(anyLong(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/shift-swaps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SWAP_REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("PENDING_MANAGER_APPROVAL"))
                .andExpect(jsonPath("$.requesterName").value("Alice"));
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void requestSwap_NotAssignedToShift_ReturnsBadRequest() throws Exception {
        when(shiftSwapService.requestSwap(anyLong(), any()))
                .thenThrow(new BadRequestException("You are not assigned to the referenced shift"));

        mockMvc.perform(post("/api/v1/shift-swaps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SWAP_REQUEST_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You are not assigned to the referenced shift"));
    }

    @Test
    void approveSwap_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/shift-swaps/10/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "3", roles = "EMPLOYEE")
    void approveSwap_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/shift-swaps/10/approve"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "3", roles = "MANAGER")
    void approveSwap_ValidRequest_ReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/shift-swaps/10/approve"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "3", roles = "MANAGER")
    void approveSwap_ConflictDetected_ReturnsConflict() throws Exception {
        doThrow(new InvalidStateException("Conflict detected: Bob has an overlapping shift"))
                .when(shiftSwapService).approveSwap(anyLong(), eq(10L));

        mockMvc.perform(patch("/api/v1/shift-swaps/10/approve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Conflict detected: Bob has an overlapping shift"));
    }

    @Test
    @WithMockUser(username = "3", roles = "MANAGER")
    void approveSwap_NotPending_ReturnsConflict() throws Exception {
        doThrow(new InvalidStateException("Swap request is not pending approval"))
                .when(shiftSwapService).approveSwap(anyLong(), eq(99L));

        mockMvc.perform(patch("/api/v1/shift-swaps/99/approve"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Swap request is not pending approval"));
    }

    @Test
    @WithMockUser(username = "3", roles = "MANAGER")
    void approveSwap_NotFound_ReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Swap request not found"))
                .when(shiftSwapService).approveSwap(anyLong(), eq(404L));

        mockMvc.perform(patch("/api/v1/shift-swaps/404/approve"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "3", roles = "MANAGER")
    void rejectSwap_WithNote_ReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/shift-swaps/10/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerNote\": \"Insufficient coverage\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "3", roles = "MANAGER")
    void rejectSwap_WithoutBody_ReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/shift-swaps/10/reject"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "3", roles = "MANAGER")
    void rejectSwap_NotPending_ReturnsConflict() throws Exception {
        doThrow(new InvalidStateException("Swap request is not pending approval"))
                .when(shiftSwapService).rejectSwap(anyLong(), eq(10L), any());

        mockMvc.perform(patch("/api/v1/shift-swaps/10/reject"))
                .andExpect(status().isConflict());
    }
}
