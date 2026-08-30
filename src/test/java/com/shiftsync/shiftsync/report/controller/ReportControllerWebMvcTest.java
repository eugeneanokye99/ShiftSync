package com.shiftsync.shiftsync.report.controller;

import com.shiftsync.shiftsync.common.exception.BadRequestException;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import com.shiftsync.shiftsync.report.dto.CoverageReportPageResponse;
import com.shiftsync.shiftsync.report.dto.LeaveUtilizationReportResponse;
import com.shiftsync.shiftsync.report.dto.OvertimeReportPageResponse;
import com.shiftsync.shiftsync.report.exporter.CsvExporter;
import com.shiftsync.shiftsync.report.service.ReportService;
import com.shiftsync.shiftsync.shift.service.ShiftService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthenticationHelper.class})
class ReportControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @MockitoBean
    private ShiftService shiftService;

    @MockitoBean
    private CsvExporter csvExporter;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    // ── Coverage ───────────────────────────────────────────────────────────────

    @Test
    void getCoverageReport_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/coverage")
                        .param("locationId", "10")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "EMPLOYEE")
    void getCoverageReport_EmployeeRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/coverage")
                        .param("locationId", "10")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "MANAGER")
    void getCoverageReport_ManagerRole_ReturnsOk() throws Exception {
        when(reportService.getCoverageReport(anyLong(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new CoverageReportPageResponse(List.of(), 0, 0, 0));

        mockMvc.perform(get("/api/v1/reports/coverage")
                        .param("locationId", "10")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "1", roles = "MANAGER")
    void getCoverageReport_FromAfterTo_ReturnsBadRequest() throws Exception {
        when(reportService.getCoverageReport(anyLong(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new BadRequestException("'from' date must not be after 'to' date"));

        mockMvc.perform(get("/api/v1/reports/coverage")
                        .param("locationId", "10")
                        .param("from", "2026-06-30")
                        .param("to", "2026-06-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' date must not be after 'to' date"));
    }

    @Test
    void getCoverageReportCsv_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/coverage")
                        .accept("text/csv")
                        .param("locationId", "10")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isUnauthorized());
    }

    // ── Overtime ───────────────────────────────────────────────────────────────

    @Test
    void getOvertimeReport_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/overtime")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "MANAGER")
    void getOvertimeReport_ManagerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/overtime")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void getOvertimeReport_HrAdminRole_ReturnsOk() throws Exception {
        when(reportService.getOvertimeReport(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new OvertimeReportPageResponse(List.of(), 0, 0, 0));

        mockMvc.perform(get("/api/v1/reports/overtime")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void getOvertimeReport_FromAfterTo_ReturnsBadRequest() throws Exception {
        when(reportService.getOvertimeReport(any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new BadRequestException("'from' date must not be after 'to' date"));

        mockMvc.perform(get("/api/v1/reports/overtime")
                        .param("from", "2026-06-30")
                        .param("to", "2026-06-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' date must not be after 'to' date"));
    }

    // ── Leave ──────────────────────────────────────────────────────────────────

    @Test
    void getLeaveReport_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/leave")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "1", roles = "MANAGER")
    void getLeaveReport_ManagerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/leave")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void getLeaveReport_HrAdminRole_ReturnsOk() throws Exception {
        when(reportService.getLeaveReport(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new LeaveUtilizationReportResponse(List.of(), 0, 0, 0, List.of()));

        mockMvc.perform(get("/api/v1/reports/leave")
                        .param("from", "2026-06-01")
                        .param("to", "2026-06-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(username = "1", roles = "HR_ADMIN")
    void getLeaveReport_FromAfterTo_ReturnsBadRequest() throws Exception {
        when(reportService.getLeaveReport(any(), any(), any(), anyInt(), anyInt()))
                .thenThrow(new BadRequestException("'from' date must not be after 'to' date"));

        mockMvc.perform(get("/api/v1/reports/leave")
                        .param("from", "2026-06-30")
                        .param("to", "2026-06-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' date must not be after 'to' date"));
    }
}
