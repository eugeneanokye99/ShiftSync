package com.shiftsync.shiftsync.audit.controller;

import com.shiftsync.shiftsync.audit.dto.AuditLogPageResponse;
import com.shiftsync.shiftsync.audit.dto.AuditLogResponse;
import com.shiftsync.shiftsync.audit.service.AuditLogService;
import com.shiftsync.shiftsync.common.enums.AuditAction;
import com.shiftsync.shiftsync.common.enums.UserRole;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuditLogControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditLogService auditLogService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    private AuditLogPageResponse singleEntryPage() {
        AuditLogResponse entry = new AuditLogResponse(
                1L, "SHIFT", 10L, AuditAction.CREATE,
                2L, "Morgan Manager", UserRole.MANAGER,
                null, "{\"id\":10}", LocalDateTime.now()
        );
        return new AuditLogPageResponse(List.of(entry), 1L, 1, 0);
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void queryAuditLogs_HrAdmin_ReturnsOk() throws Exception {
        when(auditLogService.query(eq("SHIFT"), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(singleEntryPage());

        mockMvc.perform(get("/api/v1/audit-logs").param("entityType", "SHIFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].entityType").value("SHIFT"))
                .andExpect(jsonPath("$.content[0].action").value("CREATE"))
                .andExpect(jsonPath("$.content[0].actorName").value("Morgan Manager"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void queryAuditLogs_WithOptionalFilters_PassesThemThrough() throws Exception {
        when(auditLogService.query(eq("EMPLOYEE"), eq(5L), eq(1L), any(), any(), eq(0), eq(20)))
                .thenReturn(new AuditLogPageResponse(List.of(), 0L, 0, 0));

        mockMvc.perform(get("/api/v1/audit-logs")
                        .param("entityType", "EMPLOYEE")
                        .param("entityId", "5")
                        .param("actorId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void queryAuditLogs_MissingEntityType_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryAuditLogs_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs").param("entityType", "SHIFT"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void queryAuditLogs_ManagerRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs").param("entityType", "SHIFT"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void queryAuditLogs_EmployeeRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs").param("entityType", "SHIFT"))
                .andExpect(status().isForbidden());
    }
}
