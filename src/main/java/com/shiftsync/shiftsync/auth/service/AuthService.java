package com.shiftsync.shiftsync.auth.service;

import com.shiftsync.shiftsync.auth.dto.AuthResponse;
import com.shiftsync.shiftsync.auth.dto.LoginRequest;
import com.shiftsync.shiftsync.auth.dto.RegisterRequest;
import com.shiftsync.shiftsync.auth.dto.RegisterResponse;

/**
 * The interface Auth service.
 */
public interface AuthService {
    /**
     * Register register response.
     *
     * @param request the request
     * @return the register response
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Login auth response.
     *
     * @param request the request
     * @return the auth response
     */
    AuthResponse login(LoginRequest request);

    /**
     * Refresh auth response.
     *
     * @param token the token
     * @return the auth response
     */
    AuthResponse refresh(String token);
}
