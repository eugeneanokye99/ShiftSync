package com.shiftsync.shiftsync.location.controller;

import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import com.shiftsync.shiftsync.location.dto.LocationResponse;
import com.shiftsync.shiftsync.location.service.LocationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerLocationController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, AuthenticationHelper.class})
class ManagerLocationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationService locationService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "11", roles = "MANAGER")
    void getMyAssignedLocations_ReturnsList() throws Exception {
        when(locationService.getAssignedLocationsForManager(11L)).thenReturn(List.of(
                new LocationResponse(1L, "Airport Branch", "Airport Road", 40, true)
        ));

        mockMvc.perform(get("/api/v1/managers/me/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Airport Branch"));
    }

    @Test
    @WithMockUser(username = "11", roles = "MANAGER")
    void getMyAssignedLocations_WhenNoAssignments_ReturnsEmptyList() throws Exception {
        when(locationService.getAssignedLocationsForManager(11L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/managers/me/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getMyAssignedLocations_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/managers/me/locations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "11", roles = "EMPLOYEE")
    void getMyAssignedLocations_WrongRole_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/managers/me/locations"))
                .andExpect(status().isForbidden());
    }
}

