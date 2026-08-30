package com.shiftsync.shiftsync.common.util;

import com.shiftsync.shiftsync.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationHelper {

    public Long getCurrentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new UnauthorizedException("Authentication required");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long value) {
            return value;
        }
        if (principal instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ex) {
                throw new UnauthorizedException("Invalid authentication principal");
            }
        }
        if (principal instanceof UserDetails userDetails) {
            try {
                return Long.parseLong(userDetails.getUsername());
            } catch (NumberFormatException ex) {
                throw new UnauthorizedException("Invalid authentication principal");
            }
        }

        throw new UnauthorizedException("Invalid authentication principal");
    }
}

