package com.shiftsync.shiftsync.location.controller;

import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import com.shiftsync.shiftsync.location.dto.LocationResponse;
import com.shiftsync.shiftsync.location.service.LocationService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class LocationControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationService locationService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void createLocation_WithoutToken_ReturnsUnauthorized() throws Exception {
        String body = """
                {
                  "name": "Airport Branch",
                  "address": "Airport Road",
                  "maxHeadcountPerShift": 40
                }
                """;

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void createLocation_WrongRole_ReturnsForbidden() throws Exception {
        String body = """
                {
                  "name": "Airport Branch",
                  "address": "Airport Road",
                  "maxHeadcountPerShift": 40
                }
                """;

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void createLocation_Success_ReturnsCreated() throws Exception {
        LocationResponse response = new LocationResponse(1L, "Airport Branch", "Airport Road", 40, true);
        when(locationService.createLocation(any())).thenReturn(response);

        String body = """
                {
                  "name": "Airport Branch",
                  "address": "Airport Road",
                  "maxHeadcountPerShift": 40
                }
                """;

        mockMvc.perform(post("/api/v1/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Airport Branch"));
    }

    @Test
    @WithMockUser
    void getActiveLocations_ReturnsOk() throws Exception {
        when(locationService.getActiveLocations())
                .thenReturn(List.of(new LocationResponse(1L, "Airport Branch", "Airport Road", 40, true)));

        mockMvc.perform(get("/api/v1/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Airport Branch"));
    }

    @Test
    void getActiveLocations_WithoutToken_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/locations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void updateLocation_Success_ReturnsOk() throws Exception {
        LocationResponse response = new LocationResponse(1L, "Airport Branch", "Airport Road", 50, true);
        when(locationService.updateLocation(any(), any())).thenReturn(response);

        String body = """
                {
                  "maxHeadcountPerShift": 50
                }
                """;

        mockMvc.perform(patch("/api/v1/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxHeadcountPerShift").value(50));
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void updateLocation_DuplicateName_ReturnsConflict() throws Exception {
        when(locationService.updateLocation(any(), any()))
                .thenThrow(new DuplicateResourceException("Location name already exists"));

        String body = """
                {
                  "name": "Airport Branch"
                }
                """;

        mockMvc.perform(patch("/api/v1/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Location name already exists"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateLocation_WrongRole_ReturnsForbidden() throws Exception {
        String body = """
                {
                  "name": "Airport Branch"
                }
                """;

        mockMvc.perform(patch("/api/v1/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}

