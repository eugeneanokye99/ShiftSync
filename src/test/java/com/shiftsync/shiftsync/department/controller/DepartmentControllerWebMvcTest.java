package com.shiftsync.shiftsync.department.controller;

import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import com.shiftsync.shiftsync.department.dto.DepartmentResponse;
import com.shiftsync.shiftsync.department.service.DepartmentService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepartmentController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DepartmentControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void createDepartment_Success_ReturnsCreated() throws Exception {
        when(departmentService.createDepartment(anyLong(), any()))
                .thenReturn(new DepartmentResponse(10L, 1L, "Kitchen"));

        mockMvc.perform(post("/api/v1/locations/1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kitchen"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Kitchen"));
    }

    @Test
    void createDepartment_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/locations/1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kitchen"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createDepartment_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/locations/1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kitchen"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void createDepartment_Duplicate_ReturnsConflict() throws Exception {
        when(departmentService.createDepartment(anyLong(), any()))
                .thenThrow(new DuplicateResourceException("Department name already exists for this location"));

        mockMvc.perform(post("/api/v1/locations/1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kitchen"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Department name already exists for this location"));
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void createDepartment_BlankName_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/locations/1/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").value("Department name is required"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getDepartmentsByLocation_Manager_ReturnsOk() throws Exception {
        when(departmentService.getDepartmentsByLocation(1L))
                .thenReturn(List.of(new DepartmentResponse(10L, 1L, "Kitchen")));

        mockMvc.perform(get("/api/v1/locations/1/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Kitchen"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getDepartmentsByLocation_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/locations/1/departments"))
                .andExpect(status().isForbidden());
    }
}

