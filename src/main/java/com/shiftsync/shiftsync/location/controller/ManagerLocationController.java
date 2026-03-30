package com.shiftsync.shiftsync.location.controller;

import com.shiftsync.shiftsync.common.exception.UnauthorizedException;
import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.location.dto.LocationResponse;
import com.shiftsync.shiftsync.location.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/managers/me")
@RequiredArgsConstructor
@Tag(name = "Manager Locations", description = "Manager location assignment endpoints")
public class ManagerLocationController {

    private final LocationService locationService;

    @GetMapping("/locations")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Get assigned locations",
            description = "Returns locations assigned to the authenticated manager."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Locations fetched", content = @Content(array = @ArraySchema(schema = @Schema(implementation = LocationResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<LocationResponse>> getMyAssignedLocations(Authentication authentication) {
        Long actorUserId = getCurrentUserId(authentication);
        return ResponseEntity.ok(locationService.getAssignedLocationsForManager(actorUserId));
    }

    private Long getCurrentUserId(Authentication authentication) {
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

