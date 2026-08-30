package com.shiftsync.shiftsync.auth.dto;

import com.shiftsync.shiftsync.common.enums.UserRole;

public record RegisterResponse(
        Long userId,
        String email,
        String fullName,
        UserRole role,
        String message
) {}
