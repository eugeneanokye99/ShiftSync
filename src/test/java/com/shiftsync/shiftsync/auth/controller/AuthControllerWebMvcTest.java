package com.shiftsync.shiftsync.auth.controller;

import com.shiftsync.shiftsync.auth.dto.LoginRequest;
import com.shiftsync.shiftsync.auth.dto.RegisterRequest;
import com.shiftsync.shiftsync.auth.service.AuthService;
import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.common.exception.GlobalExceptionHandler;
import com.shiftsync.shiftsync.config.security.CustomUserDetailsService;
import com.shiftsync.shiftsync.config.security.JwtAuthenticationFilter;
import com.shiftsync.shiftsync.config.security.JwtService;
import com.shiftsync.shiftsync.config.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void register_WithoutToken_ReturnsUnauthorized() throws Exception {
        String body = """
                {
                  "fullName": "Test User",
                  "email": "test@shiftsync.com",
                  "password": "Password@123",
                  "role": "EMPLOYEE"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void register_InsufficientRole_ReturnsForbidden() throws Exception {
        String body = """
                {
                  "fullName": "Test User",
                  "email": "test@shiftsync.com",
                  "password": "Password@123",
                  "role": "EMPLOYEE"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void register_DuplicateEmail_ReturnsBadRequestWithFieldError() throws Exception {
        String body = """
                {
                  "fullName": "Test User",
                  "email": "test@shiftsync.com",
                  "password": "Password@123",
                  "role": "EMPLOYEE"
                }
                """;
        when(authService.register(any(RegisterRequest.class))).thenThrow(new DuplicateResourceException("Email already exists"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Email already exists"));
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void register_MissingFields_ReturnsBadRequestWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.role").exists());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @WithMockUser(roles = "HR_ADMIN")
    void register_InvalidRoleValue_ReturnsBadRequestUnreadableBody() throws Exception {
        String body = """
                {
                  "fullName": "Test User",
                  "email": "test@shiftsync.com",
                  "password": "Password@123",
                  "role": "SUPER_USER"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    void login_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        String body = """
                {
                  "email": "test@shiftsync.com",
                  "password": "wrong-password"
                }
                """;
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void refresh_MissingAuthorizationHeader_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authorization header must contain a valid Bearer token"));

        verify(authService, never()).refresh(any(String.class));
    }

    @Test
    void refresh_MalformedAuthorizationHeader_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", "Token abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authorization header must contain a valid Bearer token"));

        verify(authService, never()).refresh(any(String.class));
    }
}

