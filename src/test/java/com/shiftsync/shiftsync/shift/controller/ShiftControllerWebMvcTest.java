package com.shiftsync.shiftsync.shift.controller;

import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.exception.InvalidStateException;
import com.shiftsync.shiftsync.common.exception.ResourceNotFoundException;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import com.shiftsync.shiftsync.shift.dto.LocationShiftPageResponse;
import com.shiftsync.shiftsync.shift.dto.ShiftResponse;
import com.shiftsync.shiftsync.shift.service.ShiftService;
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
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ShiftController.class, LocationShiftController.class})
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthenticationHelper.class})
class ShiftControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShiftService shiftService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    private static final String CREATE_BODY = """
            {
              "locationId": 10,
              "departmentId": 20,
              "date": "2026-06-01",
              "startTime": "09:00:00",
              "endTime": "17:00:00",
              "minimumHeadcount": 2
            }
            """;

    @Test
    void createShift_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void createShift_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "MANAGER")
    void createShift_ValidRequest_ReturnsCreated() throws Exception {
        ShiftResponse response = new ShiftResponse(
                50L, 10L, "HQ", 20L, "Engineering",
                LocalDate.of(2026, 6, 1),
                LocalTime.of(9, 0), LocalTime.of(17, 0),
                null, 2, "OPEN", 0
        );
        when(shiftService.createShift(anyLong(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.assignedCount").value(0));
    }

    @Test
    @WithMockUser(username = "1", roles = "MANAGER")
    void createShift_InvalidTimeOrder_ReturnsBadRequest() throws Exception {
        when(shiftService.createShift(anyLong(), any()))
                .thenThrow(new BadRequestException("End time must be after start time"));

        mockMvc.perform(post("/api/v1/shifts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End time must be after start time"));
    }

    @Test
    @WithMockUser(username = "1", roles = "MANAGER")
    void cancelShift_ValidRequest_ReturnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/v1/shifts/50/cancel"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "1", roles = "MANAGER")
    void cancelShift_AlreadyCancelled_ReturnsConflict() throws Exception {
        doThrow(new InvalidStateException("Shift is already cancelled"))
                .when(shiftService).cancelShift(anyLong(), eq(50L));

        mockMvc.perform(patch("/api/v1/shifts/50/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Shift is already cancelled"));
    }

    @Test
    @WithMockUser(username = "1", roles = "MANAGER")
    void cancelShift_NotFound_ReturnsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Shift not found"))
                .when(shiftService).cancelShift(anyLong(), eq(99L));

        mockMvc.perform(patch("/api/v1/shifts/99/cancel"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLocationShifts_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/locations/10/shifts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void getLocationShifts_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/locations/10/shifts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "MANAGER")
    void getLocationShifts_ValidRequest_ReturnsOk() throws Exception {
        LocationShiftPageResponse response = new LocationShiftPageResponse(List.of(), 0, 0, 0);
        when(shiftService.getLocationShifts(eq(10L), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/locations/10/shifts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
