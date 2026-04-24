package com.shiftsync.shiftsync.availability.controller;

import com.shiftsync.shiftsync.availability.dto.AvailabilityOverrideResponse;
import com.shiftsync.shiftsync.availability.dto.CreateAvailabilityOverrideRequest;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityItemRequest;
import com.shiftsync.shiftsync.availability.dto.RecurringAvailabilityResponse;
import com.shiftsync.shiftsync.availability.service.AvailabilityService;
import com.shiftsync.shiftsync.common.response.ErrorResponse;
import com.shiftsync.shiftsync.common.util.AuthenticationHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/employees/me/availability")
@RequiredArgsConstructor
@Tag(name = "Availability", description = "Employee availability management endpoints")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final AuthenticationHelper authenticationHelper;

    @PutMapping("/recurring")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
            summary = "Replace recurring weekly availability",
            description = "Replaces recurring weekly availability windows. Empty list clears all windows."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recurring availability saved", content = @Content(array = @ArraySchema(schema = @Schema(implementation = RecurringAvailabilityResponse.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid request or overlapping windows", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<RecurringAvailabilityResponse>> replaceRecurringAvailability(
            Authentication authentication,
            @Valid @RequestBody List<@Valid RecurringAvailabilityItemRequest> windows
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        List<RecurringAvailabilityResponse> response = availabilityService.replaceRecurringAvailability(actorUserId, windows);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/overrides")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
            summary = "Create one-off availability override",
            description = "Creates a one-off unavailability date block for the authenticated employee."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Override created", content = @Content(schema = @Schema(implementation = AvailabilityOverrideResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Overlapping override dates", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AvailabilityOverrideResponse> createOverride(
            Authentication authentication,
            @Valid @RequestBody CreateAvailabilityOverrideRequest request
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        AvailabilityOverrideResponse response = availabilityService.createOverride(actorUserId, request);

        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{id}")
                        .buildAndExpand(response.id())
                        .toUri())
                .body(response);
    }

    @GetMapping("/overrides")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
            summary = "List active availability overrides",
            description = "Lists one-off unavailability overrides that are currently active or scheduled for future dates."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active overrides retrieved", content = @Content(array = @ArraySchema(schema = @Schema(implementation = AvailabilityOverrideResponse.class)))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<AvailabilityOverrideResponse>> listActiveOverrides(Authentication authentication) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        return ResponseEntity.ok(availabilityService.listActiveOverrides(actorUserId));
    }

    @DeleteMapping("/overrides/{overrideId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(
            summary = "Delete availability override",
            description = "Deletes a one-off unavailability override that belongs to the authenticated employee."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Override deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Override not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deleteOverride(
            Authentication authentication,
            @PathVariable Long overrideId
    ) {
        Long actorUserId = authenticationHelper.getCurrentUserId(authentication);
        availabilityService.deleteOverride(actorUserId, overrideId);
        return ResponseEntity.noContent().build();
    }
}

