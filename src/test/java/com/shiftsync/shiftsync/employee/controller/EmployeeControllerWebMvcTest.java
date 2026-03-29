package com.shiftsync.shiftsync.employee.controller;

import com.shiftsync.shiftsync.common.enums.EmploymentType;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import com.shiftsync.shiftsync.employee.dto.EmployeePageResponse;
import com.shiftsync.shiftsync.employee.dto.EmployeeResponse;
import com.shiftsync.shiftsync.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class EmployeeControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void createEmployee_WithoutToken_ReturnsUnauthorized() throws Exception {
        String body = """
                {
                  "userId": 1,
                  "employmentType": "FULL_TIME",
                  "departmentId": 2,
                  "locationId": 3,
                  "contractedWeeklyHours": 40
                }
                """;

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createEmployee_WrongRole_ReturnsForbidden() throws Exception {
        String body = """
                {
                  "userId": 1,
                  "employmentType": "FULL_TIME",
                  "departmentId": 2,
                  "locationId": 3,
                  "contractedWeeklyHours": 40
                }
                """;

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void createEmployee_Success_ReturnsCreated() throws Exception {
        EmployeeResponse response = new EmployeeResponse(
                10L,
                1L,
                "Jane Doe",
                UserRole.EMPLOYEE,
                "+233000000000",
                EmploymentType.FULL_TIME,
                2L,
                "Kitchen",
                3L,
                "Airport Branch",
                List.of("barista"),
                new BigDecimal("40.00"),
                LocalDate.of(2026, 1, 1),
                true
        );

        when(employeeService.createEmployee(any())).thenReturn(response);

        String body = """
                {
                  "userId": 1,
                  "phone": "+233000000000",
                  "employmentType": "FULL_TIME",
                  "departmentId": 2,
                  "locationId": 3,
                  "skills": ["barista"],
                  "contractedWeeklyHours": 40,
                  "hireDate": "2026-01-01"
                }
                """;

        mockMvc.perform(post("/api/v1/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeId").value(10))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getEmployees_ManagerRole_ReturnsOk() throws Exception {
        EmployeeResponse employee = new EmployeeResponse(
                10L,
                1L,
                "Jane Doe",
                UserRole.EMPLOYEE,
                "+233000000000",
                EmploymentType.FULL_TIME,
                2L,
                "Kitchen",
                3L,
                "Airport Branch",
                List.of("barista"),
                new BigDecimal("40.00"),
                LocalDate.of(2026, 1, 1),
                true
        );

        EmployeePageResponse pageResponse = new EmployeePageResponse(List.of(employee), 1, 1, 0);
        when(employeeService.getEmployees(anyInt(), anyInt(), anyString(), anyString())).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/employees")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "name")
                        .param("direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.content[0].employeeId").value(10));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getEmployees_EmployeeRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/employees"))
                .andExpect(status().isForbidden());

        verify(employeeService, never()).getEmployees(anyInt(), anyInt(), anyString(), anyString());
    }
}

