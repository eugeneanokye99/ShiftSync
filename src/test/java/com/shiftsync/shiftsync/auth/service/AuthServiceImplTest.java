package com.shiftsync.shiftsync.auth.service;

import com.shiftsync.shiftsync.auth.dto.AuthResponse;
import com.shiftsync.shiftsync.auth.dto.LoginRequest;
import com.shiftsync.shiftsync.auth.dto.RegisterRequest;
import com.shiftsync.shiftsync.auth.dto.RegisterResponse;
import com.shiftsync.shiftsync.auth.entity.User;
import com.shiftsync.shiftsync.auth.repository.UserRepository;
import com.shiftsync.shiftsync.auth.service.impl.AuthServiceImpl;
import com.shiftsync.shiftsync.common.enums.UserRole;
import com.shiftsync.shiftsync.common.exception.DuplicateResourceException;
import com.shiftsync.shiftsync.common.exception.UnauthorizedException;
import com.shiftsync.shiftsync.config.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@shiftsync.com")
                .passwordHash("hashedPassword")
                .fullName("Test User")
                .role(UserRole.EMPLOYEE)
                .build();

        registerRequest = new RegisterRequest(
                "Test User",
                "test@shiftsync.com",
                "Password@123",
                UserRole.EMPLOYEE
        );

        loginRequest = new LoginRequest("test@shiftsync.com", "Password@123");
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.password())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        RegisterResponse response = authService.register(registerRequest);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@shiftsync.com");
        assertThat(response.fullName()).isEqualTo("Test User");
        assertThat(response.role()).isEqualTo(UserRole.EMPLOYEE);
        assertThat(response.message()).isEqualTo("User registered successfully");

        verify(userRepository).existsByEmail(registerRequest.email());
        verify(passwordEncoder).encode(registerRequest.password());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_EmailAlreadyExists_ThrowsDuplicateResourceException() {
        when(userRepository.existsByEmail(registerRequest.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already exists");

        verify(userRepository).existsByEmail(registerRequest.email());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(anyLong(), anyString(), anyString())).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@shiftsync.com");
        assertThat(response.fullName()).isEqualTo("Test User");
        assertThat(response.role()).isEqualTo(UserRole.EMPLOYEE);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).findByEmail("test@shiftsync.com");
        verify(jwtService).generateToken(1L, "test@shiftsync.com", "EMPLOYEE");
    }

    @Test
    void login_InvalidCredentials_ThrowsUnauthorizedException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid credentials");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtService, never()).generateToken(anyLong(), anyString(), anyString());
    }

    @Test
    void refresh_ValidToken_Success() {
        String oldToken = "old-jwt-token";
        String newToken = "new-jwt-token";

        when(jwtService.isTokenValid(oldToken)).thenReturn(true);
        when(jwtService.extractEmail(oldToken)).thenReturn("test@shiftsync.com");
        when(userRepository.findByEmail("test@shiftsync.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(anyLong(), anyString(), anyString())).thenReturn(newToken);

        AuthResponse response = authService.refresh(oldToken);

        assertThat(response).isNotNull();
        assertThat(response.token()).isEqualTo(newToken);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("test@shiftsync.com");

        verify(jwtService).isTokenValid(oldToken);
        verify(jwtService).extractEmail(oldToken);
        verify(userRepository).findByEmail("test@shiftsync.com");
        verify(jwtService).generateToken(1L, "test@shiftsync.com", "EMPLOYEE");
    }

    @Test
    void refresh_InvalidToken_ThrowsUnauthorizedException() {
        String invalidToken = "invalid-token";
        when(jwtService.isTokenValid(invalidToken)).thenReturn(false);

        assertThatThrownBy(() -> authService.refresh(invalidToken))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid or expired token");

        verify(jwtService).isTokenValid(invalidToken);
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void refresh_UserNotFound_ThrowsUnauthorizedException() {
        String token = "valid-token";
        when(jwtService.isTokenValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn("nonexistent@shiftsync.com");
        when(userRepository.findByEmail("nonexistent@shiftsync.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(token))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User not found");

        verify(jwtService).isTokenValid(token);
        verify(jwtService).extractEmail(token);
        verify(userRepository).findByEmail("nonexistent@shiftsync.com");
    }
}
