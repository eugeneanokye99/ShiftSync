package com.shiftsync.shiftsync.availability.controller;

import com.shiftsync.shiftsync.availability.dto.ManagerWeeklyAvailabilityResponse;
import com.shiftsync.shiftsync.availability.service.AvailabilityService;
import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Manager Availability", description = "Manager team availability endpoints")
public class ManagerAvailabilityController {

    private final AvailabilityService availabilityService;
    private final AuthenticationHelper authenticationHelper;

    @GetMapping("/{locationId}/availability")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(
            summary = "Get weekly team availability",
            description = "Returns per-employee availability for the requested week at a manager-assigned location."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Weekly availability retrieved", content = @Content(schema = @Schema(implementation = ManagerWeeklyAvailabilityResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid week parameter", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Manager not assigned to location", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ManagerWeeklyAvailabilityResponse> getWeeklyAvailability(
            Authentication authentication,
            @PathVariable Long locationId,
            @RequestParam("week") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate week
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        ManagerWeeklyAvailabilityResponse response =
                availabilityService.getLocationWeeklyAvailability(actorUserId, locationId, week);
        return ResponseEntity.ok(response);
    }
}

